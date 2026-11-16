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
import nu.fgv.register.server.security.policy.Permission;
import nu.fgv.register.server.security.policy.PermissionService;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.graphql.GraphqlUtil.ScrollRequest;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static nu.fgv.register.server.spex.category.SpexCategoryMapper.SPEX_CATEGORY_MAPPER;
import static nu.fgv.register.server.spex.category.SpexCategorySpecification.NO_FILTER;
import static org.springframework.util.StringUtils.hasText;

/**
 * Spex categories and their logos. Only administrators change them.
 *
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

    /**
     * The spex categories the caller may read that match the filter.
     *
     * @param filter a filter expression, or blank for all
     * @param scroll the window to read
     * @param sort   the order
     * @return the window with the total count
     */
    @RequiresAdminOrEditorOrUser
    public CountedWindow<SpexCategoryDto> find(final String filter, final ScrollRequest scroll, final Sort sort) {
        return repository
                .findBy(hasText(filter) ?
                                SpecificationsBuilder.<SpexCategory>builder().build(FilterParser.parse(filter), SpexCategorySpecification::new) :
                                NO_FILTER,
                        Permission.READ, query -> {
                            final long total = query.count();

                            return CountedWindow.of(query
                                    .limit(scroll.limit())
                                    .sortBy(sort)
                                    .scroll(scroll.positionFor(total))
                                    .map(SPEX_CATEGORY_MAPPER::toDto), total);
                        });
    }

    /**
     * @param id the spex category id
     * @return the spex category
     * @throws nu.fgv.register.server.util.error.ResourceNotFoundException when there is no such spex category
     */
    @RequiresAdminOrEditorOrUser
    public SpexCategoryDto findById(final UUID id) {
        return repository
                .findByUuid0(id)
                .map(SPEX_CATEGORY_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(SpexCategory.class, id));
    }

}
