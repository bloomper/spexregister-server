package nu.fgv.register.server.spex;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spex.category.SpexCategoryDto;
import nu.fgv.register.server.spex.category.SpexCategoryRepository;
import nu.fgv.register.server.util.FileUtil;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static nu.fgv.register.server.spex.SpexMapper.SPEX_MAPPER;
import static nu.fgv.register.server.spex.SpexSpecification.hasIds;
import static nu.fgv.register.server.spex.SpexSpecification.hasParent;
import static nu.fgv.register.server.spex.SpexSpecification.hasParentIds;
import static nu.fgv.register.server.spex.SpexSpecification.hasYear;
import static nu.fgv.register.server.spex.SpexSpecification.isNotRevival;
import static nu.fgv.register.server.spex.SpexSpecification.isRevival;
import static nu.fgv.register.server.spex.category.SpexCategoryMapper.SPEX_CATEGORY_MAPPER;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class SpexService {

    private final SpexRepository repository;
    private final SpexDetailsRepository detailsRepository;
    private final SpexCategoryRepository categoryRepository;

    public List<SpexDto> findAll(final Sort sort) {
        return repository
                .findAll(isNotRevival(), sort)
                .stream().map(SPEX_MAPPER::toDto)
                .toList();
    }

    public Page<SpexDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<Spex>builder().build(FilterParser.parse(filter), SpexSpecification::new), pageable)
                        .map(SPEX_MAPPER::toDto) :
                repository
                        .findAll(pageable)
                        .map(SPEX_MAPPER::toDto);
    }

    public Optional<SpexDto> findById(final Long id) {
        return repository
                .findById(id)
                .map(SPEX_MAPPER::toDto);
    }

    public List<SpexDto> findByIds(final List<Long> ids, final Sort sort) {
        return repository
                .findAll(hasIds(ids), sort)
                .stream()
                .map(SPEX_MAPPER::toDto)
                .toList();
    }

    public List<SpexDto> findRevivalsByParentIds(final List<Long> parentIds, final Sort sort) {
        return repository
                .findAll(hasParentIds(parentIds), sort)
                .stream()
                .map(SPEX_MAPPER::toDto)
                .toList();
    }

    public SpexDto create(final SpexCreateDto dto) {
        final Spex model = SPEX_MAPPER.toModel(dto);
        detailsRepository.save(model.getDetails());
        return SPEX_MAPPER.toDto(repository.save(model));
    }

    public Optional<SpexDto> update(final SpexUpdateDto dto) {
        return partialUpdate(dto);
    }

    public Optional<SpexDto> partialUpdate(final SpexUpdateDto dto) {
        return repository
                .findById(dto.getId())
                .map(spex -> {
                    SPEX_MAPPER.toPartialModel(dto, spex);
                    return spex;
                })
                .map(spex -> {
                    detailsRepository.save(spex.getDetails());
                    return repository.save(spex);
                })
                .map(SPEX_MAPPER::toDto);
    }

    public void deleteById(final Long id) {
        repository
                .findById(id)
                .ifPresent(spex -> {
                    repository.findAll(hasParent(spex)).forEach(revival -> repository.deleteById(revival.getId()));
                    repository.deleteById(spex.getId());
                    detailsRepository.deleteById(spex.getDetails().getId());
                });
    }

    public Optional<SpexDto> savePoster(final Long spexId, final byte[] poster, final String contentType) {
        return repository
                .findById(spexId)
                .map(spex -> {
                    spex.getDetails().setPoster(poster);
                    spex.getDetails().setPosterContentType(hasText(contentType) ? contentType : FileUtil.detectMimeType(poster));
                    detailsRepository.save(spex.getDetails());
                    return SPEX_MAPPER.toDto(spex);
                });
    }

    public Optional<SpexDto> deletePoster(final Long spexId) {
        return repository
                .findById(spexId)
                .map(spex -> {
                    spex.getDetails().setPoster(null);
                    spex.getDetails().setPosterContentType(null);
                    detailsRepository.save(spex.getDetails());
                    return SPEX_MAPPER.toDto(spex);
                });
    }

    public Optional<Pair<byte[], String>> getPoster(final Long spexId) {
        return repository
                .findById(spexId)
                .map(Spex::getDetails)
                .filter(details -> details.getPoster() != null && hasText(details.getPosterContentType()))
                .map(details -> Pair.of(details.getPoster(), details.getPosterContentType()));
    }

    public Optional<SpexDto> findRevivalById(final Long spexId, final Long id) {
        if (doesSpexExist(spexId)) {
            return repository
                    .findById(id)
                    .filter(revival -> revival.getParent() != null && revival.getParent().getId().equals(spexId))
                    .map(SPEX_MAPPER::toDto);
        } else {
            throw new ResourceNotFoundException(String.format("Spex %s does not exist", spexId));
        }
    }

    public Page<SpexDto> findRevivals(final Pageable pageable) {
        return repository
                .findAll(isRevival(), pageable)
                .map(SPEX_MAPPER::toDto);
    }

    public Page<SpexDto> findRevivalsByParent(final Long spexId, final Pageable pageable) {
        if (doesSpexExist(spexId)) {
            return repository
                    .findById(spexId)
                    .map(parent -> repository
                            .findAll(hasParent(parent), pageable)
                            .map(SPEX_MAPPER::toDto)
                    )
                    .orElseGet(Page::empty);
        } else {
            throw new ResourceNotFoundException(String.format("Spex %s does not exist", spexId));
        }
    }

    public Optional<SpexDto> addRevival(final Long spexId, final String year) {
        if (doesSpexExist(spexId)) {
            return repository
                    .findById(spexId)
                    .filter(parent -> !repository.exists(hasParent(parent).and(hasYear(year))))
                    .map(parent -> {
                        final Spex revival = new Spex();
                        revival.setDetails(parent.getDetails());
                        revival.setParent(parent);
                        revival.setYear(year);
                        return repository.save(revival);
                    })
                    .map(SPEX_MAPPER::toDto);
        } else {
            throw new ResourceNotFoundException(String.format("Spex %s does not exist", spexId));
        }
    }

    public boolean deleteRevival(final Long spexId, final String year) {
        if (doesSpexExist(spexId)) {
            return repository
                    .findById(spexId)
                    .filter(parent -> repository.exists(hasParent(parent).and(hasYear(year))))
                    .flatMap(parent -> repository.findOne(hasParent(parent).and(hasYear(year))))
                    .map(revival -> {
                        repository.deleteById(revival.getId());
                        return true;
                    })
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("Spex %s does not exist", spexId));
        }
    }

    public Optional<SpexCategoryDto> findCategoryBySpex(final Long spexId) {
        if (doesSpexExist(spexId)) {
            return repository
                    .findById(spexId)
                    .map(spex -> spex.getDetails().getCategory())
                    .map(SPEX_CATEGORY_MAPPER::toDto);
        } else {
            throw new ResourceNotFoundException(String.format("Spex %s does not exist", spexId));
        }
    }

    public boolean addCategory(final Long spexId, final Long id) {
        if (doSpexAndCategoryExist(spexId, id)) {
            return repository
                    .findById(spexId)
                    .map(spex -> categoryRepository
                            .findById(id)
                            .map(category -> {
                                spex.getDetails().setCategory(category);
                                detailsRepository.save(spex.getDetails());
                                return true;
                            })
                            .orElse(false))
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("Spex %s and/or category %s do not exist", spexId, id));
        }
    }

    public boolean removeCategory(final Long spexId) {
        if (doesSpexExist(spexId)) {
            return repository
                    .findById(spexId)
                    .filter(spex -> spex.getDetails().getCategory() != null)
                    .map(spex -> {
                        spex.getDetails().setCategory(null);
                        detailsRepository.save(spex.getDetails());
                        return true;
                    })
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("Spex %s does not exist", spexId));
        }
    }

    private boolean doesSpexExist(final Long id) {
        return repository.existsById(id);
    }

    private boolean doSpexAndCategoryExist(final Long spexId, final Long categoryId) {
        return doesSpexExist(spexId) && categoryRepository.existsById(categoryId);
    }
}
