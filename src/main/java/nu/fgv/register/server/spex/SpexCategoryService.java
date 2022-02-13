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

import static nu.fgv.register.server.spex.SpexCategoryMapper.SPEX_CATEGORY_MAPPER;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class SpexCategoryService {

    private final SpexCategoryRepository repository;

    public Page<SpexCategoryDto> find(final Pageable pageable) {
        return repository.findAll(pageable).map(SPEX_CATEGORY_MAPPER::toDto);
    }

    public Optional<SpexCategoryDto> findById(final Long id) {
        return repository.findById(id).map(SPEX_CATEGORY_MAPPER::toDto);
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
                .map(model -> {
                    SPEX_CATEGORY_MAPPER.toPartialModel(dto, model);
                    return model;
                })
                .map(repository::save)
                .map(SPEX_CATEGORY_MAPPER::toDto);
    }

    public void deleteById(final Long id) {
        repository.deleteById(id);
    }

    public Optional<SpexCategoryDto> saveLogo(final Long id, final byte[] logo, final String contentType) {
        return repository.findById(id).map(model -> {
            model.setLogo(logo);
            model.setLogoContentType(hasText(contentType) ? contentType : FileUtil.detectMimeType(logo));
            repository.save(model);
            return SPEX_CATEGORY_MAPPER.toDto(model);
        });
    }

    public Optional<Pair<byte[], String>> getLogo(final Long id) {
        return repository.findById(id)
                .filter(model -> model.getLogo() != null && hasText(model.getLogoContentType()))
                .map(model -> Pair.of(model.getLogo(), model.getLogoContentType()));
    }
}
