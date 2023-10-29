package nu.fgv.register.server.spex.category;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.FileUtil;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static nu.fgv.register.server.spex.category.SpexCategoryMapper.SPEX_CATEGORY_MAPPER;
import static nu.fgv.register.server.spex.category.SpexCategorySpecification.hasIds;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class SpexCategoryService {

    private final SpexCategoryRepository repository;

    public List<SpexCategoryDto> findAll(final Sort sort) {
        return repository
                .findAll(sort)
                .stream()
                .map(SPEX_CATEGORY_MAPPER::toDto)
                .collect(Collectors.toList());
    }

    public Page<SpexCategoryDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<SpexCategory>builder().build(FilterParser.parse(filter), SpexCategorySpecification::new), pageable)
                        .map(SPEX_CATEGORY_MAPPER::toDto) :
                repository
                        .findAll(pageable)
                        .map(SPEX_CATEGORY_MAPPER::toDto);
    }

    public Optional<SpexCategoryDto> findById(final Long id) {
        return repository
                .findById(id)
                .map(SPEX_CATEGORY_MAPPER::toDto);
    }

    public List<SpexCategoryDto> findByIds(final List<Long> ids, final Sort sort) {
        return repository
                .findAll(hasIds(ids), sort)
                .stream()
                .map(SPEX_CATEGORY_MAPPER::toDto)
                .collect(Collectors.toList());
    }

    public SpexCategoryDto create(final SpexCategoryCreateDto dto) {
        return SPEX_CATEGORY_MAPPER.toDto(repository.save(SPEX_CATEGORY_MAPPER.toModel(dto)));
    }

    public Optional<SpexCategoryDto> update(final SpexCategoryUpdateDto dto) {
        return repository
                .findById(dto.getId())
                .map(category -> SPEX_CATEGORY_MAPPER.toModel(dto))
                .map(repository::save)
                .map(SPEX_CATEGORY_MAPPER::toDto);
    }

    public Optional<SpexCategoryDto> partialUpdate(final SpexCategoryUpdateDto dto) {
        return repository
                .findById(dto.getId())
                .map(category -> {
                    SPEX_CATEGORY_MAPPER.toPartialModel(dto, category);
                    return category;
                })
                .map(repository::save)
                .map(SPEX_CATEGORY_MAPPER::toDto);
    }

    public void deleteById(final Long id) {
        repository.deleteById(id);
    }

    public Optional<SpexCategoryDto> saveLogo(final Long spexId, final byte[] logo, final String contentType) {
        return repository
                .findById(spexId)
                .map(category -> {
                    category.setLogo(logo);
                    category.setLogoContentType(hasText(contentType) ? contentType : FileUtil.detectMimeType(logo));
                    repository.save(category);
                    return SPEX_CATEGORY_MAPPER.toDto(category);
                });
    }

    public Optional<SpexCategoryDto> deleteLogo(final Long spexId) {
        return repository
                .findById(spexId)
                .map(category -> {
                    category.setLogo(null);
                    category.setLogoContentType(null);
                    repository.save(category);
                    return SPEX_CATEGORY_MAPPER.toDto(category);
                });
    }

    public Optional<Pair<byte[], String>> getLogo(final Long spexId) {
        return repository.findById(spexId)
                .filter(category -> category.getLogo() != null && hasText(category.getLogoContentType()))
                .map(category -> Pair.of(category.getLogo(), category.getLogoContentType()));
    }
}
