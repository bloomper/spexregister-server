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
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import nu.fgv.register.server.util.security.RequiresAdminOrEditor;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static nu.fgv.register.server.tag.TagMapper.TAG_MAPPER;
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
    public List<TagDto> findAll(final Sort sort) {
        return repository
                .findAll(sort, BasePermission.READ)
                .stream()
                .map(TAG_MAPPER::toDto)
                .toList();
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
    public Optional<TagDto> findById(final Long id) {
        return repository
                .findById0(id)
                .map(TAG_MAPPER::toDto);
    }

    @RequiresAdminOrEditorOrUser
    public List<TagDto> findByIds(final List<Long> ids, final Sort sort) {
        return repository
                .findAll(hasIds(ids), sort, BasePermission.READ)
                .stream()
                .map(TAG_MAPPER::toDto)
                .toList();
    }

    @RequiresAdminOrEditor
    public TagDto create(final TagCreateDto dto) {
        return Optional.of(TAG_MAPPER.toModel(dto))
                .map(model -> {
                    final Tag tag = repository.save(model);
                    final ObjectIdentity oid = toObjectIdentity(Tag.class, tag.getId());

                    permissionService.grantPermission(oid, BasePermission.READ, ROLE_ADMIN_SID, ROLE_EDITOR_SID);
                    permissionService.grantPermission(oid, BasePermission.WRITE, ROLE_ADMIN_SID, ROLE_EDITOR_SID);
                    permissionService.grantPermission(oid, BasePermission.DELETE, ROLE_ADMIN_SID, ROLE_EDITOR_SID);
                    permissionService.grantPermission(oid, BasePermission.READ, ROLE_USER_SID);

                    return TAG_MAPPER.toDto(tag);
                })
                .orElse(null);
    }

    @RequiresAdminOrEditor
    public Optional<TagDto> update(final TagUpdateDto dto) {
        return partialUpdate(dto);
    }

    @RequiresAdminOrEditor
    public Optional<TagDto> partialUpdate(final TagUpdateDto dto) {
        return repository
                .findById0(dto.getId())
                .map(tag -> {
                    TAG_MAPPER.toPartialModel(dto, tag);
                    return tag;
                })
                .map(repository::save)
                .map(TAG_MAPPER::toDto);
    }

    @RequiresAdminOrEditor
    public void deleteById(final Long id) {
        repository.deleteById(id);
        permissionService.deleteAcl(toObjectIdentity(Tag.class, id));
    }

}
