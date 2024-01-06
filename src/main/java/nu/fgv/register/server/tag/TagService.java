package nu.fgv.register.server.tag;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static nu.fgv.register.server.tag.TagMapper.TAG_MAPPER;
import static nu.fgv.register.server.tag.TagSpecification.hasIds;
import static org.springframework.util.StringUtils.hasText;

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
                .toList();
    }

    public Page<TagDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<Tag>builder().build(FilterParser.parse(filter), TagSpecification::new), pageable)
                        .map(TAG_MAPPER::toDto) :
                repository
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
                .findAll(hasIds(ids), sort)
                .stream()
                .map(TAG_MAPPER::toDto)
                .toList();
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
                .map(tag -> {
                    TAG_MAPPER.toPartialModel(dto, tag);
                    return tag;
                })
                .map(repository::save)
                .map(TAG_MAPPER::toDto);
    }

    public void deleteById(final Long id) {
        repository.deleteById(id);
    }

}
