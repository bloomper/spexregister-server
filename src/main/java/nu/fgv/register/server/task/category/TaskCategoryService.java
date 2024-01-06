package nu.fgv.register.server.task.category;

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

import static nu.fgv.register.server.task.category.TaskCategoryMapper.TASK_CATEGORY_MAPPER;
import static nu.fgv.register.server.task.category.TaskCategorySpecification.hasIds;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class TaskCategoryService {

    private final TaskCategoryRepository repository;

    public List<TaskCategoryDto> findAll(final Sort sort) {
        return repository
                .findAll(sort)
                .stream()
                .map(TASK_CATEGORY_MAPPER::toDto)
                .toList();
    }

    public Page<TaskCategoryDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<TaskCategory>builder().build(FilterParser.parse(filter), TaskCategorySpecification::new), pageable)
                        .map(TASK_CATEGORY_MAPPER::toDto) :
                repository
                        .findAll(pageable)
                        .map(TASK_CATEGORY_MAPPER::toDto);
    }

    public Optional<TaskCategoryDto> findById(final Long id) {
        return repository
                .findById(id)
                .map(TASK_CATEGORY_MAPPER::toDto);
    }

    public List<TaskCategoryDto> findByIds(final List<Long> ids, final Sort sort) {
        return repository
                .findAll(hasIds(ids), sort)
                .stream()
                .map(TASK_CATEGORY_MAPPER::toDto)
                .toList();
    }

    public TaskCategoryDto create(final TaskCategoryCreateDto dto) {
        return TASK_CATEGORY_MAPPER.toDto(repository.save(TASK_CATEGORY_MAPPER.toModel(dto)));
    }

    public Optional<TaskCategoryDto> update(final TaskCategoryUpdateDto dto) {
        return repository
                .findById(dto.getId())
                .map(category -> TASK_CATEGORY_MAPPER.toModel(dto))
                .map(repository::save)
                .map(TASK_CATEGORY_MAPPER::toDto);
    }

    public Optional<TaskCategoryDto> partialUpdate(final TaskCategoryUpdateDto dto) {
        return repository
                .findById(dto.getId())
                .map(category -> {
                    TASK_CATEGORY_MAPPER.toPartialModel(dto, category);
                    return category;
                })
                .map(repository::save)
                .map(TASK_CATEGORY_MAPPER::toDto);
    }

    public void deleteById(final Long id) {
        repository.deleteById(id);
    }

}
