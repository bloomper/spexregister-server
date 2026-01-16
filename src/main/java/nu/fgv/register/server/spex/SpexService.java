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

package nu.fgv.register.server.spex;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.spex.category.SpexCategory;
import nu.fgv.register.server.spex.category.SpexCategoryDto;
import nu.fgv.register.server.spex.category.SpexCategoryRepository;
import nu.fgv.register.server.util.FileUtil;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.util.error.ResourceNoValueException;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.error.ResourcesNotFoundException;
import nu.fgv.register.server.util.error.SubresourceAlreadyExistsException;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import nu.fgv.register.server.util.security.RequiresAdmin;
import nu.fgv.register.server.util.security.RequiresAdminOrEditor;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static nu.fgv.register.server.spex.SpexMapper.SPEX_MAPPER;
import static nu.fgv.register.server.spex.SpexSpecification.NO_FILTER;
import static nu.fgv.register.server.spex.SpexSpecification.hasId;
import static nu.fgv.register.server.spex.SpexSpecification.hasIds;
import static nu.fgv.register.server.spex.SpexSpecification.hasParent;
import static nu.fgv.register.server.spex.SpexSpecification.hasYear;
import static nu.fgv.register.server.spex.SpexSpecification.isNotRevival;
import static nu.fgv.register.server.spex.SpexSpecification.isRevival;
import static nu.fgv.register.server.spex.category.SpexCategoryMapper.SPEX_CATEGORY_MAPPER;
import static nu.fgv.register.server.util.graphql.GraphqlUtil.emptyWindow;
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
public class SpexService {

    private final SpexRepository repository;
    private final SpexDetailsRepository detailsRepository;
    private final SpexCategoryRepository categoryRepository;
    private final PermissionService permissionService;

    @RequiresAdminOrEditorOrUser
    public List<SpexDto> findAll(final Sort sort) {
        return repository
                .findAll(isNotRevival(), sort, BasePermission.READ)
                .stream().map(SPEX_MAPPER::toDto)
                .toList();
    }

    @RequiresAdminOrEditorOrUser
    public Window<SpexDto> find(final String filter, final int limit, final Sort sort, final ScrollPosition scrollPosition) {
        return hasText(filter) ?
                repository
                        .findBy(SpecificationsBuilder.<Spex>builder().build(FilterParser.parse(filter), SpexSpecification::new), BasePermission.READ, query -> query
                                .limit(limit)
                                .sortBy(sort)
                                .scroll(scrollPosition))
                        .map(SPEX_MAPPER::toDto) :
                repository
                        .findBy(NO_FILTER, BasePermission.READ, query -> query
                                .limit(limit)
                                .sortBy(sort)
                                .scroll(scrollPosition))
                        .map(SPEX_MAPPER::toDto);
    }

