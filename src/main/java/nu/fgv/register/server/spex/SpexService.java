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
import nu.fgv.register.server.security.policy.Permission;
import nu.fgv.register.server.security.policy.PermissionService;
import nu.fgv.register.server.spex.category.SpexCategoryDto;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.graphql.GraphqlUtil.ScrollRequest;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import nu.fgv.register.server.util.security.RequiresSpexReader;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static nu.fgv.register.server.spex.SpexMapper.SPEX_MAPPER;
import static nu.fgv.register.server.spex.SpexSpecification.NO_FILTER;
import static nu.fgv.register.server.spex.SpexSpecification.hasIds;
import static nu.fgv.register.server.spex.SpexSpecification.hasParentIds;
import static nu.fgv.register.server.spex.SpexSpecification.isNotRevival;
import static nu.fgv.register.server.spex.SpexSpecification.isRevival;
import static nu.fgv.register.server.spex.category.SpexCategoryMapper.SPEX_CATEGORY_MAPPER;
import static org.springframework.util.StringUtils.hasText;

/**
 * Reading spex and their revivals. Read only: Spexipedia keeps the spex and only the sync writes the copy here.
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
    private final PermissionService permissionService;

    /**
     * The spex the caller may read that match the filter, revivals included unless the filter leaves them out.
     *
     * @param filter a filter expression, or blank for all
     * @param scroll the window to read
     * @param sort   the order
     * @return the window with the total count
     */
    @RequiresSpexReader
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
     * @param id the spex id
     * @return the spex
     * @throws nu.fgv.register.server.util.error.ResourceNotFoundException when there is no such spex
     */
    @RequiresSpexReader
    public SpexDto findById(final UUID id) {
        return repository
                .findByUuid0(id)
                .map(SPEX_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(Spex.class, id));
    }

    /**
     * @param id the spex id
     * @return whether the spex exists
     * @throws org.springframework.security.access.AccessDeniedException when it exists but may not be read
     */
    @RequiresAdminOrEditorOrUser
    public boolean exists(final UUID id) {
        return repository
                .findByUuid0(id)
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
    public Iterable<Spex> streamByIds(final List<UUID> ids, final String filter, final Sort sort) {
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
    public Iterable<Spex> streamRevivalsByParentIds(final List<UUID> parentIds, final String filter, final Sort sort) {
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
     * For batch loading.
     *
     * @param ids spex ids
     * @return the originals of those that are revivals and readable, by revival id
     */
    @RequiresAdminOrEditorOrUser
    public Map<UUID, SpexDto> findParentsBySpex(final Collection<UUID> ids) {
        return findReadable(ids).stream()
                .filter(spex -> spex.getParent() != null)
                .collect(Collectors.toMap(Spex::getUuid, spex -> SPEX_MAPPER.toDto(spex.getParent())));
    }

    /**
     * For batch loading.
     *
     * @param ids spex ids
     * @return the revivals by year of those that are originals and readable, by original id
     */
    @RequiresSpexReader
    public Map<UUID, List<SpexDto>> findRevivalsByParents(final Collection<UUID> ids) {
        final List<UUID> parentIds = findReadable(ids).stream()
                .filter(parent -> !parent.isRevival())
                .map(Spex::getUuid)
                .toList();

        if (parentIds.isEmpty()) {
            return Map.of();
        }

        return repository
                .findAll(hasParentIds(parentIds), Sort.by(Spex_.YEAR), Permission.READ)
                .stream()
                .collect(Collectors.groupingBy(revival -> revival.getParent().getUuid(), Collectors.mapping(SPEX_MAPPER::toDto, Collectors.toList())));
    }

    /**
     * For batch loading.
     *
     * @param ids spex ids
     * @return the categories of those that have one and are readable, by spex id
     */
    @RequiresAdminOrEditorOrUser
    public Map<UUID, SpexCategoryDto> findCategoriesBySpex(final Collection<UUID> ids) {
        return findReadable(ids).stream()
                .filter(spex -> spex.getDetails().getCategory() != null)
                .collect(Collectors.toMap(Spex::getUuid, spex -> SPEX_CATEGORY_MAPPER.toDto(spex.getDetails().getCategory())));
    }

    private List<Spex> findReadable(final Collection<UUID> ids) {
        return repository.findByUuidIn(ids).stream()
                .filter(permissionService::hasReadPermission)
                .toList();
    }
}
