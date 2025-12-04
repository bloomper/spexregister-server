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

package nu.fgv.register.server.spexare;

import graphql.GraphQLContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventService;
import nu.fgv.register.server.util.error.ResourceNoValueException;
import nu.fgv.register.server.util.graphql.GraphqlUtil;
import nu.fgv.register.server.util.search.WindowWithFacets;
import nu.fgv.register.server.util.security.RequiresAdmin;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
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
public class SpexareGraphqlApi {

    private final SpexareService service;
    private final EventService eventService;

    @QueryMapping("spexarePaged")
    @RequiresAdminOrEditorOrUser
    public Window<SpexareDto> retrieve(final ScrollSubrange subrange, @Nullable @Argument final String filter, @Nullable final Sort sort) {
        final GraphqlUtil.ScrollPositionAndLimitHolder holder = extractScrollPositionAndLimitAndOrder(subrange);

        return service.find(Optional.ofNullable(filter).orElse(""), holder.limit(), Optional.ofNullable(sort).orElse(Sort.unsorted()), holder.scrollPosition());
    }

    @QueryMapping("spexareSearchPaged")
    @RequiresAdminOrEditorOrUser
    public WindowWithFacets<SpexareDto> retrieve(final GraphQLContext graphQLContext, @Nullable @Argument final Integer offset, @Nullable @Argument final Integer limit, @Argument final String q, @Nullable final Sort sort) {
        final WindowWithFacets<SpexareDto> result = service.search(q, Optional.ofNullable(offset).orElse(0), Optional.ofNullable(limit).orElse(20), Optional.ofNullable(sort).orElse(Sort.unsorted()));

        graphQLContext.put("facets", result.getFacets());

        return result;
    }

    @MutationMapping("spexareCreate")
    @RequiresAdmin
    public SpexareDto create(@Valid @Argument final SpexareCreateDto input) {
        return service.create(input);
    }

    @QueryMapping("spexare")
    @RequiresAdminOrEditorOrUser
    public SpexareDto retrieve(@Argument final Long id) {
        return service.findById(id);
    }

    @MutationMapping("spexareUpdate")
    @RequiresAdminOrEditorOrUser
    public SpexareDto update(@Valid @Argument final SpexareUpdateDto input) {
        return service.update(input);
    }

    @MutationMapping("spexareDelete")
    @RequiresAdmin
    public void delete(@Argument final Long id) {
        service.deleteById(id);
    }

    @MutationMapping("spexareImageDelete")
    @RequiresAdminOrEditorOrUser
    public void deleteImage(@Argument final Long id) {
        service.deleteImage(id);
    }

    @SchemaMapping(typeName = "Spexare", field = "partner")
    @RequiresAdminOrEditorOrUser
    public @Nullable SpexareDto retrievePartner(final SpexareDto dto) {
        try {
            return service.findPartnerBySpexare(dto.getId());
        } catch (final ResourceNoValueException e) {
            // Ignore
            return null;
        }
    }

    @MutationMapping("spexarePartnerAdd")
    @RequiresAdminOrEditorOrUser
    public void addPartner(@Argument final Long spexareId, @Argument final Long id) {
        service.addPartner(spexareId, id);
    }

    @MutationMapping("spexarePartnerRemove")
    @RequiresAdminOrEditorOrUser
    public void removePartner(@Argument final Long spexareId) {
        service.removePartner(spexareId);
    }

    @QueryMapping("spexareEvents")
    @RequiresAdmin
    public List<EventDto> events(@Nullable @Argument final Integer sinceInDays) {
        return eventService.findBySource(Optional.ofNullable(sinceInDays).orElse(90), Event.SourceType.SPEXARE);
    }

}
