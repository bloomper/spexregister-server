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
import nu.fgv.register.server.permission.PermissionGrantService;
import nu.fgv.register.server.security.policy.PermissionService;
import nu.fgv.register.server.image.Image;
import nu.fgv.register.server.spex.category.SpexCategory;
import nu.fgv.register.server.spex.category.SpexCategoryDto;
import nu.fgv.register.server.spex.category.SpexCategoryRepository;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.util.error.ResourceNoValueException;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.error.ResourcesNotFoundException;
import nu.fgv.register.server.util.error.SubresourceAlreadyExistsException;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.graphql.GraphqlUtil;
import nu.fgv.register.server.util.graphql.GraphqlUtil.ScrollRequest;
import nu.fgv.register.server.util.security.RequiresAdmin;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUserOrRevival;
import nu.fgv.register.server.util.security.RequiresReferenceDataReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import nu.fgv.register.server.security.policy.Permission;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static nu.fgv.register.server.spex.SpexMapper.SPEX_MAPPER;
import static nu.fgv.register.server.spex.SpexSpecification.NO_FILTER;
import static nu.fgv.register.server.spex.SpexSpecification.hasId;
import static nu.fgv.register.server.spex.SpexSpecification.hasIds;
import static nu.fgv.register.server.spex.SpexSpecification.hasParent;
import static nu.fgv.register.server.spex.SpexSpecification.hasParentIds;
import static nu.fgv.register.server.spex.SpexSpecification.hasYear;
import static nu.fgv.register.server.spex.SpexSpecification.isNotRevival;
import static nu.fgv.register.server.spex.SpexSpecification.isRevival;
import static nu.fgv.register.server.spex.category.SpexCategoryMapper.SPEX_CATEGORY_MAPPER;
import static nu.fgv.register.server.util.FileUtil.requireSupportedImageMimeType;
import static org.springframework.util.StringUtils.hasText;

