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

package nu.fgv.register.server.spexare;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.util.error.ResourceNoValueException;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.error.ResourcesNotFoundException;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import nu.fgv.register.server.util.search.AggregationFilter;
import nu.fgv.register.server.util.search.Facet;
import nu.fgv.register.server.util.search.PageWithFacets;
import nu.fgv.register.server.util.search.PageWithFacetsImpl;
import nu.fgv.register.server.util.search.WindowWithFacets;
import nu.fgv.register.server.util.search.WindowWithFacetsImpl;
import nu.fgv.register.server.util.security.RequiresAdmin;
import nu.fgv.register.server.util.security.RequiresAdminOrEditor;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.hibernate.search.engine.search.query.SearchResult;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static nu.fgv.register.server.spexare.SpexareMapper.SPEXARE_MAPPER;
import static nu.fgv.register.server.spexare.SpexareSpecification.NO_FILTER;
import static nu.fgv.register.server.spexare.SpexareSpecification.hasIds;
import static nu.fgv.register.server.util.FileUtil.detectMimeType;
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
public class SpexareService {

    private final SpexareRepository repository;
    private final PermissionService permissionService;
    private final SpexareFacetService facetService;

    @RequiresAdminOrEditorOrUser
    public WindowWithFacets<SpexareDto> search(final String query, final List<AggregationFilter> aggregationFilters, final int offset, final int limit, final Sort sort) {
        final SearchResult<Spexare> searchResult = repository.search(query, aggregationFilters, offset, limit, sort);
        final List<Facet> facets = facetService.getFacets(searchResult);
        final boolean hasNext = searchResult.total().hitCount() > offset + limit;

        return new WindowWithFacetsImpl<>(SPEXARE_MAPPER.toDtos(searchResult.hits()), _ -> ScrollPosition.offset(offset), hasNext, facets);
    }

    @RequiresAdminOrEditorOrUser
    public PageWithFacets<SpexareDto> search(final String query, final List<AggregationFilter> aggregationFilters, final Pageable pageable) {
        final SearchResult<Spexare> searchResult = repository.search(query, aggregationFilters, pageable);
        final List<Facet> facets = facetService.getFacets(searchResult);

        return new PageWithFacetsImpl<>(SPEXARE_MAPPER.toDtos(searchResult.hits()), pageable, searchResult.total(), facets);
    }

    @RequiresAdminOrEditorOrUser
    public Window<SpexareDto> find(final String filter, final int limit, final Sort sort, final ScrollPosition scrollPosition) {
        return hasText(filter) ?
                repository
                        .findBy(SpecificationsBuilder.<Spexare>builder().build(FilterParser.parse(filter), SpexareSpecification::new), BasePermission.READ, query -> query
                                .limit(limit)
                                .sortBy(sort)
                                .scroll(scrollPosition))
                        .map(SPEXARE_MAPPER::toDto) :
                repository
                        .findBy(NO_FILTER, BasePermission.READ, query -> query
                                .limit(limit)
                                .sortBy(sort)
                                .scroll(scrollPosition))
                        .map(SPEXARE_MAPPER::toDto);
    }

