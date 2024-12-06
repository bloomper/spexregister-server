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
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import nu.fgv.register.server.util.security.RequiresAdmin;
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

import static nu.fgv.register.server.task.category.TaskCategoryMapper.TASK_CATEGORY_MAPPER;
import static nu.fgv.register.server.task.category.TaskCategorySpecification.NO_FILTER;
import static nu.fgv.register.server.task.category.TaskCategorySpecification.hasIds;
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
public class TaskCategoryService {

    private final TaskCategoryRepository repository;
    private final PermissionService permissionService;

    @RequiresAdminOrEditorOrUser
    public List<TaskCategoryDto> findAll(final Sort sort) {
        return repository
                .findAll(sort, BasePermission.READ)
                .stream()
                .map(TASK_CATEGORY_MAPPER::toDto)
                .toList();
    }

    @RequiresAdminOrEditorOrUser
    public Window<TaskCategoryDto> find(final String filter, final int limit, final Sort sort, final ScrollPosition scrollPosition) {
        return hasText(filter) ?
                repository
                        .findBy(SpecificationsBuilder.<TaskCategory>builder().build(FilterParser.parse(filter), TaskCategorySpecification::new), BasePermission.READ, query -> query
                                .limit(limit)
                                .sortBy(sort)
                                .scroll(scrollPosition))
                        .map(TASK_CATEGORY_MAPPER::toDto) :
                repository
                        .findBy(NO_FILTER, BasePermission.READ, query -> query
                                .limit(limit)
                                .sortBy(sort)
                                .scroll(scrollPosition))
                        .map(TASK_CATEGORY_MAPPER::toDto);
    }

    @RequiresAdminOrEditorOrUser
    public Page<TaskCategoryDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<TaskCategory>builder().build(FilterParser.parse(filter), TaskCategorySpecification::new), pageable, BasePermission.READ)
                        .map(TASK_CATEGORY_MAPPER::toDto) :
                repository
                        .findAll(pageable, BasePermission.READ)
                        .map(TASK_CATEGORY_MAPPER::toDto);
    }

    @RequiresAdminOrEditorOrUser
    public TaskCategoryDto findById(final Long id) {
        return repository
                .findById0(id)
                .map(TASK_CATEGORY_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(TaskCategory.class, id));
    }

    @RequiresAdminOrEditorOrUser
    public List<TaskCategoryDto> findByIds(final List<Long> ids, final Sort sort) {
        return repository
                .findAll(hasIds(ids), sort, BasePermission.READ)
                .stream()
                .map(TASK_CATEGORY_MAPPER::toDto)
                .toList();
    }

    @RequiresAdmin
    public TaskCategoryDto create(final TaskCategoryCreateDto dto) {
        return Optional.of(TASK_CATEGORY_MAPPER.toModel(dto))
                .map(repository::save)
                .map(category -> {
                    final ObjectIdentity oid = toObjectIdentity(TaskCategory.class, category.getId());

                    permissionService.grantPermission(oid, BasePermission.ADMINISTRATION, ROLE_ADMIN_SID);
                    permissionService.grantPermission(oid, BasePermission.READ, ROLE_EDITOR_SID, ROLE_USER_SID);
                    permissionService.grantPermission(oid, BasePermission.WRITE, ROLE_EDITOR_SID);

                    return TASK_CATEGORY_MAPPER.toDto(category);
                })
                .orElseThrow(() -> new InternalErrorException("Could not create task category"));
    }

    @RequiresAdmin
    public TaskCategoryDto update(final TaskCategoryUpdateDto dto) {
        return partialUpdate(dto);
    }

    @RequiresAdmin
    public TaskCategoryDto partialUpdate(final TaskCategoryUpdateDto dto) {
        return repository
                .findById0(dto.getId())
                .map(permissionService::checkWritePermission)
                .map(category -> {
                    TASK_CATEGORY_MAPPER.toPartialModel(dto, category);
                    return category;
                })
                .map(repository::save)
                .map(TASK_CATEGORY_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(TaskCategory.class, dto.getId()));
    }

    @RequiresAdmin
    public void deleteById(final Long id) {
        if (doesTaskCategoryExist(id)) {
            repository.findById0(id)
                    .map(permissionService::checkDeletePermission)
                    .ifPresent(category -> {
                        permissionService.deleteAcl(toObjectIdentity(TaskCategory.class, id));
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
