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

package nu.fgv.register.server.news;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventService;
import nu.fgv.register.server.impex.JobService;
import nu.fgv.register.server.impex.model.ExportType;
import nu.fgv.register.server.impex.model.JobReferenceDto;
import nu.fgv.register.server.util.graphql.GraphqlUtil;
import nu.fgv.register.server.util.security.RequiresAdminOrEditor;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.query.ScrollSubrange;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static nu.fgv.register.server.util.graphql.GraphqlUtil.extractScrollPositionAndLimitAndOrder;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class NewsGraphqlApi {

    private final NewsService service;
    private final EventService eventService;
    private final JobService jobService;

    @QueryMapping("newsPaged")
    @RequiresAdminOrEditorOrUser
    public Window<NewsDto> retrieve(final ScrollSubrange subrange, @Argument final Optional<String> filter, final Optional<Sort> sort) {
        final GraphqlUtil.ScrollPositionAndLimitHolder holder = extractScrollPositionAndLimitAndOrder(subrange);

        return service.find(filter.orElse(""), holder.limit(), sort.orElse(Sort.unsorted()), holder.scrollPosition());
    }

    @QueryMapping("newsExport")
    @RequiresAdminOrEditor
    public JobReferenceDto export(@Argument final List<Long> ids, @Argument final String filter, @Argument final ExportType type, final Locale locale) {
        return JobReferenceDto.builder()
                .id(jobService.createExportJob(NewsExportService.class, ids, filter, type, locale))
                .build();
    }

    @MutationMapping("newsCreate")
    @RequiresAdminOrEditor
    public NewsDto create(@Valid @Argument final NewsCreateDto input) {
        return service.create(input);
    }

    @QueryMapping("news")
    @RequiresAdminOrEditorOrUser
    public NewsDto retrieve(@Argument final Long id) {
        return service.findById(id);
    }

    @MutationMapping("newsUpdate")
    @RequiresAdminOrEditor
    public NewsDto update(@Valid @Argument final NewsUpdateDto input) {
        return service.update(input);
    }

    @MutationMapping("newsDelete")
    @RequiresAdminOrEditor
    public void delete(@Argument final Long id) {
        service.deleteById(id);
    }

    @QueryMapping("newsEvents")
    @RequiresAdminOrEditorOrUser
    public List<EventDto> events(@Argument final Long sourceId, @Nullable @Argument final Integer sinceInDays) {
        return eventService.findBySourceTypeAndId(Event.SourceType.NEWS, sourceId, Optional.ofNullable(sinceInDays).orElse(90));
    }
}
