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
import nu.fgv.register.server.news.NewsCreateDto;
import nu.fgv.register.server.news.NewsDto;
import nu.fgv.register.server.news.NewsService;
import nu.fgv.register.server.news.NewsUpdateDto;
import nu.fgv.register.server.news.News_;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static nu.fgv.register.server.mcp.McpToolSupport.FILTER_DESCRIPTION;
import static nu.fgv.register.server.mcp.McpToolSupport.PAGE_DESCRIPTION;
import static nu.fgv.register.server.mcp.McpToolSupport.SIZE_DESCRIPTION;
import static nu.fgv.register.server.mcp.McpToolSupport.filter;
import static nu.fgv.register.server.mcp.McpToolSupport.orElse;
import static nu.fgv.register.server.mcp.McpToolSupport.pageable;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
@RequiredArgsConstructor
public class NewsMcpTools {

    private static final String VISIBLE_FROM_DESCRIPTION = "First day the news is shown to users (ISO date, yyyy-MM-dd); without it the news is never published";
    private static final String VISIBLE_TO_DESCRIPTION = "Last day the news is shown to users (ISO date, yyyy-MM-dd); without it the news is shown indefinitely";

    private final NewsService service;
    private final McpToolSupport support;

    @McpTool(name = "list_news", description = "List news, newest first. Users with only the USER role see published news only. Filterable fields: subject, text, visibleFrom, visibleTo, published.",
            annotations = @McpAnnotations(readOnlyHint = true, openWorldHint = false))
    public PageResult<McpNews> listNews(@McpToolParam(required = false, description = FILTER_DESCRIPTION) final @Nullable String filter,
                                        @McpToolParam(required = false, description = PAGE_DESCRIPTION) final @Nullable Integer page,
                                        @McpToolParam(required = false, description = SIZE_DESCRIPTION) final @Nullable Integer size) {
        return PageResult.of(service.find(filter(filter), pageable(page, size, Sort.by(Sort.Direction.DESC, News_.VISIBLE_FROM))), McpNews::of);
    }

    @McpTool(name = "get_news", description = "Get a news item",
            annotations = @McpAnnotations(readOnlyHint = true, openWorldHint = false))
    public McpNews getNews(@McpToolParam(description = "News id") final Long id) {
        return McpNews.of(service.findById(id));
    }

    @McpTool(name = "create_news", description = "Create a news item. It is published automatically while today is within the visibility period. Requires the ADMIN or EDITOR role.",
            annotations = @McpAnnotations(destructiveHint = false, openWorldHint = false))
    public McpNews createNews(@McpToolParam(description = "Subject") final String subject,
                              @McpToolParam(description = "Text") final String text,
                              @McpToolParam(required = false, description = VISIBLE_FROM_DESCRIPTION) final @Nullable LocalDate visibleFrom,
                              @McpToolParam(required = false, description = VISIBLE_TO_DESCRIPTION) final @Nullable LocalDate visibleTo) {
        support.audit("create_news");

        return McpNews.of(service.create(support.validate(NewsCreateDto.builder()
                .subject(subject)
                .text(text)
                .visibleFrom(visibleFrom)
                .visibleTo(visibleTo)
                .build())));
    }

    @McpTool(name = "update_news", description = "Update a news item; omitted fields are left unchanged (a visibility date cannot be cleared once set). Requires the ADMIN or EDITOR role.",
            annotations = @McpAnnotations(idempotentHint = true, openWorldHint = false))
    public McpNews updateNews(@McpToolParam(description = "News id") final Long id,
                              @McpToolParam(required = false, description = "Subject") final @Nullable String subject,
                              @McpToolParam(required = false, description = "Text") final @Nullable String text,
                              @McpToolParam(required = false, description = VISIBLE_FROM_DESCRIPTION) final @Nullable LocalDate visibleFrom,
                              @McpToolParam(required = false, description = VISIBLE_TO_DESCRIPTION) final @Nullable LocalDate visibleTo) {
        support.audit("update_news");
        final NewsDto current = service.findById(id);

        return McpNews.of(service.update(support.validate(NewsUpdateDto.builder()
                .id(id)
                .subject(orElse(subject, current.getSubject()))
                .text(orElse(text, current.getText()))
                .visibleFrom(visibleFrom != null ? visibleFrom : current.getVisibleFrom())
                .visibleTo(visibleTo != null ? visibleTo : current.getVisibleTo())
                .build())));
    }

    @McpTool(name = "delete_news", description = "Permanently delete a single news item. Requires the ADMIN or EDITOR role.",
            annotations = @McpAnnotations(openWorldHint = false))
    public String deleteNews(@McpToolParam(description = "News id") final Long id) {
        support.audit("delete_news");
        service.deleteById(id);

        return "Deleted news %d".formatted(id);
    }

    public record McpNews(Long id, String subject, String text, @Nullable LocalDate visibleFrom, @Nullable LocalDate visibleTo, boolean published) {

        static McpNews of(final NewsDto dto) {
            return new McpNews(dto.getId(), dto.getSubject(), dto.getText(), dto.getVisibleFrom(), dto.getVisibleTo(), Boolean.TRUE.equals(dto.getPublished()));
        }
    }
}
