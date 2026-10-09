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
import nu.fgv.register.server.tag.TagCreateDto;
import nu.fgv.register.server.tag.TagDto;
import nu.fgv.register.server.tag.TagService;
import nu.fgv.register.server.tag.TagUpdateDto;
import nu.fgv.register.server.tag.Tag_;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import static nu.fgv.register.server.mcp.McpToolSupport.FILTER_DESCRIPTION;
import static nu.fgv.register.server.mcp.McpToolSupport.PAGE_DESCRIPTION;
import static nu.fgv.register.server.mcp.McpToolSupport.SIZE_DESCRIPTION;
import static nu.fgv.register.server.mcp.McpToolSupport.filter;
import static nu.fgv.register.server.mcp.McpToolSupport.pageable;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
@RequiredArgsConstructor
public class TagMcpTools {

    private final TagService service;
    private final McpToolSupport support;

    @McpTool(name = "list_tags", description = "List tags. Filterable fields: name.",
            annotations = @McpAnnotations(readOnlyHint = true, openWorldHint = false))
    public PageResult<McpTag> listTags(@McpToolParam(required = false, description = FILTER_DESCRIPTION) final @Nullable String filter,
                                       @McpToolParam(required = false, description = PAGE_DESCRIPTION) final @Nullable Integer page,
                                       @McpToolParam(required = false, description = SIZE_DESCRIPTION) final @Nullable Integer size) {
        return PageResult.of(service.find(filter(filter), pageable(page, size, Sort.by(Tag_.NAME))), McpTag::of);
    }

    @McpTool(name = "get_tag", description = "Get a tag",
            annotations = @McpAnnotations(readOnlyHint = true, openWorldHint = false))
    public McpTag getTag(@McpToolParam(description = "Tag id") final Long id) {
        return McpTag.of(service.findById(id));
    }

    @McpTool(name = "create_tag", description = "Create a tag. Requires the ADMIN or EDITOR role.",
            annotations = @McpAnnotations(destructiveHint = false, openWorldHint = false))
    public McpTag createTag(@McpToolParam(description = "Tag name") final String name) {
        support.audit("create_tag");

        return McpTag.of(service.create(support.validate(TagCreateDto.builder().name(name).build())));
    }

    @McpTool(name = "update_tag", description = "Rename a tag. Requires the ADMIN or EDITOR role.",
            annotations = @McpAnnotations(idempotentHint = true, openWorldHint = false))
    public McpTag updateTag(@McpToolParam(description = "Tag id") final Long id,
                            @McpToolParam(description = "New tag name") final String name) {
        support.audit("update_tag");

        return McpTag.of(service.update(support.validate(new TagUpdateDto(id, name))));
    }

    @McpTool(name = "delete_tag", description = "Permanently delete a single tag. Requires the ADMIN or EDITOR role.",
            annotations = @McpAnnotations(openWorldHint = false))
    public String deleteTag(@McpToolParam(description = "Tag id") final Long id) {
        support.audit("delete_tag");
        service.deleteById(id);

        return "Deleted tag %d".formatted(id);
    }

    public record McpTag(Long id, String name) {

        static McpTag of(final TagDto dto) {
            return new McpTag(dto.getId(), dto.getName());
        }
    }
}
