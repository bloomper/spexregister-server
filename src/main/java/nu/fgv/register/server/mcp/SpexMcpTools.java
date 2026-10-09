/*
 * Copyright 2026 the original author or authors.
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

package nu.fgv.register.server.mcp;

import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.mcp.McpToolSupport.PageResult;
import nu.fgv.register.server.spex.SpexCreateDto;
import nu.fgv.register.server.spex.SpexDto;
import nu.fgv.register.server.spex.SpexService;
import nu.fgv.register.server.spex.SpexUpdateDto;
import nu.fgv.register.server.spex.Spex_;
import nu.fgv.register.server.spex.category.SpexCategoryCreateDto;
import nu.fgv.register.server.spex.category.SpexCategoryDto;
import nu.fgv.register.server.spex.category.SpexCategoryService;
import nu.fgv.register.server.spex.category.SpexCategoryUpdateDto;
import nu.fgv.register.server.spex.category.SpexCategory_;
import nu.fgv.register.server.util.error.ResourceNoValueException;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

import static nu.fgv.register.server.mcp.McpToolSupport.FILTER_DESCRIPTION;
import static nu.fgv.register.server.mcp.McpToolSupport.PAGE_DESCRIPTION;
import static nu.fgv.register.server.mcp.McpToolSupport.SIZE_DESCRIPTION;
import static nu.fgv.register.server.mcp.McpToolSupport.filter;
import static nu.fgv.register.server.mcp.McpToolSupport.orElse;
import static nu.fgv.register.server.mcp.McpToolSupport.pageable;
import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
@RequiredArgsConstructor
public class SpexMcpTools {

    private static final String NOT_REVIVAL_FILTER = "parent:NULL";
    private static final Pattern YEAR = Pattern.compile("^(19|20|21)\\d{2}$");

    private final SpexService spexService;
    private final SpexCategoryService categoryService;
    private final McpToolSupport support;

    @McpTool(name = "list_spex", description = "List spex (original productions, excluding revivals unless includeRevivals is true). Filterable fields: year, details.title, details.category.id, details.category.name.",
            annotations = @McpAnnotations(readOnlyHint = true, openWorldHint = false))
    public PageResult<McpSpex> listSpex(@McpToolParam(required = false, description = FILTER_DESCRIPTION) final @Nullable String filter,
                                        @McpToolParam(required = false, description = "Also include revivals, defaults to false") final @Nullable Boolean includeRevivals,
                                        @McpToolParam(required = false, description = PAGE_DESCRIPTION) final @Nullable Integer page,
                                        @McpToolParam(required = false, description = SIZE_DESCRIPTION) final @Nullable Integer size) {
        final String userFilter = filter(filter);
        final String effectiveFilter;

        if (Boolean.TRUE.equals(includeRevivals)) {
            effectiveFilter = userFilter;
        } else {
            effectiveFilter = hasText(userFilter) ? "%s AND ( %s )".formatted(NOT_REVIVAL_FILTER, userFilter) : NOT_REVIVAL_FILTER;
        }

        return PageResult.of(spexService.find(effectiveFilter, pageable(page, size, Sort.by(Spex_.YEAR))), McpSpex::of);
    }

    @McpTool(name = "get_spex", description = "Get a spex, including its category and revivals",
            annotations = @McpAnnotations(readOnlyHint = true, openWorldHint = false))
    public McpSpexDetails getSpex(@McpToolParam(description = "Spex id") final Long id) {
        final SpexDto spex = spexService.findById(id);
        final List<McpSpex> revivals = Boolean.TRUE.equals(spex.getRevival()) ?
                List.of() :
                spexService.findRevivalsByParent(id).stream().map(McpSpex::of).toList();

        return new McpSpexDetails(McpSpex.of(spex), categoryOf(id), revivals);
    }

    @McpTool(name = "create_spex", description = "Create a spex, optionally in a spex category. Requires the ADMIN role.",
            annotations = @McpAnnotations(destructiveHint = false, openWorldHint = false))
    public McpSpexDetails createSpex(@McpToolParam(description = "Year of the premiere, four digits") final String year,
                                     @McpToolParam(description = "Title") final String title,
                                     @McpToolParam(required = false, description = "Id of the spex category") final @Nullable Long categoryId) {
        support.audit("create_spex");
        final SpexDto spex = spexService.create(support.validate(SpexCreateDto.builder().year(year).title(title).build()));

        if (categoryId != null) {
            spexService.addCategory(spex.getId(), categoryId);
        }

        return getSpex(spex.getId());
    }

    @McpTool(name = "update_spex", description = "Update a spex; omitted fields are left unchanged. The title is shared with the spex's revivals. Requires the ADMIN or EDITOR role.",
            annotations = @McpAnnotations(idempotentHint = true, openWorldHint = false))
    public McpSpex updateSpex(@McpToolParam(description = "Spex id") final Long id,
                              @McpToolParam(required = false, description = "Year, four digits") final @Nullable String year,
                              @McpToolParam(required = false, description = "Title") final @Nullable String title) {
        support.audit("update_spex");
        final SpexDto current = spexService.findById(id);

        return McpSpex.of(spexService.update(support.validate(new SpexUpdateDto(
                id,
                orElse(year, current.getYear()),
                orElse(title, current.getTitle())
        ))));
    }

    @McpTool(name = "set_spex_category", description = "Set or clear the category of a spex. Requires the ADMIN role.",
            annotations = @McpAnnotations(idempotentHint = true, openWorldHint = false))
    public McpSpexDetails setSpexCategory(@McpToolParam(description = "Spex id") final Long id,
                                          @McpToolParam(required = false, description = "Spex category id, omit to remove the category") final @Nullable Long categoryId) {
        support.audit("set_spex_category");

        if (categoryId != null) {
            spexService.addCategory(id, categoryId);
        } else {
            spexService.removeCategory(id);
        }

        return getSpex(id);
    }

    @McpTool(name = "delete_spex", description = "Permanently delete a single spex together with all its revivals. Requires the ADMIN role.",
            annotations = @McpAnnotations(openWorldHint = false))
    public String deleteSpex(@McpToolParam(description = "Spex id") final Long id) {
        support.audit("delete_spex");
        spexService.deleteById(id);

        return "Deleted spex %d".formatted(id);
    }

    @McpTool(name = "add_spex_revival", description = "Add a revival of a spex in a given year. Requires the ADMIN or EDITOR role.",
            annotations = @McpAnnotations(destructiveHint = false, openWorldHint = false))
    public McpSpex addSpexRevival(@McpToolParam(description = "Id of the original spex") final Long spexId,
                                  @McpToolParam(description = "Year of the revival, four digits") final String year) {
        support.audit("add_spex_revival");
        if (!YEAR.matcher(year).matches()) {
            throw new IllegalArgumentException("Year must be four digits between 1900 and 2199, was '%s'".formatted(year));
        }


        return McpSpex.of(spexService.addRevival(spexId, year));
    }

    @McpTool(name = "delete_spex_revival", description = "Permanently delete a single revival of a spex. Requires the ADMIN or EDITOR role.",
            annotations = @McpAnnotations(openWorldHint = false))
    public String deleteSpexRevival(@McpToolParam(description = "Id of the original spex") final Long spexId,
                                    @McpToolParam(description = "Id of the revival") final Long revivalId) {
        support.audit("delete_spex_revival");
        spexService.deleteRevival(spexId, revivalId);

        return "Deleted revival %d of spex %d".formatted(revivalId, spexId);
    }

    @McpTool(name = "list_spex_categories", description = "List spex categories (e.g. the different spex ensembles). Filterable fields: name, firstYear.",
            annotations = @McpAnnotations(readOnlyHint = true, openWorldHint = false))
    public PageResult<McpSpexCategory> listSpexCategories(@McpToolParam(required = false, description = FILTER_DESCRIPTION) final @Nullable String filter,
                                                          @McpToolParam(required = false, description = PAGE_DESCRIPTION) final @Nullable Integer page,
                                                          @McpToolParam(required = false, description = SIZE_DESCRIPTION) final @Nullable Integer size) {
        return PageResult.of(categoryService.find(filter(filter), pageable(page, size, Sort.by(SpexCategory_.NAME))), McpSpexCategory::of);
    }

    @McpTool(name = "get_spex_category", description = "Get a spex category",
            annotations = @McpAnnotations(readOnlyHint = true, openWorldHint = false))
    public McpSpexCategory getSpexCategory(@McpToolParam(description = "Spex category id") final Long id) {
        return McpSpexCategory.of(categoryService.findById(id));
    }

    @McpTool(name = "create_spex_category", description = "Create a spex category. Requires the ADMIN role.",
            annotations = @McpAnnotations(destructiveHint = false, openWorldHint = false))
    public McpSpexCategory createSpexCategory(@McpToolParam(description = "Category name") final String name,
                                              @McpToolParam(description = "First year, four digits") final String firstYear) {
        support.audit("create_spex_category");

        return McpSpexCategory.of(categoryService.create(support.validate(new SpexCategoryCreateDto(name, firstYear))));
    }

    @McpTool(name = "update_spex_category", description = "Update a spex category; omitted fields are left unchanged. Requires the ADMIN role.",
            annotations = @McpAnnotations(idempotentHint = true, openWorldHint = false))
    public McpSpexCategory updateSpexCategory(@McpToolParam(description = "Spex category id") final Long id,
                                              @McpToolParam(required = false, description = "New name") final @Nullable String name,
                                              @McpToolParam(required = false, description = "First year, four digits") final @Nullable String firstYear) {
        support.audit("update_spex_category");
        final SpexCategoryDto current = categoryService.findById(id);

        return McpSpexCategory.of(categoryService.update(support.validate(new SpexCategoryUpdateDto(
                id,
                orElse(name, current.getName()),
                orElse(firstYear, current.getFirstYear())
        ))));
    }

    @McpTool(name = "delete_spex_category", description = "Permanently delete a single spex category. Requires the ADMIN role.",
            annotations = @McpAnnotations(openWorldHint = false))
    public String deleteSpexCategory(@McpToolParam(description = "Spex category id") final Long id) {
        support.audit("delete_spex_category");
        categoryService.deleteById(id);

        return "Deleted spex category %d".formatted(id);
    }

    private @Nullable McpSpexCategory categoryOf(final Long spexId) {
        try {
            return McpSpexCategory.of(spexService.findCategoryBySpex(spexId));
        } catch (final ResourceNoValueException _) {
            return null;
        }
    }

    public record McpSpex(Long id, String year, String title, boolean revival) {

        static McpSpex of(final SpexDto dto) {
            return new McpSpex(dto.getId(), dto.getYear(), dto.getTitle(), Boolean.TRUE.equals(dto.getRevival()));
        }
    }

    public record McpSpexDetails(McpSpex spex, @Nullable McpSpexCategory category, List<McpSpex> revivals) {
    }

    public record McpSpexCategory(Long id, String name, String firstYear) {

        static McpSpexCategory of(final SpexCategoryDto dto) {
            return new McpSpexCategory(dto.getId(), dto.getName(), dto.getFirstYear());
        }
    }
}
