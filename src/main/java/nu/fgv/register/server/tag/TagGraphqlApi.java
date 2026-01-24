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

package nu.fgv.register.server.tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventService;
import nu.fgv.register.server.impex.JobService;
import nu.fgv.register.server.impex.model.ImpexType;
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

import java.util.Collections;
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
public class TagGraphqlApi {

    private final TagService service;
    private final EventService eventService;
    private final JobService jobService;

    @QueryMapping("tagPaged")
    @RequiresAdminOrEditorOrUser
    public Window<TagDto> retrieve(final ScrollSubrange subrange, @Argument final Optional<String> filter, final Optional<Sort> sort) {
        final GraphqlUtil.ScrollPositionAndLimitHolder holder = extractScrollPositionAndLimitAndOrder(subrange);

        return service.find(filter.orElse(""), holder.limit(), sort.orElse(Sort.unsorted()), holder.scrollPosition());
    }

    @QueryMapping("tagExport")
    @RequiresAdminOrEditor
    public JobReferenceDto export(@Nullable @Argument final List<Long> ids, @Nullable @Argument final String filter, @Argument final ImpexType type, final Locale locale) {
        return JobReferenceDto.builder()
                .id(jobService.createExportJob(TagExportService.class, Optional.ofNullable(ids).orElse(Collections.emptyList()), Optional.ofNullable(filter).orElse(""), type, locale))
                .build();
    }

    @MutationMapping("tagCreate")
    @RequiresAdminOrEditor
    public TagDto create(@Valid @Argument final TagCreateDto input) {
        return service.create(input);
    }

    @QueryMapping("tag")
    @RequiresAdminOrEditorOrUser
    public TagDto retrieve(@Argument final Long id) {
        return service.findById(id);
    }

    @MutationMapping("tagUpdate")
    @RequiresAdminOrEditor
    public TagDto update(@Valid @Argument final TagUpdateDto input) {
        return service.update(input);
    }

    @MutationMapping("tagDelete")
    @RequiresAdminOrEditor
    public void delete(@Argument final Long id) {
        service.deleteById(id);
    }

    @QueryMapping("tagEvents")
    @RequiresAdminOrEditorOrUser
    public List<EventDto> events(@Argument final Long sourceId, @Nullable @Argument final Integer sinceInDays) {
        return eventService.findBySourceTypeAndId(Event.SourceType.TAG, sourceId, Optional.ofNullable(sinceInDays).orElse(90));
    }
}
