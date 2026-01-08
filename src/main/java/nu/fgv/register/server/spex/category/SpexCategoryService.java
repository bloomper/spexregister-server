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

package nu.fgv.register.server.spex.category;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.util.FileUtil;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.util.error.ResourceNoValueException;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import nu.fgv.register.server.util.security.RequiresAdmin;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.util.Pair;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static nu.fgv.register.server.spex.category.SpexCategoryMapper.SPEX_CATEGORY_MAPPER;
import static nu.fgv.register.server.spex.category.SpexCategorySpecification.NO_FILTER;
import static nu.fgv.register.server.spex.category.SpexCategorySpecification.hasIds;
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
public class SpexCategoryService {

    private final SpexCategoryRepository repository;
    private final PermissionService permissionService;

    @RequiresAdminOrEditorOrUser
    public List<SpexCategoryDto> findAll(final Sort sort) {
        return repository
                .findAll(sort, BasePermission.READ)
                .stream()
                .map(SPEX_CATEGORY_MAPPER::toDto)
                .toList();
    }

    @RequiresAdminOrEditorOrUser
    public Window<SpexCategoryDto> find(final String filter, final int limit, final Sort sort, final ScrollPosition scrollPosition) {
        return hasText(filter) ?
                repository
                        .findBy(SpecificationsBuilder.<SpexCategory>builder().build(FilterParser.parse(filter), SpexCategorySpecification::new), BasePermission.READ, query -> query
                                .limit(limit)
                                .sortBy(sort)
                                .scroll(scrollPosition))
                        .map(SPEX_CATEGORY_MAPPER::toDto) :
                repository
                        .findBy(NO_FILTER, BasePermission.READ, query -> query
                                .limit(limit)
                                .sortBy(sort)
                                .scroll(scrollPosition))
                        .map(SPEX_CATEGORY_MAPPER::toDto);
    }

    @RequiresAdminOrEditorOrUser
    public Page<SpexCategoryDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<SpexCategory>builder().build(FilterParser.parse(filter), SpexCategorySpecification::new), pageable, BasePermission.READ)
                        .map(SPEX_CATEGORY_MAPPER::toDto) :
                repository
                        .findAll(pageable, BasePermission.READ)
                        .map(SPEX_CATEGORY_MAPPER::toDto);
    }

    @RequiresAdminOrEditorOrUser
    public SpexCategoryDto findById(final Long id) {
        return repository
                .findById0(id)
                .map(SPEX_CATEGORY_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(SpexCategory.class, id));
    }

    @RequiresAdminOrEditorOrUser
    public List<SpexCategoryDto> findByIds(final List<Long> ids, final Sort sort) {
        return repository
                .findAll(hasIds(ids), sort, BasePermission.READ)
                .stream()
                .map(SPEX_CATEGORY_MAPPER::toDto)
                .toList();
    }

    @RequiresAdmin
    public SpexCategoryDto create(final SpexCategoryCreateDto dto) {
        return Optional.of(SPEX_CATEGORY_MAPPER.toModel(dto))
                .map(repository::save)
                .map(category -> {
                    final ObjectIdentity oid = toObjectIdentity(SpexCategory.class, category.getId());

                    permissionService.grantPermission(oid, BasePermission.ADMINISTRATION, ROLE_ADMIN_SID);
                    permissionService.grantPermission(oid, BasePermission.READ, ROLE_ADMIN_SID, ROLE_EDITOR_SID, ROLE_USER_SID);

                    return SPEX_CATEGORY_MAPPER.toDto(category);
                })
                .orElseThrow(() -> new InternalErrorException("Could not create spex category"));
    }

    @RequiresAdmin
    public SpexCategoryDto update(final SpexCategoryUpdateDto dto) {
        return partialUpdate(dto);
    }

    @RequiresAdmin
    public SpexCategoryDto partialUpdate(final SpexCategoryUpdateDto dto) {
        return repository
                .findById0(dto.id())
                .map(permissionService::checkWritePermission)
                .map(category -> {
                    SPEX_CATEGORY_MAPPER.toPartialModel(dto, category);
                    return category;
                })
                .map(repository::save)
                .map(SPEX_CATEGORY_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(SpexCategory.class, dto.id()));
    }

    @RequiresAdmin
    public void deleteById(final Long id) {
        if (doesSpexCategoryExist(id)) {
            repository.findById0(id)
                    .map(permissionService::checkDeletePermission)
                    .ifPresent(category -> {
                        permissionService.deleteAcl(toObjectIdentity(SpexCategory.class, id));
                        repository.delete(category);
                    });
        } else {
            throw new ResourceNotFoundException(SpexCategory.class, id);
        }
    }

    @RequiresAdmin
    public SpexCategoryDto saveLogo(final Long id, final byte[] logo, @Nullable final String contentType) {
        return repository
                .findById0(id)
                .map(permissionService::checkWritePermission)
                .map(category -> {
                    category.setLogo(logo);
                    category.setLogoContentType(hasText(contentType) ? contentType : FileUtil.detectMimeType(logo));
                    repository.save(category);
                    return SPEX_CATEGORY_MAPPER.toDto(category);
                })
                .orElseThrow(() -> new ResourceNotFoundException(SpexCategory.class, id));
    }

    @RequiresAdmin
    public SpexCategoryDto deleteLogo(final Long id) {
        return repository
                .findById0(id)
                .map(permissionService::checkWritePermission)
                .map(category -> {
                    category.setLogo(null);
                    category.setLogoContentType(null);
                    repository.save(category);
                    return SPEX_CATEGORY_MAPPER.toDto(category);
                })
                .orElseThrow(() -> new ResourceNotFoundException(SpexCategory.class, id));
    }

    @RequiresAdminOrEditorOrUser
    public Pair<byte[], String> getLogo(final Long id) {
        if (!doesSpexCategoryExist(id)) {
            throw new ResourceNotFoundException(SpexCategory.class, id);
        }
        return repository
                .findById0(id)
                .filter(category -> category.getLogo() != null && hasText(category.getLogoContentType()))
                .map(category -> Pair.of(category.getLogo(), category.getLogoContentType()))
                .orElseThrow(() -> new ResourceNoValueException(SpexCategory.class, SpexCategory_.LOGO, id));
    }

    private boolean doesSpexCategoryExist(final Long id) {
        return repository.findById0(id).isPresent();
    }
}
