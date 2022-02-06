package nu.fgv.register.server.spex;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.FileUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class SpexService {

    private final SpexRepository repository;
    private final SpexCategoryRepository categoryRepository;
    private final SpexMapper mapper;

    public Page<SpexDto> find(final boolean includeRevivals, final Pageable pageable) {
        if (includeRevivals) {
            return repository.findAll(pageable).map(mapper::toDto);
        } else {
            return repository.findAllByParentIsNull(pageable).map(mapper::toDto);
        }
    }

    public Optional<SpexDto> findById(final Long id) {
        return repository.findById(id).map(mapper::toDto);
    }

    public SpexDto save(final SpexDto dto) {
        return mapper.toDto(repository.save(mapper.toModel(dto)));
    }

    public Optional<SpexDto> update(final SpexDto dto) {
        if (repository.existsById(dto.getId())) {
            return Optional.of(mapper.toDto(repository.save(mapper.toModel(dto))));
        } else {
            return Optional.empty();
        }
    }

    public void deleteById(final Long id) {
        repository.deleteById(id);
    }

    public Optional<SpexDto> savePoster(final Long id, final byte[] poster, final String contentType) {
        return repository.findById(id).map(model -> {
            model.getDetails().setPoster(poster);
            model.getDetails().setPosterContentType(hasText(contentType) ? contentType : FileUtil.detectMimeType(poster));
            repository.save(model);
            return mapper.toDto(model);
        });
    }

    public Optional<Pair<byte[], String>> getPoster(final Long id) {
        return repository.findById(id)
                .map(Spex::getDetails)
                .filter(model -> model.getPoster() != null && hasText(model.getPosterContentType()))
                .map(model -> Pair.of(model.getPoster(), model.getPosterContentType()));
    }

    public Page<SpexDto> findRevivals(final Pageable pageable) {
        return repository.findAllByParentIsNotNull(pageable).map(mapper::toDto);
    }

    public Page<SpexDto> findRevivalsByParent(final Long id, final Pageable pageable) {
        return repository.findRevivalsByParent(id, pageable).map(mapper::toDto);
    }

    public Optional<SpexDto> addRevival(final Long id, final String year) {
        if (repository.existsById(id) && !repository.existsRevivalByParentAndYear(id, year)) {
            return repository.findById(id)
                    .map(parent -> {
                        final Spex revival = new Spex();
                        revival.setDetails(parent.getDetails());
                        revival.setParent(parent);
                        revival.setYear(year);
                        return repository.save(revival);
                    })
                    .map(mapper::toDto);
        } else {
            return Optional.empty();
        }
    }

    public boolean removeRevival(final Long id, final String year) {
        if (repository.existsById(id) && repository.existsRevivalByParentAndYear(id, year)) {
            repository.findByParentAndYear(id, year)
                    .ifPresent(revival -> repository.deleteById(revival.getId()));
            return true;
        } else {
            return false;
        }
    }

    public Optional<SpexDto> updateCategory(final Long id, final Long categoryId) {
        if (repository.existsById(id) && categoryRepository.existsById(categoryId)) {
            repository.findById(id).ifPresent(spex ->
                    categoryRepository.findById(categoryId).ifPresent(category -> {
                        spex.getDetails().setCategory(category);
                        repository.save(spex);
                    }));
            return findById(id);
        } else {
            return Optional.empty();
        }
    }

}