/**
 * Spex and their revivals. Changes to the shared {@link SpexDetails} touch the original spex's last modified time, so
 * that lists sorted by it see the change.
 *
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
    private final PermissionGrantService permissionGrantService;

    /**
     * @param sort the order
     * @return the original spex the caller may read, without revivals
     */
    @RequiresAdminOrEditorOrUser
    public List<SpexDto> findAll(final Sort sort) {
        return repository
                .findAll(isNotRevival(), sort, Permission.READ)
                .stream().map(SPEX_MAPPER::toDto)
                .toList();
    }

    /**
     * The spex the caller may read that match the filter, revivals included unless the filter leaves them out.
     *
     * @param filter a filter expression, or blank for all
     * @param scroll the window to read
     * @param sort   the order
     * @return the window with the total count
     */
    @RequiresReferenceDataReader
    public CountedWindow<SpexDto> find(final String filter, final ScrollRequest scroll, final Sort sort) {
        return repository
                .findBy(hasText(filter) ?
                                SpecificationsBuilder.<Spex>builder().build(FilterParser.parse(filter), SpexSpecification::new) :
                                NO_FILTER,
                        Permission.READ, query -> {
                            final long total = query.count();

                            return CountedWindow.of(query
                                    .limit(scroll.limit())
                                    .sortBy(sort)
                                    .scroll(scroll.positionFor(total))
                                    .map(SPEX_MAPPER::toDto), total);
                        });
    }

    /**
     * The spex the caller may read that match the filter, for the REST API.
     *
     * @param filter   a filter expression, or blank for all
     * @param pageable the page to read
     * @return the page
     */
    @RequiresAdminOrEditorOrUser
    public Page<SpexDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<Spex>builder().build(FilterParser.parse(filter), SpexSpecification::new), pageable, Permission.READ)
                        .map(SPEX_MAPPER::toDto) :
                repository
                        .findAll(pageable, Permission.READ)
                        .map(SPEX_MAPPER::toDto);
    }

    /**
     * @param id the spex id
     * @return the spex
     * @throws nu.fgv.register.server.util.error.ResourceNotFoundException when there is no such spex
     */
    @RequiresReferenceDataReader
    public SpexDto findById(final Long id) {
        return repository
                .findById0(id)
                .map(SPEX_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(Spex.class, id));
    }

    /**
     * @param id the spex id
     * @return whether the spex exists
     * @throws org.springframework.security.access.AccessDeniedException when it exists but may not be read
     */
    @RequiresAdminOrEditorOrUser
    public boolean exists(final Long id) {
        return repository
                .findById0(id)
                .isPresent();
    }

    /**
     * The spex to export, streamed when iterated, so it must be consumed inside a transaction.
     *
     * @param ids    the spex to export, or empty to use the filter
     * @param filter a filter expression on the original spex when no ids are given, or blank for all
     * @param sort   the order
     * @return the spex the caller may read
     */
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

            return repository.streamAll(spec, sort, Permission.READ)
                    .iterator();
        };
    }

    /**
     * The revivals to export alongside {@link #streamByIds}, streamed when iterated, so it must be consumed inside a
     * transaction.
     *
     * @param parentIds the spex to export, or empty to use the filter
     * @param filter    a filter expression on the revivals when no ids are given, or blank for all
     * @param sort      the order
     * @return the spex the caller may read
     */
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

            return repository.streamAll(spec, sort, Permission.READ)
                    .iterator();
        };
    }

    /**
     * @param dto the new spex
     * @return the created spex
     */
    @RequiresAdmin
    public SpexDto create(final SpexCreateDto dto) {
        return Optional.of(SPEX_MAPPER.toModel(dto))
                .map(model -> {
                    detailsRepository.save(model.getDetails());
                    final Spex spex = repository.save(model);

                    return SPEX_MAPPER.toDto(spex);
                })
                .orElseThrow(() -> new InternalErrorException("Could not create spex"));
    }

    /**
     * The same as {@link #partialUpdate}.
     *
     * @param dto the spex id and new values
     * @return the updated spex
     */
    @RequiresAdminOrEditorOrUser
    public SpexDto update(final SpexUpdateDto dto) {
        return partialUpdate(dto);
    }

    /**
     * Changes the year and the shared title, which changes the title of the parent and all its revivals too.
     *
     * @param dto the spex id and new values
     * @return the updated spex
     */
    @RequiresAdminOrEditorOrUser
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

    /**
     * Removes the spex with its revivals, their permission grants and the shared details.
     *
     * @param id the spex id
     */
    @RequiresAdmin
    public void deleteById(final Long id) {
        if (doesSpexExist(id)) {
            repository
                    .findById0(id)
                    .map(permissionService::checkDeletePermission)
                    .ifPresent(spex -> {
                        repository.findAll(hasParent(spex)).forEach(revival -> {
                            permissionGrantService.deleteBySpex(revival);
                            repository.delete(revival);
                        });
                        permissionGrantService.deleteBySpex(spex);
                        repository.delete(spex);
                        detailsRepository.delete(spex.getDetails());
                    });
        } else {
            throw new ResourceNotFoundException(Spex.class, id);
        }
    }

    /**
     * Replaces the poster of the spex and all that share its details.
     *
     * @param id     the spex id
     * @param poster a PNG, JPEG, GIF or SVG image
     * @return the spex
     */
    @RequiresAdminOrEditorOrUser
    public SpexDto savePoster(final Long id, final byte[] poster) {
        return repository
                .findById0(id)
                .map(permissionService::checkWritePermission)
                .map(spex -> {
                    spex.getDetails().setPoster(Image.of(poster, requireSupportedImageMimeType(poster)));
                    detailsRepository.save(spex.getDetails());
                    touchSpexByDetails(spex.getDetails());
                    return SPEX_MAPPER.toDto(spex);
                })
                .orElseThrow(() -> new ResourceNotFoundException(Spex.class, id));
    }

    /**
     * Removes the poster of the spex and all that share its details.
     *
     * @param id the spex id
     * @return the spex
     */
    @RequiresAdminOrEditorOrUser
    public SpexDto deletePoster(final Long id) {
        return repository
                .findById0(id)
                .map(permissionService::checkWritePermission)
                .map(spex -> {
                    spex.getDetails().setPoster(null);
                    detailsRepository.save(spex.getDetails());
                    touchSpexByDetails(spex.getDetails());
                    return SPEX_MAPPER.toDto(spex);
                })
                .orElseThrow(() -> new ResourceNotFoundException(Spex.class, id));
    }

    /**
     * @param id the spex id
     * @return the poster's bytes and content type
     * @throws nu.fgv.register.server.util.error.ResourceNoValueException when the spex has no poster
     */
    @RequiresReferenceDataReader
    public Pair<byte[], String> getPoster(final Long id) {
        if (!doesSpexExist(id)) {
            throw new ResourceNotFoundException(Spex.class, id);
        }
        return repository
                .findById0(id)
                .map(Spex::getDetails)
                .map(SpexDetails::getPoster)
                .map(poster -> Pair.of(poster.getData(), poster.getContentType()))
                .orElseThrow(() -> new ResourceNoValueException(Spex.class, SpexDetails_.POSTER, id));
    }

    /**
     * @param id the id of a revival
     * @return the original it revives
     * @throws nu.fgv.register.server.util.error.ResourceNoValueException when the spex is not a revival
     */
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

    /**
     * @param spexId the original spex id
     * @param id     the revival id
     * @return the revival
     * @throws nu.fgv.register.server.util.error.ResourceNotFoundException when it is not a revival of that spex
     */
    @RequiresReferenceDataReader
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

    /**
     * @param id the original spex id
     * @return the revivals the caller may read by year, or none when the spex is itself a revival
     */
    @RequiresAdminOrEditorOrUser
    public List<SpexDto> findRevivalsByParent(final Long id) {
        if (doesSpexExist(id)) {
            return repository
                    .findById0(id)
                    .filter(parent -> !parent.isRevival())
                    .map(parent -> repository
                            .findAll(hasParent(parent), Pageable.unpaged(Sort.by(Spex_.YEAR)), Permission.READ)
                            .stream()
                            .map(SPEX_MAPPER::toDto)
                            .toList()
                    )
                    .orElseGet(Collections::emptyList);
        } else {
            throw new ResourceNotFoundException(Spex.class, id);
        }
    }

    @RequiresReferenceDataReader
    public CountedWindow<SpexDto> findRevivalsByParent(final Long id, final ScrollRequest scroll, final Sort sort) {
        return findRevivalsByParent(
                id,
                parent -> repository
                        .findBy(hasParent(parent), Permission.READ, query -> {
                            final long total = query.count();

                            return CountedWindow.of(query
                                    .limit(scroll.limit())
                                    .sortBy(sort)
                                    .scroll(scroll.positionFor(total))
                                    .map(SPEX_MAPPER::toDto), total);
                        }),
                GraphqlUtil::emptyWindow
        );
    }

    @RequiresAdminOrEditorOrUser
    public Page<SpexDto> findRevivalsByParent(final Long id, final Pageable pageable) {
        return findRevivalsByParent(
                id,
                parent -> repository
                        .findAll(hasParent(parent), pageable, Permission.READ)
                        .map(SPEX_MAPPER::toDto),
                Page::empty
        );
    }

    /**
     * Adds a revival in the year, sharing the original's details.
     *
     * @param id   the original spex id
     * @param year the revival's year
     * @return the revival
     * @throws nu.fgv.register.server.util.error.SubresourceAlreadyExistsException when the spex already has a revival
     *     that year
     */
    @RequiresAdminOrEditorOrUserOrRevival
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

                        return revival;
                    })
                    .map(SPEX_MAPPER::toDto)
                    .orElseThrow(() -> new SubresourceAlreadyExistsException(new String[]{Spex.class.getSimpleName(), "Revival"}, Spex_.YEAR, year, id));
        } else {
            throw new ResourceNotFoundException(Spex.class, id);
        }
    }

    /**
     * Removes a revival and its permission grants. The shared details stay with the original.
     *
     * @param spexId the original spex id
     * @param id     the revival id
     */
    @RequiresAdminOrEditorOrUser
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
                                permissionGrantService.deleteBySpex(revival);
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

    /**
     * For batch loading.
     *
     * @param ids spex ids
     * @return the originals of those that are revivals and readable, by revival id
     */
    @RequiresAdminOrEditorOrUser
    public Map<Long, SpexDto> findParentsBySpex(final Collection<Long> ids) {
        return findReadable(ids).stream()
                .filter(spex -> spex.getParent() != null)
                .collect(Collectors.toMap(Spex::getId, spex -> SPEX_MAPPER.toDto(spex.getParent())));
    }

    /**
     * For batch loading.
     *
     * @param ids spex ids
     * @return the revivals by year of those that are originals and readable, by original id
     */
    @RequiresReferenceDataReader
    public Map<Long, List<SpexDto>> findRevivalsByParents(final Collection<Long> ids) {
        final List<Long> parentIds = findReadable(ids).stream()
                .filter(parent -> !parent.isRevival())
                .map(Spex::getId)
                .toList();

        if (parentIds.isEmpty()) {
            return Map.of();
        }

        return repository
                .findAll(hasParentIds(parentIds), Sort.by(Spex_.YEAR), Permission.READ)
                .stream()
                .collect(Collectors.groupingBy(revival -> revival.getParent().getId(), Collectors.mapping(SPEX_MAPPER::toDto, Collectors.toList())));
    }

    /**
     * For batch loading.
     *
     * @param ids spex ids
     * @return the categories of those that have one and are readable, by spex id
     */
    @RequiresAdminOrEditorOrUser
    public Map<Long, SpexCategoryDto> findCategoriesBySpex(final Collection<Long> ids) {
        return findReadable(ids).stream()
                .filter(spex -> spex.getDetails().getCategory() != null)
                .collect(Collectors.toMap(Spex::getId, spex -> SPEX_CATEGORY_MAPPER.toDto(spex.getDetails().getCategory())));
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

    /**
     * Sets the category of the spex and all that share its details.
     *
     * @param spexId the spex id
     * @param id     the category id
     */
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

    /**
     * Clears the category of the spex and all that share its details.
     *
     * @param id the spex id
     */
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

    private List<Spex> findReadable(final Collection<Long> ids) {
        return repository.findAllById(ids).stream()
                .filter(permissionService::hasReadPermission)
                .toList();
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
