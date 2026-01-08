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
import nu.fgv.register.server.util.FileUtil;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.util.error.ResourceNoValueException;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.error.ResourcesNotFoundException;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import nu.fgv.register.server.util.search.Facet;
import nu.fgv.register.server.util.search.FacetValue;
import nu.fgv.register.server.util.search.PageWithFacets;
import nu.fgv.register.server.util.search.PageWithFacetsImpl;
import nu.fgv.register.server.util.search.WindowWithFacets;
import nu.fgv.register.server.util.search.WindowWithFacetsImpl;
import nu.fgv.register.server.util.security.RequiresAdmin;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.util.common.SearchException;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.util.Pair;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static nu.fgv.register.server.spexare.SpexareMapper.SPEXARE_MAPPER;
import static nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository.AGGREGATIONS;
import static nu.fgv.register.server.spexare.SpexareSpecification.NO_FILTER;
import static nu.fgv.register.server.spexare.SpexareSpecification.hasIds;
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

    @RequiresAdminOrEditorOrUser
    public WindowWithFacets<SpexareDto> search(final String query, final int offset, final int limit, final Sort sort) {
        final SearchResult<Spexare> searchResult = repository.search(query, offset, limit, sort);
        final List<Facet> facets = getFacets(searchResult);
        final boolean hasNext = searchResult.total().hitCount() > offset + limit;

        return new WindowWithFacetsImpl<>(SPEXARE_MAPPER.toDtos(searchResult.hits()), index -> ScrollPosition.offset(offset), hasNext, facets);
    }

    @RequiresAdminOrEditorOrUser
    public PageWithFacets<SpexareDto> search(final String query, final Pageable pageable) {
        final SearchResult<Spexare> searchResult = repository.search(query, pageable);
        final List<Facet> facets = getFacets(searchResult);

        return new PageWithFacetsImpl<>(SPEXARE_MAPPER.toDtos(searchResult.hits()), pageable, searchResult.total(), facets);
    }

    @RequiresAdminOrEditorOrUser
    public List<SpexareDto> findAll(final Sort sort) {
        return repository
                .findAll(sort, BasePermission.READ)
                .stream()
                .map(SPEXARE_MAPPER::toDto)
                .toList();
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
    public List<SpexareDto> findByIds(final List<Long> ids, final Sort sort) {
        return repository
                .findAll(hasIds(ids), sort, BasePermission.READ)
                .stream()
                .map(SPEXARE_MAPPER::toDto)
                .toList();
    }

    @RequiresAdmin
    public SpexareDto create(final SpexareCreateDto dto) {
        return Optional.of(SPEXARE_MAPPER.toModel(dto))
                .map(model -> {
                    final Spexare spexare = repository.save(model);
                    final ObjectIdentity oid = toObjectIdentity(Spexare.class, spexare.getId());

                    permissionService.grantPermission(oid, BasePermission.ADMINISTRATION, ROLE_ADMIN_SID);
                    permissionService.grantPermission(oid, BasePermission.READ, ROLE_ADMIN_SID);
                    if (spexare.getPublished()) {
                        permissionService.grantPermission(oid, BasePermission.READ, ROLE_EDITOR_SID, ROLE_USER_SID);
                        permissionService.grantPermission(oid, BasePermission.WRITE, ROLE_EDITOR_SID);
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
                        permissionService.grantPermission(oid, BasePermission.READ, ROLE_EDITOR_SID, ROLE_USER_SID);
                        permissionService.grantPermission(oid, BasePermission.WRITE, ROLE_EDITOR_SID);
                    } else {
                        permissionService.revokePermission(oid, BasePermission.READ, ROLE_EDITOR_SID, ROLE_USER_SID);
                        permissionService.revokePermission(oid, BasePermission.WRITE, ROLE_EDITOR_SID);
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
                    spexare.setImageContentType(hasText(contentType) ? contentType : FileUtil.detectMimeType(image));
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
    public SpexareDto findPartnerBySpexare(final Long id) {
        if (doesSpexareExist(id)) {
            return repository
                    .findById0(id)
                    .filter(spexare -> spexare.getPartner() != null)
                    .map(Spexare::getPartner)
                    .map(SPEXARE_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNoValueException(Spexare.class, Spexare_.PARTNER, id));
        } else {
            throw new ResourceNotFoundException(Spexare.class, id);
        }
    }

    @RequiresAdminOrEditorOrUser
    public void addPartner(final Long spexareId, final Long id) {
        if (doSpexareAndPartnerExist(spexareId, id)) {
            repository
                    .findById0(spexareId)
                    .map(permissionService::checkWritePermission)
                    .ifPresent(spexare -> repository
                            .findById0(id)
                            .ifPresent(partner -> {
                                final Spexare previousPartner = spexare.getPartner();

                                spexare.setPartner(partner);
                                partner.setPartner(spexare);
                                repository.save(spexare);
                                repository.save(partner);

                                if (spexare.getUser() != null) {
                                    final ObjectIdentity oid = toObjectIdentity(Spexare.class, partner.getId());

                                    permissionService.grantPermission(oid, BasePermission.WRITE, new PrincipalSid(spexare.getUser().getExternalId()));

                                    if (previousPartner != null) {
                                        final ObjectIdentity previousOid = toObjectIdentity(Spexare.class, previousPartner.getId());

                                        permissionService.revokePermission(previousOid, BasePermission.WRITE, new PrincipalSid(spexare.getUser().getExternalId()));
                                    }
                                }

                                if (partner.getUser() != null) {
                                    final ObjectIdentity oid = toObjectIdentity(Spexare.class, spexare.getId());

                                    permissionService.grantPermission(oid, BasePermission.WRITE, new PrincipalSid(partner.getUser().getExternalId()));
                                }

                                if (previousPartner != null && previousPartner.getUser() != null) {
                                    final ObjectIdentity oid = toObjectIdentity(Spexare.class, spexare.getId());

                                    permissionService.revokePermission(oid, BasePermission.WRITE, new PrincipalSid(previousPartner.getUser().getExternalId()));
                                }
                            }));
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

                        if (spexare.getUser() != null) {
                            final ObjectIdentity oid = toObjectIdentity(Spexare.class, partner.getId());

                            permissionService.revokePermission(oid, BasePermission.WRITE, new PrincipalSid(spexare.getUser().getExternalId()));
                        }

                        if (partner.getUser() != null) {
                            final ObjectIdentity oid = toObjectIdentity(Spexare.class, spexare.getId());

                            permissionService.revokePermission(oid, BasePermission.WRITE, new PrincipalSid(partner.getUser().getExternalId()));
                        }

                        partner.setPartner(null);
                        repository.save(partner);

                        spexare.setPartner(null);
                        repository.save(spexare);
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

    private List<Facet> getFacets(final SearchResult<Spexare> searchResult) {
        return AGGREGATIONS.stream()
                .filter(a -> {
                    try {
                        searchResult.aggregation(AggregationKey.of(a));
                        return true;
                    } catch (final SearchException _) {
                        return false;
                    }
                })
                .map(a -> {
                    final Map<Object, Long> values = searchResult.aggregation(AggregationKey.of(a));

                    return Facet.builder()
                            .name(a)
                            .values(values.entrySet().stream()
                                    .map(entry -> new FacetValue(String.valueOf(entry.getKey()), entry.getValue()))
                                    .toList())
                            .build();
                })
                .toList();
    }
}
