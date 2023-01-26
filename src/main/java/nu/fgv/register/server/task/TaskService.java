package nu.fgv.register.server.task;

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

import static nu.fgv.register.server.task.TaskMapper.TASK_MAPPER;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class TaskService {

    private final TaskRepository repository;
    private final TaskCategoryRepository categoryRepository;

    public List<TaskDto> findAll(final Sort sort) {
        return repository.findAll(sort).stream().map(TASK_MAPPER::toDto).collect(Collectors.toList());
    }

    public Page<TaskDto> find(final Pageable pageable) {
        return repository.findAll(pageable).map(TASK_MAPPER::toDto);
    }

    public Optional<TaskDto> findById(final Long id) {
        return repository.findById(id).map(TASK_MAPPER::toDto);
    }

    public List<TaskDto> findByIds(final List<Long> ids, final Sort sort) {
        return repository.findByIds(ids, sort).stream().map(TASK_MAPPER::toDto).collect(Collectors.toList());
    }

    public TaskDto create(final TaskCreateDto dto) {
        return TASK_MAPPER.toDto(repository.save(TASK_MAPPER.toModel(dto)));
    }

    public Optional<TaskDto> update(final TaskUpdateDto dto) {
        return partialUpdate(dto);
    }

    public Optional<TaskDto> partialUpdate(final TaskUpdateDto dto) {
        return repository
                .findById(dto.getId())
                .map(model -> {
                    TASK_MAPPER.toPartialModel(dto, model);
                    return model;
                })
                .map(repository::save)
                .map(TASK_MAPPER::toDto);
    }

    public void deleteById(final Long id) {
        repository.deleteById(id);
    }

    public Optional<TaskDto> updateCategory(final Long id, final Long categoryId) {
        if (repository.existsById(id) && categoryRepository.existsById(categoryId)) {
            repository.findById(id).ifPresent(task ->
                    categoryRepository.findById(categoryId).ifPresent(category -> {
                        task.setCategory(category);
                        repository.save(task);
                    }));
            return findById(id);
        } else {
            return Optional.empty();
        }
    }

    public Optional<TaskDto> removeCategory(final Long id) {
        return repository
                .findById(id)
                .map(task -> {
                    task.setCategory(null);
                    repository.save(task);
                    return task;
                })
                .map(TASK_MAPPER::toDto);
    }

}
