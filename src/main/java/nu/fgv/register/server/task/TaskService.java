/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nu.fgv.register.server.task;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.security.policy.PermissionService;
import nu.fgv.register.server.task.category.TaskCategory;
import nu.fgv.register.server.task.category.TaskCategoryDto;
import nu.fgv.register.server.task.category.TaskCategoryRepository;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.util.error.ResourceNoValueException;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.error.ResourcesNotFoundException;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.graphql.GraphqlUtil.ScrollRequest;
import nu.fgv.register.server.util.security.RequiresAdmin;
import nu.fgv.register.server.util.security.RequiresAdminOrEditor;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import nu.fgv.register.server.util.security.RequiresReferenceDataReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import nu.fgv.register.server.security.policy.Permission;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static nu.fgv.register.server.task.TaskMapper.TASK_MAPPER;
import static nu.fgv.register.server.task.TaskSpecification.NO_FILTER;
import static nu.fgv.register.server.task.TaskSpecification.hasIds;
import static nu.fgv.register.server.task.category.TaskCategoryMapper.TASK_CATEGORY_MAPPER;
import static org.springframework.util.StringUtils.hasText;

/**
 * Tasks and their categories.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class TaskService {

    private final TaskRepository repository;
    private final TaskCategoryRepository categoryRepository;
    private final PermissionService permissionService;

    /**
     * The tasks the caller may read that match the filter.
     *
     * @param filter a filter expression, or blank for all
     * @param scroll the window to read
     * @param sort   the order
     * @return the window with the total count
     */
    @RequiresReferenceDataReader
    public CountedWindow<TaskDto> find(final String filter, final ScrollRequest scroll, final Sort sort) {
        return repository
                .findBy(hasText(filter) ?
                                SpecificationsBuilder.<Task>builder().build(FilterParser.parse(filter), TaskSpecification::new) :
                                NO_FILTER,
                        Permission.READ, query -> {
                            final long total = query.count();

                            return CountedWindow.of(query
                                    .limit(scroll.limit())
                                    .sortBy(sort)
                                    .scroll(scroll.positionFor(total))
                                    .map(TASK_MAPPER::toDto), total);
                        });
    }

    /**
     * The tasks the caller may read that match the filter, for the REST API.
     *
     * @param filter   a filter expression, or blank for all
     * @param pageable the page to read
     * @return the page
     */
    @RequiresAdminOrEditorOrUser
    public Page<TaskDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<Task>builder().build(FilterParser.parse(filter), TaskSpecification::new), pageable, Permission.READ)
                        .map(TASK_MAPPER::toDto) :
                repository
                        .findAll(pageable, Permission.READ)
                        .map(TASK_MAPPER::toDto);
    }

    /**
     * @param id the task id
     * @return the task
     * @throws nu.fgv.register.server.util.error.ResourceNotFoundException when there is no such task
     */
    @RequiresReferenceDataReader
    public TaskDto findById(final Long id) {
        return repository
                .findById0(id)
                .map(TASK_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(Task.class, id));
    }

    /**
     * @param id the task id
     * @return whether the task exists
     * @throws org.springframework.security.access.AccessDeniedException when it exists but may not be read
     */
    @RequiresAdminOrEditorOrUser
    public boolean exists(final Long id) {
        return repository
                .findById0(id)
                .isPresent();
    }

    /**
     * The tasks to export, streamed when iterated, so it must be consumed inside a transaction.
     *
     * @param ids    the tasks to export, or empty to use the filter
     * @param filter a filter expression when no ids are given, or blank for all
     * @param sort   the order
     * @return the tasks the caller may read
     */
    @RequiresAdminOrEditorOrUser
    public Iterable<Task> streamByIds(final List<Long> ids, final String filter, final Sort sort) {
        return () -> {
            final Specification<Task> spec;

            if (!ids.isEmpty()) {
                spec = hasIds(ids);
            } else if (hasText(filter)) {
                spec = SpecificationsBuilder.<Task>builder()
                        .build(FilterParser.parse(filter), TaskSpecification::new);
            } else {
                spec = null;
            }

            return repository.streamAll(spec, sort, Permission.READ)
                    .iterator();
        };
    }

    /**
     * @param dto the new task
     * @return the created task
     */
    @RequiresAdmin
    public TaskDto create(final TaskCreateDto dto) {
        return Optional.of(TASK_MAPPER.toModel(dto))
                .map(model -> {
                    final Task task = repository.save(model);

                    return TASK_MAPPER.toDto(task);
                })
                .orElseThrow(() -> new InternalErrorException("Could not create task"));
    }

    /**
     * The same as {@link #partialUpdate}.
     *
     * @param dto the task id and new values
     * @return the updated task
     */
    @RequiresAdminOrEditor
    public TaskDto update(final TaskUpdateDto dto) {
        return partialUpdate(dto);
    }

    /**
     * Changes the values given; null values are left unchanged.
     *
     * @param dto the task id and new values
     * @return the updated task
     * @throws nu.fgv.register.server.util.error.ResourceNotFoundException when there is no such task
     */
    @RequiresAdminOrEditor
    public TaskDto partialUpdate(final TaskUpdateDto dto) {
        return repository
                .findById0(dto.id())
                .map(permissionService::checkWritePermission)
                .map(task -> {
                    TASK_MAPPER.toPartialModel(dto, task);
                    return task;
                })
                .map(repository::save)
                .map(TASK_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(Task.class, dto.id()));
    }

    /**
     * @param id the task id
     * @throws nu.fgv.register.server.util.error.ResourceNotFoundException when there is no such task
     */
    @RequiresAdmin
    public void deleteById(final Long id) {
        if (doesTaskExist(id)) {
            repository.findById0(id)
                    .map(permissionService::checkDeletePermission)
                    .ifPresent(task -> {
                        repository.delete(task);
                    });
        } else {
            throw new ResourceNotFoundException(Task.class, id);
        }
    }

    /**
     * @param id the task id
     * @return the task's category
     * @throws nu.fgv.register.server.util.error.ResourceNoValueException when the task has no category
     */
    @RequiresReferenceDataReader
    public TaskCategoryDto findCategoryByTask(final Long id) {
        if (doesTaskExist(id)) {
            return repository
                    .findById0(id)
                    .filter(task -> task.getCategory() != null)
                    .map(Task::getCategory)
                    .map(TASK_CATEGORY_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNoValueException(Task.class, Task_.CATEGORY, id));
        } else {
            throw new ResourceNotFoundException(Task.class, id);
        }
    }

    /**
     * Puts the task in the category.
     *
     * @param taskId the task id
     * @param id     the category id
     */
    @RequiresAdmin
    public void addCategory(final Long taskId, final Long id) {
        if (doTaskAndCategoryExist(taskId, id)) {
            repository
                    .findById0(taskId)
                    .map(permissionService::checkWritePermission)
                    .ifPresent(task -> categoryRepository
                            .findById(id)
                            .ifPresent(category -> {
                                task.setCategory(category);
                                repository.save(task);
                            })
                    );
        } else {
            throw new ResourcesNotFoundException(List.of(Task.class, TaskCategory.class), taskId, id);
        }
    }

    /**
     * Takes the task out of its category.
     *
     * @param id the task id
     */
    @RequiresAdmin
    public void removeCategory(final Long id) {
        if (doesTaskExist(id)) {
            repository
                    .findById0(id)
                    .map(permissionService::checkWritePermission)
                    .ifPresent(task -> {
                        task.setCategory(null);
                        repository.save(task);
                    });
        } else {
            throw new ResourceNotFoundException(Task.class, id);
        }
    }

    private boolean doesTaskExist(final Long id) {
        return repository.findById0(id).isPresent();
    }

    private boolean doTaskAndCategoryExist(final Long taskId, final Long categoryId) {
        return doesTaskExist(taskId) && categoryRepository.findById(categoryId).isPresent();
    }

}
