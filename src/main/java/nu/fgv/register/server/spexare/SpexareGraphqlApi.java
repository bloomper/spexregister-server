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
import nu.fgv.register.server.impex.JobService;
import nu.fgv.register.server.impex.model.ExportType;
import nu.fgv.register.server.impex.model.JobReferenceDto;
import nu.fgv.register.server.impex.model.ReportType;
import nu.fgv.register.server.util.graphql.GraphqlUtil;
import nu.fgv.register.server.util.search.AggregationFilter;
import nu.fgv.register.server.util.search.WindowWithFacets;
import nu.fgv.register.server.util.security.RequiresAdmin;
import nu.fgv.register.server.util.security.RequiresAdminOrEditor;
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
public class SpexareGraphqlApi {

    private final SpexareService service;
    private final EventService eventService;
    private final JobService jobService;

    @QueryMapping("spexarePaged")
    @RequiresAdminOrEditorOrUser
    public Window<SpexareDto> retrieve(final ScrollSubrange subrange, @Argument final Optional<String> filter, final Optional<Sort> sort) {
        final GraphqlUtil.ScrollPositionAndLimitHolder holder = extractScrollPositionAndLimitAndOrder(subrange);

        return service.find(filter.orElse(""), holder.limit(), sort.orElse(Sort.unsorted()), holder.scrollPosition());
    }

    @QueryMapping("spexareSearchPaged")
    @RequiresAdminOrEditorOrUser
    public WindowWithFacets<SpexareDto> retrieve(final GraphQLContext graphQLContext, @Argument final Optional<Integer> offset, @Argument final Optional<Integer> limit, @Argument final String q, @Argument final List<AggregationFilter> aggregationFilters, final Optional<Sort> sort) {
        final WindowWithFacets<SpexareDto> result = service.search(q, aggregationFilters, offset.orElse(0), limit.orElse(20), sort.orElse(Sort.unsorted()));

        graphQLContext.put("facets", result.getFacets());

        return result;
    }

    @QueryMapping("spexareExport")
    @RequiresAdminOrEditor
    public JobReferenceDto export(@Argument final List<Long> ids, @Argument final String filter, @Argument final ExportType type, @Argument final ReportType reportType, final Locale locale) {
        return JobReferenceDto.builder()
                .id(jobService.createExportJob(SpexareExportService.class, ids, filter, type, reportType, locale))
                .build();
    }

    @MutationMapping("spexareCreate")
    @RequiresAdminOrEditor
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
        return service.findPartnerBySpexare(dto.getId())
                .orElse(null);
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
    @RequiresAdminOrEditorOrUser
    public List<EventDto> events(@Argument final Long sourceId, @Nullable @Argument final Integer sinceInDays) {
        return eventService.findBySourceTypeAndId(Event.SourceType.SPEXARE, sourceId, Optional.ofNullable(sinceInDays).orElse(90));
    }

}
