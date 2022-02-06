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
public class SpexCategoryService {

    private final SpexCategoryRepository repository;
    private final SpexCategoryMapper mapper;

    public Page<SpexCategoryDto> find(final Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    public Optional<SpexCategoryDto> findById(final Long id) {
        return repository.findById(id).map(mapper::toDto);
    }

    public SpexCategoryDto save(final SpexCategoryDto dto) {
        return mapper.toDto(repository.save(mapper.toModel(dto)));
    }

    public Optional<SpexCategoryDto> update(final SpexCategoryDto dto) {
        if (repository.existsById(dto.getId())) {
            return Optional.of(mapper.toDto(repository.save(mapper.toModel(dto))));
        } else {
            return Optional.empty();
        }
    }

    public void deleteById(final Long id) {
        repository.deleteById(id);
    }

    public Optional<SpexCategoryDto> saveLogo(final Long id, final byte[] logo, final String contentType) {
        return repository.findById(id).map(model -> {
            model.setLogo(logo);
            model.setLogoContentType(hasText(contentType) ? contentType : FileUtil.detectMimeType(logo));
            repository.save(model);
            return mapper.toDto(model);
        });
    }

    public Optional<Pair<byte[], String>> getLogo(final Long id) {
        return repository.findById(id)
                .filter(model -> model.getLogo() != null && hasText(model.getLogoContentType()))
                .map(model -> Pair.of(model.getLogo(), model.getLogoContentType()));
    }
}
