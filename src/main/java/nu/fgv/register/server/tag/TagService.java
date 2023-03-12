package nu.fgv.register.server.tag;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static nu.fgv.register.server.tag.TagMapper.TAG_MAPPER;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class TagService {

    private final TagRepository repository;

    public List<TagDto> findAll(final Sort sort) {
        return repository
                .findAll(sort)
                .stream()
                .map(TAG_MAPPER::toDto)
                .collect(Collectors.toList());
    }

    public Page<TagDto> find(final Pageable pageable) {
        return repository
                .findAll(pageable)
                .map(TAG_MAPPER::toDto);
    }

    public Optional<TagDto> findById(final Long id) {
        return repository
                .findById(id)
                .map(TAG_MAPPER::toDto);
    }

    public List<TagDto> findByIds(final List<Long> ids, final Sort sort) {
        return repository
                .findByIds(ids, sort)
                .stream()
                .map(TAG_MAPPER::toDto)
                .collect(Collectors.toList());
    }

    public TagDto create(final TagCreateDto dto) {
        return TAG_MAPPER.toDto(repository.save(TAG_MAPPER.toModel(dto)));
    }

    public Optional<TagDto> update(final TagUpdateDto dto) {
        return repository
                .findById(dto.getId())
                .map(tag -> TAG_MAPPER.toModel(dto))
                .map(repository::save)
                .map(TAG_MAPPER::toDto);
    }

    public Optional<TagDto> partialUpdate(final TagUpdateDto dto) {
        return repository
                .findById(dto.getId())
                .map(model -> {
                    TAG_MAPPER.toPartialModel(dto, model);
                    return model;
                })
                .map(repository::save)
                .map(TAG_MAPPER::toDto);
    }

    public void deleteById(final Long id) {
        repository.deleteById(id);
    }

}
