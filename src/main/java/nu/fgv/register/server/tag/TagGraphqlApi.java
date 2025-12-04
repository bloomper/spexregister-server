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
import nu.fgv.register.server.util.graphql.GraphqlUtil;
import nu.fgv.register.server.util.security.RequiresAdmin;
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

    @QueryMapping("tagPaged")
    @RequiresAdminOrEditorOrUser
    public Window<TagDto> retrieve(final ScrollSubrange subrange, @Nullable @Argument final String filter, @Nullable final Sort sort) {
        final GraphqlUtil.ScrollPositionAndLimitHolder holder = extractScrollPositionAndLimitAndOrder(subrange);

        return service.find(Optional.ofNullable(filter).orElse(""), holder.limit(), Optional.ofNullable(sort).orElse(Sort.unsorted()), holder.scrollPosition());
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
    @RequiresAdmin
    public List<EventDto> events(@Nullable @Argument final Integer sinceInDays) {
        return eventService.findBySource(Optional.ofNullable(sinceInDays).orElse(90), Event.SourceType.TAG);
    }
}
