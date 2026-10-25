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

package nu.fgv.register.server.task.category;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.security.policy.PermissionService;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.graphql.GraphqlUtil.ScrollRequest;
import nu.fgv.register.server.util.security.RequiresAdmin;
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

import static nu.fgv.register.server.task.category.TaskCategoryMapper.TASK_CATEGORY_MAPPER;
import static nu.fgv.register.server.task.category.TaskCategorySpecification.NO_FILTER;
import static nu.fgv.register.server.task.category.TaskCategorySpecification.hasIds;
import static org.springframework.util.StringUtils.hasText;

/**
 * Task categories. Only administrators change them.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class TaskCategoryService {

    private final TaskCategoryRepository repository;
    private final PermissionService permissionService;

    /**
     * The task categories the caller may read that match the filter.
     *
     * @param filter a filter expression, or blank for all
     * @param scroll the window to read
     * @param sort   the order
     * @return the window with the total count
     */
    @RequiresReferenceDataReader
    public CountedWindow<TaskCategoryDto> find(final String filter, final ScrollRequest scroll, final Sort sort) {
        return repository
                .findBy(hasText(filter) ?
                                SpecificationsBuilder.<TaskCategory>builder().build(FilterParser.parse(filter), TaskCategorySpecification::new) :
                                NO_FILTER,
                        Permission.READ, query -> {
                            final long total = query.count();

                            return CountedWindow.of(query
                                    .limit(scroll.limit())
                                    .sortBy(sort)
                                    .scroll(scroll.positionFor(total))
                                    .map(TASK_CATEGORY_MAPPER::toDto), total);
                        });
    }

    /**
     * The task categories the caller may read that match the filter, for the REST API.
     *
     * @param filter   a filter expression, or blank for all
     * @param pageable the page to read
     * @return the page
     */
    @RequiresAdminOrEditorOrUser
    public Page<TaskCategoryDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<TaskCategory>builder().build(FilterParser.parse(filter), TaskCategorySpecification::new), pageable, Permission.READ)
                        .map(TASK_CATEGORY_MAPPER::toDto) :
                repository
                        .findAll(pageable, Permission.READ)
                        .map(TASK_CATEGORY_MAPPER::toDto);
    }

    /**
     * @param id the task category id
     * @return the task category
     * @throws nu.fgv.register.server.util.error.ResourceNotFoundException when there is no such task category
     */
    @RequiresReferenceDataReader
    public TaskCategoryDto findById(final Long id) {
        return repository
                .findById0(id)
                .map(TASK_CATEGORY_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(TaskCategory.class, id));
    }

    /**
     * @param id the task category id
     * @return whether the task category exists
     * @throws org.springframework.security.access.AccessDeniedException when it exists but may not be read
     */
    @RequiresAdminOrEditorOrUser
    public boolean exists(final Long id) {
        return repository
                .findById0(id)
                .isPresent();
    }

    /**
     * The task categories to export, streamed when iterated, so it must be consumed inside a transaction.
     *
     * @param ids    the task categories to export, or empty to use the filter
     * @param filter a filter expression when no ids are given, or blank for all
     * @param sort   the order
     * @return the task categories the caller may read
     */
    @RequiresAdminOrEditorOrUser
    public Iterable<TaskCategory> streamByIds(final List<Long> ids, final String filter, final Sort sort) {
        return () -> {
            final Specification<TaskCategory> spec;

            if (!ids.isEmpty()) {
                spec = hasIds(ids);
            } else if (hasText(filter)) {
                spec = SpecificationsBuilder.<TaskCategory>builder()
                        .build(FilterParser.parse(filter), TaskCategorySpecification::new);
            } else {
                spec = null;
            }

            return repository.streamAll(spec, sort, Permission.READ)
                    .iterator();
        };
    }

    /**
     * @param dto the new task category
     * @return the created task category
     */
    @RequiresAdmin
    public TaskCategoryDto create(final TaskCategoryCreateDto dto) {
        return Optional.of(TASK_CATEGORY_MAPPER.toModel(dto))
                .map(repository::save)
                .map(category -> {
                    return TASK_CATEGORY_MAPPER.toDto(category);
                })
                .orElseThrow(() -> new InternalErrorException("Could not create task category"));
    }

    /**
     * The same as {@link #partialUpdate}.
     *
     * @param dto the task category id and new values
     * @return the updated task category
     */
    @RequiresAdmin
    public TaskCategoryDto update(final TaskCategoryUpdateDto dto) {
        return partialUpdate(dto);
    }

    /**
     * Changes the values given; null values are left unchanged.
     *
     * @param dto the task category id and new values
     * @return the updated task category
     * @throws nu.fgv.register.server.util.error.ResourceNotFoundException when there is no such task category
     */
    @RequiresAdmin
    public TaskCategoryDto partialUpdate(final TaskCategoryUpdateDto dto) {
        return repository
                .findById0(dto.id())
                .map(permissionService::checkWritePermission)
                .map(category -> {
                    TASK_CATEGORY_MAPPER.toPartialModel(dto, category);
                    return category;
                })
                .map(repository::save)
                .map(TASK_CATEGORY_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(TaskCategory.class, dto.id()));
    }

    /**
     * @param id the task category id
     * @throws nu.fgv.register.server.util.error.ResourceNotFoundException when there is no such task category
     */
    @RequiresAdmin
    public void deleteById(final Long id) {
        if (doesTaskCategoryExist(id)) {
            repository.findById0(id)
                    .map(permissionService::checkDeletePermission)
                    .ifPresent(category -> {
                        repository.delete(category);
                    });
        } else {
            throw new ResourceNotFoundException(TaskCategory.class, id);
        }
    }

    private boolean doesTaskCategoryExist(final Long id) {
        return repository.findById0(id).isPresent();
    }
}
