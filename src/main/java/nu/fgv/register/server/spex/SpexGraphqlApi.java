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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventService;
import nu.fgv.register.server.impex.JobService;
import nu.fgv.register.server.impex.model.ExportType;
import nu.fgv.register.server.impex.model.JobReferenceDto;
import nu.fgv.register.server.spex.category.SpexCategoryDto;
import nu.fgv.register.server.util.error.ResourceNoValueException;
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
public class SpexGraphqlApi {

    private final SpexService service;
    private final EventService eventService;
    private final JobService jobService;

    @QueryMapping("spexPaged")
    @RequiresAdminOrEditorOrUser
    public Window<SpexDto> retrieve(final ScrollSubrange subrange, @Argument final Optional<String> filter, final Optional<Sort> sort) {
        final GraphqlUtil.ScrollPositionAndLimitHolder holder = extractScrollPositionAndLimitAndOrder(subrange);

        return service.find(filter.orElse(""), holder.limit(), sort.orElse(Sort.unsorted()), holder.scrollPosition());
    }

    @QueryMapping("spexExport")
    @RequiresAdminOrEditor
    public JobReferenceDto export(@Argument final List<Long> ids, @Argument final String filter, @Argument final ExportType type, final Locale locale) {
        return JobReferenceDto.builder()
                .id(jobService.createExportJob(SpexExportService.class, ids, filter, type, locale))
                .build();
    }

    @MutationMapping("spexCreate")
    @RequiresAdmin
    public SpexDto create(@Valid @Argument final SpexCreateDto input) {
        return service.create(input);
    }

    @QueryMapping("spex")
    @RequiresAdminOrEditorOrUser
    public SpexDto retrieve(@Argument final Long id) {
        return service.findById(id);
    }

    @MutationMapping("spexUpdate")
    @RequiresAdminOrEditor
    public SpexDto update(@Valid @Argument final SpexUpdateDto input) {
        return service.update(input);
    }

    @MutationMapping("spexDelete")
    @RequiresAdminOrEditor
    public void delete(@Argument final Long id) {
        service.deleteById(id);
    }

    @MutationMapping("spexPosterDelete")
    @RequiresAdminOrEditor
    public void deletePoster(@Argument final Long id) {
        service.deletePoster(id);
    }

    @SchemaMapping(typeName = "Spex", field = "parent")
    @RequiresAdminOrEditorOrUser
    public @Nullable SpexDto retrieveParent(final SpexDto dto) {
        try {
            return service.findParentById(dto.getId());
        } catch (final ResourceNoValueException _) {
            // Ignore
            return null;
        }
    }

    @SchemaMapping(typeName = "Spex", field = "revivalsPaged")
    @RequiresAdminOrEditorOrUser
    public Window<SpexDto> retrieveRevivalsByParent(final SpexDto dto, final ScrollSubrange subrange, final Optional<Sort> sort) {
        final GraphqlUtil.ScrollPositionAndLimitHolder holder = extractScrollPositionAndLimitAndOrder(subrange);

        return service.findRevivalsByParent(dto.getId(), holder.limit(), sort.orElse(Sort.unsorted()), holder.scrollPosition());
    }

    @SchemaMapping(typeName = "Spex", field = "revivals")
    @RequiresAdminOrEditorOrUser
    public List<SpexDto> retrieveRevivalsByParent(final SpexDto dto) {
        return service.findRevivalsByParent(dto.getId());
    }

    @QueryMapping("spexRevival")
    @RequiresAdminOrEditorOrUser
    public SpexDto retrieveRevival(@Argument final Long spexId, @Argument final Long id) {
        return service.findRevivalById(spexId, id);
    }

    @MutationMapping("spexRevivalCreate")
    @RequiresAdminOrEditor
    public SpexDto createRevival(@Argument final Long spexId, @Argument final String year) {
        return service.addRevival(spexId, year);
    }

    @MutationMapping("spexRevivalDelete")
    @RequiresAdminOrEditor
    public void deleteRevival(@Argument final Long spexId, @Argument final Long id) {
        service.deleteRevival(spexId, id);
    }

    @SchemaMapping(typeName = "Spex", field = "category")
    @RequiresAdminOrEditorOrUser
    public @Nullable SpexCategoryDto retrieveCategory(final SpexDto dto) {
        try {
            return service.findCategoryBySpex(dto.getId());
        } catch (final ResourceNoValueException _) {
            // Ignore
            return null;
        }
    }

    @MutationMapping("spexCategoryAdd")
    @RequiresAdmin
    public void addCategory(@Argument final Long spexId, @Argument final Long id) {
        service.addCategory(spexId, id);
    }

    @MutationMapping("spexCategoryRemove")
    @RequiresAdmin
    public void removeCategory(@Argument final Long spexId) {
        service.removeCategory(spexId);
    }

    @QueryMapping("spexEvents")
    @RequiresAdminOrEditorOrUser
    public List<EventDto> events(@Argument final Long sourceId, @Nullable @Argument final Integer sinceInDays) {
        return eventService.findBySourceTypeAndId(Event.SourceType.SPEX, sourceId, Optional.ofNullable(sinceInDays).orElse(90));
    }

}
