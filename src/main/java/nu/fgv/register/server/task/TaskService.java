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
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.task.category.TaskCategory;
import nu.fgv.register.server.task.category.TaskCategoryDto;
import nu.fgv.register.server.task.category.TaskCategoryRepository;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.util.error.ResourceNoValueException;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.error.ResourcesNotFoundException;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import nu.fgv.register.server.util.security.RequiresAdmin;
import nu.fgv.register.server.util.security.RequiresAdminOrEditor;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static nu.fgv.register.server.task.TaskMapper.TASK_MAPPER;
import static nu.fgv.register.server.task.TaskSpecification.NO_FILTER;
import static nu.fgv.register.server.task.TaskSpecification.hasIds;
import static nu.fgv.register.server.task.category.TaskCategoryMapper.TASK_CATEGORY_MAPPER;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_ADMIN_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_EDITOR_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_USER_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.springframework.util.StringUtils.hasText;

/**
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

    @RequiresAdminOrEditorOrUser
    public Window<TaskDto> find(final String filter, final int limit, final Sort sort, final ScrollPosition scrollPosition) {
        return hasText(filter) ?
                repository
                        .findBy(SpecificationsBuilder.<Task>builder().build(FilterParser.parse(filter), TaskSpecification::new), BasePermission.READ, query -> query
                                .limit(limit)
                                .sortBy(sort)
                                .scroll(scrollPosition))
                        .map(TASK_MAPPER::toDto) :
                repository
                        .findBy(NO_FILTER, BasePermission.READ, query -> query
                                .limit(limit)
                                .sortBy(sort)
                                .scroll(scrollPosition))
                        .map(TASK_MAPPER::toDto);
    }

    @RequiresAdminOrEditorOrUser
    public Page<TaskDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<Task>builder().build(FilterParser.parse(filter), TaskSpecification::new), pageable, BasePermission.READ)
                        .map(TASK_MAPPER::toDto) :
                repository
                        .findAll(pageable, BasePermission.READ)
                        .map(TASK_MAPPER::toDto);
    }

    @RequiresAdminOrEditorOrUser
    public TaskDto findById(final Long id) {
        return repository
                .findById0(id)
                .map(TASK_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(Task.class, id));
    }

    @RequiresAdminOrEditorOrUser
    public Iterable<Task> streamByIds(final List<Long> ids, final Sort sort) {
        return () -> repository.streamAll(ids.isEmpty() ? null : hasIds(ids), sort, BasePermission.READ)
                .iterator();
    }

    @RequiresAdmin
    public TaskDto create(final TaskCreateDto dto) {
        return Optional.of(TASK_MAPPER.toModel(dto))
                .map(model -> {
                    final Task task = repository.save(model);
                    final ObjectIdentity oid = toObjectIdentity(Task.class, task.getId());

                    permissionService.grantPermission(oid, BasePermission.ADMINISTRATION, ROLE_ADMIN_SID);
                    permissionService.grantPermission(oid, BasePermission.READ, ROLE_ADMIN_SID, ROLE_EDITOR_SID, ROLE_USER_SID);
                    permissionService.grantPermission(oid, BasePermission.WRITE, ROLE_EDITOR_SID);

                    return TASK_MAPPER.toDto(task);
                })
                .orElseThrow(() -> new InternalErrorException("Could not create task"));
    }

    @RequiresAdminOrEditor
    public TaskDto update(final TaskUpdateDto dto) {
        return partialUpdate(dto);
    }

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

    @RequiresAdmin
    public void deleteById(final Long id) {
        if (doesTaskExist(id)) {
            repository.findById0(id)
                    .map(permissionService::checkDeletePermission)
                    .ifPresent(task -> {
                        permissionService.deleteAcl(toObjectIdentity(Task.class, id));
                        repository.delete(task);
                    });
        } else {
            throw new ResourceNotFoundException(Task.class, id);
        }
    }

    @RequiresAdminOrEditorOrUser
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
