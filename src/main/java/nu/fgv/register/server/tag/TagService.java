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

package nu.fgv.register.server.tag;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.graphql.GraphqlUtil.ScrollRequest;
import nu.fgv.register.server.util.security.RequiresAdminOrEditor;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static nu.fgv.register.server.tag.TagMapper.TAG_MAPPER;
import static nu.fgv.register.server.tag.TagSpecification.NO_FILTER;
import static nu.fgv.register.server.tag.TagSpecification.hasIds;
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
public class TagService {

    private final TagRepository repository;
    private final PermissionService permissionService;

    @RequiresAdminOrEditorOrUser
    public CountedWindow<TagDto> find(final String filter, final ScrollRequest scroll, final Sort sort) {
        return repository
                .findBy(hasText(filter) ?
                                SpecificationsBuilder.<Tag>builder().build(FilterParser.parse(filter), TagSpecification::new) :
                                NO_FILTER,
                        BasePermission.READ, query -> {
                            final long total = query.count();

                            return CountedWindow.of(query
                                    .limit(scroll.limit())
                                    .sortBy(sort)
                                    .scroll(scroll.positionFor(total))
                                    .map(TAG_MAPPER::toDto), total);
                        });
    }

    @RequiresAdminOrEditorOrUser
    public Page<TagDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<Tag>builder().build(FilterParser.parse(filter), TagSpecification::new), pageable, BasePermission.READ)
                        .map(TAG_MAPPER::toDto) :
                repository
                        .findAll(pageable, BasePermission.READ)
                        .map(TAG_MAPPER::toDto);
    }

    @RequiresAdminOrEditorOrUser
    public TagDto findById(final Long id) {
        return repository
                .findById0(id)
                .map(TAG_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(Tag.class, id));
    }

    @RequiresAdminOrEditorOrUser
    public boolean exists(final Long id) {
        return repository
                .findById0(id)
                .isPresent();
    }

    @RequiresAdminOrEditorOrUser
    public Iterable<Tag> streamByIds(final List<Long> ids, final String filter, final Sort sort) {
        return () -> {
            final Specification<Tag> spec;

            if (!ids.isEmpty()) {
                spec = hasIds(ids);
            } else if (hasText(filter)) {
                spec = SpecificationsBuilder.<Tag>builder()
                        .build(FilterParser.parse(filter), TagSpecification::new);
            } else {
                spec = null;
            }

            return repository.streamAll(spec, sort, BasePermission.READ)
                    .iterator();
        };
    }

    @RequiresAdminOrEditor
    public TagDto create(final TagCreateDto dto) {
        return Optional.of(TAG_MAPPER.toModel(dto))
                .map(model -> {
                    final Tag tag = repository.save(model);
                    final ObjectIdentity oid = toObjectIdentity(Tag.class, tag.getId());

                    permissionService.grantPermission(oid, BasePermission.ADMINISTRATION, ROLE_ADMIN_SID);
                    permissionService.grantPermission(oid, BasePermission.READ, ROLE_ADMIN_SID, ROLE_EDITOR_SID, ROLE_USER_SID);
                    permissionService.grantPermission(oid, BasePermission.WRITE, ROLE_EDITOR_SID);
                    permissionService.grantPermission(oid, BasePermission.DELETE, ROLE_EDITOR_SID);

                    return TAG_MAPPER.toDto(tag);
                })
                .orElseThrow(() -> new InternalErrorException("Could not create tag"));
    }

    @RequiresAdminOrEditor
    public TagDto update(final TagUpdateDto dto) {
        return partialUpdate(dto);
    }

    @RequiresAdminOrEditor
    public TagDto partialUpdate(final TagUpdateDto dto) {
        return repository
                .findById0(dto.id())
                .map(permissionService::checkWritePermission)
                .map(tag -> {
                    TAG_MAPPER.toPartialModel(dto, tag);
                    return tag;
                })
                .map(repository::save)
                .map(TAG_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(Tag.class, dto.id()));
    }

    @RequiresAdminOrEditor
    public void deleteById(final Long id) {
        if (doesTagExist(id)) {
            repository.findById0(id)
                    .map(permissionService::checkDeletePermission)
                    .ifPresent(tag -> {
                        permissionService.deleteAcl(toObjectIdentity(Tag.class, id));
                        repository.delete(tag);
                    });
        } else {
            throw new ResourceNotFoundException(Tag.class, id);
        }
    }

    private boolean doesTagExist(final Long id) {
        return repository.findById0(id).isPresent();
    }

}