    @RequiresAdminOrEditorOrUser
    public Page<SpexareDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<Spexare>builder().build(FilterParser.parse(filter), SpexareSpecification::new), pageable, BasePermission.READ)
                        .map(SPEXARE_MAPPER::toDto) :
                repository
                        .findAll(pageable, BasePermission.READ)
                        .map(SPEXARE_MAPPER::toDto);
    }

    @RequiresAdminOrEditorOrUser
    public SpexareDto findById(final Long id) {
        return repository
                .findById0(id)
                .map(SPEXARE_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(Spexare.class, id));
    }

    @RequiresAdminOrEditorOrUser
    public boolean exists(final Long id) {
        return repository
                .findById0(id)
                .isPresent();
    }

    @RequiresAdminOrEditorOrUser
    public Iterable<Spexare> streamByIds(final List<Long> ids, final String filter, final Sort sort) {
        return () -> {
            final Specification<Spexare> spec;

            if (!ids.isEmpty()) {
                spec = hasIds(ids);
            } else if (hasText(filter)) {
                spec = SpecificationsBuilder.<Spexare>builder()
                        .build(FilterParser.parse(filter), SpexareSpecification::new);
            } else {
                spec = null;
            }

            return repository.streamAll(spec, sort, BasePermission.READ)
                    .iterator();
        };
    }

    @RequiresAdminOrEditorOrUser
    public List<Spexare> getAllowedByIds(final List<Long> ids) {
        final List<Spexare> result = new ArrayList<>();

        for (final Spexare s : streamByIds(ids, "", Sort.unsorted())) {
            result.add(s);
        }
        return result;
    }

    @RequiresAdminOrEditorOrUser
    public List<Long> getAllowedIdsByIds(final List<Long> ids) {
        final List<Long> result = new ArrayList<>();

        for (final Spexare s : streamByIds(ids, "", Sort.unsorted())) {
            result.add(s.getId());
        }
        return result;
    }

    @RequiresAdminOrEditor
    public SpexareDto create(final SpexareCreateDto dto) {
        return Optional.of(SPEXARE_MAPPER.toModel(dto))
                .map(model -> {
                    final Spexare spexare = repository.save(model);
                    final ObjectIdentity oid = toObjectIdentity(Spexare.class, spexare.getId());

                    permissionService.grantPermission(oid, BasePermission.ADMINISTRATION, ROLE_ADMIN_SID);
                    permissionService.grantPermission(oid, BasePermission.READ, ROLE_ADMIN_SID, ROLE_EDITOR_SID);
                    permissionService.grantPermission(oid, BasePermission.WRITE, ROLE_EDITOR_SID);
                    if (spexare.getPublished()) {
                        permissionService.grantPermission(oid, BasePermission.READ, ROLE_USER_SID);
                    }

                    return SPEXARE_MAPPER.toDto(model);
                })
                .orElseThrow(() -> new InternalErrorException("Could not create spexare"));
    }

    @RequiresAdminOrEditorOrUser
    public SpexareDto update(final SpexareUpdateDto dto) {
        return partialUpdate(dto);
    }

    @RequiresAdminOrEditorOrUser
    public SpexareDto partialUpdate(final SpexareUpdateDto dto) {
        return repository
                .findById0(dto.id())
                .map(permissionService::checkWritePermission)
                .map(spexare -> {
                    SPEXARE_MAPPER.toPartialModel(dto, spexare);
                    return spexare;
                })
                .map(repository::save)
                .map(spexare -> {
                    final ObjectIdentity oid = toObjectIdentity(Spexare.class, spexare.getId());

                    if (spexare.getPublished()) {
                        permissionService.grantPermission(oid, BasePermission.READ, ROLE_USER_SID);
                    } else {
                        permissionService.revokePermission(oid, BasePermission.READ, ROLE_USER_SID);
                    }

                    return spexare;
                })
                .map(SPEXARE_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(Spexare.class, dto.id()));
    }

    @RequiresAdmin
    public void deleteById(final Long id) {
        if (doesSpexareExist(id)) {
            repository.findById0(id)
                    .map(permissionService::checkDeletePermission)
                    .ifPresent(spexare -> {
                        permissionService.deleteAcl(toObjectIdentity(Spexare.class, id));
                        repository.delete(spexare);
                    });
        } else {
            throw new ResourceNotFoundException(Spexare.class, id);
        }
    }

    @RequiresAdminOrEditorOrUser
    public SpexareDto saveImage(final Long id, final byte[] image, @Nullable final String contentType) {
        return repository
                .findById0(id)
                .map(permissionService::checkWritePermission)
                .map(spexare -> {
                    spexare.setImage(image);
                    spexare.setImageContentType(hasText(contentType) ? contentType : detectMimeType(image));
                    repository.save(spexare);
                    return SPEXARE_MAPPER.toDto(spexare);
                })
                .orElseThrow(() -> new ResourceNotFoundException(Spexare.class, id));
    }

    @RequiresAdminOrEditorOrUser
    public SpexareDto deleteImage(final Long id) {
        return repository
                .findById0(id)
                .map(permissionService::checkWritePermission)
                .map(spexare -> {
                    spexare.setImage(null);
                    spexare.setImageContentType(null);
                    repository.save(spexare);
                    return SPEXARE_MAPPER.toDto(spexare);
                })
                .orElseThrow(() -> new ResourceNotFoundException(Spexare.class, id));
    }

    @RequiresAdminOrEditorOrUser
    public Pair<byte[], String> getImage(final Long id) {
        if (!doesSpexareExist(id)) {
            throw new ResourceNotFoundException(Spexare.class, id);
        }
        return repository
                .findById0(id)
                .filter(spexare -> spexare.getImage() != null && hasText(spexare.getImageContentType()))
                .map(spexare -> Pair.of(spexare.getImage(), spexare.getImageContentType()))
                .orElseThrow(() -> new ResourceNoValueException(Spexare.class, Spexare_.IMAGE, id));
    }

    @RequiresAdminOrEditorOrUser
    public Optional<SpexareDto> findPartnerBySpexare(final Long id) {
        if (doesSpexareExist(id)) {
            return repository
                    .findById0(id)
                    .filter(spexare -> spexare.getPartner() != null)
                    .map(Spexare::getPartner)
                    .map(SPEXARE_MAPPER::toDto);
        } else {
            throw new ResourceNotFoundException(Spexare.class, id);
        }
    }

    @RequiresAdminOrEditorOrUser
    public void addPartner(final Long spexareId, final Long id) {
        if (doSpexareAndPartnerExist(spexareId, id)) {
            final Spexare spexare = repository.findById(spexareId)
                    .map(permissionService::checkWritePermission)
                    .orElseThrow(() -> new ResourceNotFoundException(Spexare.class, spexareId));

            final Spexare partner = repository.findById(id)
                    .map(permissionService::checkWritePermission)
                    .orElseThrow(() -> new ResourceNotFoundException(Spexare.class, id));

            if (spexare.getPartner() != null) {
                removePartner(spexare.getId());
            }
            if (partner.getPartner() != null) {
                removePartner(partner.getId());
            }

            spexare.setPartner(partner);
            partner.setPartner(spexare);

            repository.saveAndFlush(spexare);
            repository.saveAndFlush(partner);

            if (spexare.getUser() != null) {
                final ObjectIdentity oid = toObjectIdentity(Spexare.class, partner.getId());
                permissionService.grantPermission(oid, BasePermission.WRITE, new PrincipalSid(spexare.getUser().getExternalId()));
            }

            if (partner.getUser() != null) {
                final ObjectIdentity oid = toObjectIdentity(Spexare.class, spexare.getId());
                permissionService.grantPermission(oid, BasePermission.WRITE, new PrincipalSid(partner.getUser().getExternalId()));
            }
        } else {
            throw new ResourcesNotFoundException(new String[]{Spexare.class.getSimpleName(), "Partner"}, spexareId, id);
        }
    }

    @RequiresAdminOrEditorOrUser
    public void removePartner(final Long id) {
        if (doesSpexareExist(id)) {
            repository
                    .findById0(id)
                    .map(permissionService::checkWritePermission)
                    .filter(spexare -> spexare.getPartner() != null)
                    .ifPresent(spexare -> {
                        final Spexare partner = spexare.getPartner();

                        permissionService.checkWritePermission(partner);

                        if (spexare.getUser() != null) {
                            final ObjectIdentity oid = toObjectIdentity(Spexare.class, partner.getId());

                            permissionService.revokePermission(oid, BasePermission.WRITE, new PrincipalSid(spexare.getUser().getExternalId()));
                        }

                        if (partner.getUser() != null) {
                            final ObjectIdentity oid = toObjectIdentity(Spexare.class, spexare.getId());

                            permissionService.revokePermission(oid, BasePermission.WRITE, new PrincipalSid(partner.getUser().getExternalId()));
                        }

                        partner.setPartner(null);
                        spexare.setPartner(null);

                        repository.saveAndFlush(partner);
                        repository.saveAndFlush(spexare);
                    });
        } else {
            throw new ResourceNotFoundException(Spexare.class, id);
        }
    }

    private boolean doesSpexareExist(final Long id) {
        return repository.findById0(id).isPresent();
    }

    private boolean doSpexareAndPartnerExist(final Long spexareId, final Long partnerId) {
        return doesSpexareExist(spexareId) && doesSpexareExist(partnerId);
    }

}