    @RequiresAdminOrEditorOrUser
    public Page<SpexDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<Spex>builder().build(FilterParser.parse(filter), SpexSpecification::new), pageable, BasePermission.READ)
                        .map(SPEX_MAPPER::toDto) :
                repository
                        .findAll(pageable, BasePermission.READ)
                        .map(SPEX_MAPPER::toDto);
    }

    @RequiresAdminOrEditorOrUser
    public SpexDto findById(final Long id) {
        return repository
                .findById0(id)
                .map(SPEX_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(Spex.class, id));
    }

    @RequiresAdminOrEditorOrUser
    public boolean exists(final Long id) {
        return repository
                .findById0(id)
                .isPresent();
    }

    @RequiresAdminOrEditorOrUser
    public Iterable<Spex> streamByIds(final List<Long> ids, final String filter, final Sort sort) {
        return () -> {
            final Specification<Spex> spec;

            if (!ids.isEmpty()) {
                spec = hasIds(ids);
            } else if (hasText(filter)) {
                spec = SpecificationsBuilder.<Spex>builder()
                        .build(FilterParser.parse(filter), SpexSpecification::new).and(isNotRevival());
            } else {
                spec = isNotRevival();
            }

            return repository.streamAll(spec, sort, BasePermission.READ)
                    .iterator();
        };
    }

    @RequiresAdminOrEditorOrUser
    public Iterable<Spex> streamRevivalsByParentIds(final List<Long> parentIds, final String filter, final Sort sort) {
        return () -> {
            final Specification<Spex> spec;

            if (!parentIds.isEmpty()) {
                spec = hasIds(parentIds);
            } else if (hasText(filter)) {
                spec = SpecificationsBuilder.<Spex>builder()
                        .build(FilterParser.parse(filter), SpexSpecification::new).and(isRevival());
            } else {
                spec = isRevival();
            }

            return repository.streamAll(spec, sort, BasePermission.READ)
                    .iterator();
        };
    }

    @RequiresAdmin
    public SpexDto create(final SpexCreateDto dto) {
        return Optional.of(SPEX_MAPPER.toModel(dto))
                .map(model -> {
                    detailsRepository.save(model.getDetails());
                    final Spex spex = repository.save(model);
                    final ObjectIdentity oid = toObjectIdentity(Spex.class, spex.getId());

                    permissionService.grantPermission(oid, BasePermission.ADMINISTRATION, ROLE_ADMIN_SID);
                    permissionService.grantPermission(oid, BasePermission.READ, ROLE_ADMIN_SID, ROLE_EDITOR_SID, ROLE_USER_SID);
                    permissionService.grantPermission(oid, BasePermission.WRITE, ROLE_EDITOR_SID);

                    return SPEX_MAPPER.toDto(spex);
                })
                .orElseThrow(() -> new InternalErrorException("Could not create spex"));
    }

    @RequiresAdminOrEditor
    public SpexDto update(final SpexUpdateDto dto) {
        return partialUpdate(dto);
    }

    @RequiresAdminOrEditor
    public SpexDto partialUpdate(final SpexUpdateDto dto) {
        return repository
                .findById0(dto.id())
                .map(permissionService::checkWritePermission)
                .map(spex -> {
                    SPEX_MAPPER.toPartialModel(dto, spex);
                    return spex;
                })
                .map(spex -> {
                    detailsRepository.save(spex.getDetails());
                    touchSpexByDetails(spex.getDetails());
                    return repository.save(spex);
                })
                .map(SPEX_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(Spex.class, dto.id()));
    }

    @RequiresAdmin
    public void deleteById(final Long id) {
        if (doesSpexExist(id)) {
            repository
                    .findById0(id)
                    .map(permissionService::checkDeletePermission)
                    .ifPresent(spex -> {
                        repository.findAll(hasParent(spex)).forEach(revival -> {
                            permissionService.deleteAcl(toObjectIdentity(Spex.class, revival.getId()));
                            repository.delete(revival);
                        });
                        permissionService.deleteAcl(toObjectIdentity(Spex.class, id));
                        repository.delete(spex);
                        detailsRepository.delete(spex.getDetails());
                    });
        } else {
            throw new ResourceNotFoundException(Spex.class, id);
        }
    }

    @RequiresAdminOrEditor
    public SpexDto savePoster(final Long id, final byte[] poster, @Nullable final String contentType) {
        return repository
                .findById0(id)
                .map(permissionService::checkWritePermission)
                .map(spex -> {
                    spex.getDetails().setPoster(poster);
                    spex.getDetails().setPosterContentType(hasText(contentType) ? contentType : FileUtil.detectMimeType(poster));
                    detailsRepository.save(spex.getDetails());
                    touchSpexByDetails(spex.getDetails());
                    return SPEX_MAPPER.toDto(spex);
                })
                .orElseThrow(() -> new ResourceNotFoundException(Spex.class, id));
    }

    @RequiresAdminOrEditor
    public SpexDto deletePoster(final Long id) {
        return repository
                .findById0(id)
                .map(permissionService::checkWritePermission)
                .map(spex -> {
                    spex.getDetails().setPoster(null);
                    spex.getDetails().setPosterContentType(null);
                    detailsRepository.save(spex.getDetails());
                    touchSpexByDetails(spex.getDetails());
                    return SPEX_MAPPER.toDto(spex);
                })
                .orElseThrow(() -> new ResourceNotFoundException(Spex.class, id));
    }

    @RequiresAdminOrEditorOrUser
    public Pair<byte[], String> getPoster(final Long id) {
        if (!doesSpexExist(id)) {
            throw new ResourceNotFoundException(Spex.class, id);
        }
        return repository
                .findById0(id)
                .map(Spex::getDetails)
                .filter(details -> details.getPoster() != null && hasText(details.getPosterContentType()))
                .map(details -> Pair.of(details.getPoster(), details.getPosterContentType()))
                .orElseThrow(() -> new ResourceNoValueException(Spex.class, SpexDetails_.POSTER, id));
    }

    @RequiresAdminOrEditorOrUser
    public SpexDto findParentById(final Long id) {
        if (doesSpexExist(id)) {
            return repository
                    .findById0(id)
                    .filter(revival -> revival.getParent() != null)
                    .map(Spex::getParent)
                    .map(SPEX_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNoValueException(Spex.class, Spex_.PARENT, id));
        } else {
            throw new ResourceNotFoundException(Spex.class, id);
        }
    }

    @RequiresAdminOrEditorOrUser
    public SpexDto findRevivalById(final Long spexId, final Long id) {
        if (doSpexAndRevivalExist(spexId, id)) {
            return repository
                    .findById0(id)
                    .filter(revival -> revival.getParent() != null && revival.getParent().getId().equals(spexId))
                    .map(SPEX_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNotFoundException(Spex.class, id));
        } else {
            throw new ResourcesNotFoundException(new String[]{Spex.class.getSimpleName(), "Revival"}, spexId, id);
        }
    }

    @RequiresAdminOrEditorOrUser
    public List<SpexDto> findRevivalsByParent(final Long id) {
        if (doesSpexExist(id)) {
            return repository
                    .findById0(id)
                    .filter(parent -> !parent.isRevival())
                    .map(parent -> repository
                            .findAll(hasParent(parent), Pageable.unpaged(Sort.by(Spex_.YEAR)), BasePermission.READ)
                            .stream()
                            .map(SPEX_MAPPER::toDto)
                            .toList()
                    )
                    .orElseGet(Collections::emptyList);
        } else {
            throw new ResourceNotFoundException(Spex.class, id);
        }
    }

    @RequiresAdminOrEditorOrUser
    public Window<SpexDto> findRevivalsByParent(final Long id, final int limit, final Sort sort, final ScrollPosition scrollPosition) {
        return findRevivalsByParent(
                id,
                parent -> repository
                        .findBy(hasParent(parent), BasePermission.READ, query -> query
                                .limit(limit)
                                .sortBy(sort)
                                .scroll(scrollPosition)
                        )
                        .map(SPEX_MAPPER::toDto),
                () -> emptyWindow(scrollPosition)
        );
    }

    @RequiresAdminOrEditorOrUser
    public Page<SpexDto> findRevivalsByParent(final Long id, final Pageable pageable) {
        return findRevivalsByParent(
                id,
                parent -> repository
                        .findAll(hasParent(parent), pageable, BasePermission.READ)
                        .map(SPEX_MAPPER::toDto),
                Page::empty
        );
    }

    @RequiresAdminOrEditor
    public SpexDto addRevival(final Long id, final String year) {
        if (doesSpexExist(id)) {
            return repository
                    .findById0(id)
                    .map(permissionService::checkWritePermission)
                    .filter(parent -> !repository.exists(hasParent(parent).and(hasYear(year))))
                    .map(parent -> {
                        final Spex spex = new Spex();
                        spex.setDetails(parent.getDetails());
                        spex.setParent(parent);
                        spex.setYear(year);

                        final Spex revival = repository.save(spex);
                        touchSpexByDetails(parent.getDetails());

                        final ObjectIdentity oid = toObjectIdentity(Spex.class, revival.getId());

                        permissionService.grantPermission(oid, BasePermission.ADMINISTRATION, ROLE_ADMIN_SID);
                        permissionService.grantPermission(oid, BasePermission.READ, ROLE_EDITOR_SID, ROLE_USER_SID);
                        permissionService.grantPermission(oid, BasePermission.WRITE, ROLE_EDITOR_SID);

                        return revival;
                    })
                    .map(SPEX_MAPPER::toDto)
                    .orElseThrow(() -> new SubresourceAlreadyExistsException(new String[]{Spex.class.getSimpleName(), "Revival"}, Spex_.YEAR, year, id));
        } else {
            throw new ResourceNotFoundException(Spex.class, id);
        }
    }

    @RequiresAdminOrEditor
    public void deleteRevival(final Long spexId, final Long id) {
        if (doSpexAndRevivalExist(spexId, id)) {
            repository
                    .findById0(spexId)
                    .map(permissionService::checkWritePermission)
                    .filter(parent -> repository.exists(hasParent(parent).and(hasId(id))))
                    .flatMap(parent -> repository.findOne(hasParent(parent).and(hasId(id))))
                    .ifPresentOrElse(
                            revival -> {
                                final SpexDetails details = revival.getDetails();
                                permissionService.deleteAcl(toObjectIdentity(Spex.class, revival.getId()));
                                repository.deleteById(revival.getId());
                                touchSpexByDetails(details);
                            },
                            () -> {
                                throw new ResourceNotFoundException(Spex.class, id);
                            }
                    );
        } else {
            throw new ResourceNotFoundException(Spex.class, spexId);
        }
    }

    @RequiresAdminOrEditorOrUser
    public SpexCategoryDto findCategoryBySpex(final Long id) {
        if (doesSpexExist(id)) {
            return repository
                    .findById0(id)
                    .filter(spex -> spex.getDetails().getCategory() != null)
                    .map(spex -> spex.getDetails().getCategory())
                    .map(SPEX_CATEGORY_MAPPER::toDto)
                    .orElseThrow(() -> new ResourceNoValueException(Spex.class, SpexDetails_.CATEGORY, id));
        } else {
            throw new ResourceNotFoundException(Spex.class, id);
        }
    }

    @RequiresAdmin
    public void addCategory(final Long spexId, final Long id) {
        if (doSpexAndCategoryExist(spexId, id)) {
            repository
                    .findById0(spexId)
                    .map(permissionService::checkWritePermission)
                    .ifPresent(spex -> categoryRepository
                            .findById0(id)
                            .ifPresent(category -> {
                                spex.getDetails().setCategory(category);
                                detailsRepository.save(spex.getDetails());
                                touchSpexByDetails(spex.getDetails());
                            })
                    );
        } else {
            throw new ResourcesNotFoundException(List.of(Spex.class, SpexCategory.class), spexId, id);
        }
    }

    @RequiresAdmin
    public void removeCategory(final Long id) {
        if (doesSpexExist(id)) {
            repository
                    .findById0(id)
                    .map(permissionService::checkWritePermission)
                    .ifPresent(spex -> {
                        spex.getDetails().setCategory(null);
                        detailsRepository.save(spex.getDetails());
                        touchSpexByDetails(spex.getDetails());
                    });
        } else {
            throw new ResourceNotFoundException(Spex.class, id);
        }
    }

    private void touchSpexByDetails(final SpexDetails details) {
        repository.findByDetailsAndParentIsNull(details).ifPresent(spex -> {
            spex.setLastModifiedAt(Instant.now());
            repository.save(spex);
        });
    }

    private <T> T findRevivalsByParent(final Long id, final Function<Spex, T> retrievalFunction, final Supplier<T> emptyResultSupplier) {
        if (doesSpexExist(id)) {
            return repository.findById0(id)
                    .map(retrievalFunction)
                    .orElseGet(emptyResultSupplier);
        } else {
            throw new ResourceNotFoundException(Spex.class, id);
        }
    }

    private boolean doesSpexExist(final Long id) {
        return repository.findById0(id).isPresent();
    }

    private boolean doSpexAndRevivalExist(final Long spexId, final Long id) {
        return repository.findById0(spexId).isPresent() && repository.findById0(id).isPresent();
    }

    private boolean doSpexAndCategoryExist(final Long spexId, final Long categoryId) {
        return doesSpexExist(spexId) && categoryRepository.findById0(categoryId).isPresent();
    }
}
