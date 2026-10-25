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
import nu.fgv.register.server.impex.JobService;
import nu.fgv.register.server.impex.model.ImpexType;
import nu.fgv.register.server.impex.model.JobReferenceDto;
import nu.fgv.register.server.spex.category.SpexCategoryDto;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.security.RequiresAdmin;
import nu.fgv.register.server.util.security.RequiresAdminOrEditor;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUserOrRevival;
import nu.fgv.register.server.util.security.RequiresReferenceDataReader;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.data.query.ScrollSubrange;
import org.springframework.stereotype.Controller;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static nu.fgv.register.server.util.graphql.GraphqlUtil.extractScrollRequest;

/**
 * The {@code Spex} queries and mutations and the fields that need loading, batched where a list is resolved.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class SpexGraphqlApi {

    private final SpexService service;
    private final JobService jobService;

    @QueryMapping("spexPaged")
    @RequiresReferenceDataReader
    public CountedWindow<SpexDto> retrieve(final ScrollSubrange subrange, @Argument final Optional<String> filter, final Optional<Sort> sort) {
        return service.find(filter.orElse(""), extractScrollRequest(subrange), sort.orElse(Sort.unsorted()));
    }

    @QueryMapping("spexExport")
    @RequiresAdminOrEditor
    public JobReferenceDto export(@Nullable @Argument final List<Long> ids, @Nullable @Argument final String filter, @Argument final ImpexType type, final Locale locale) {
        return JobReferenceDto.builder()
                .id(jobService.createExportJob(SpexExportService.class, Optional.ofNullable(ids).orElse(Collections.emptyList()), Optional.ofNullable(filter).orElse(""), type, locale))
                .build();
    }

    @MutationMapping("spexCreate")
    @RequiresAdmin
    public SpexDto create(@Valid @Argument final SpexCreateDto input) {
        return service.create(input);
    }

    @QueryMapping("spex")
    @RequiresReferenceDataReader
    public SpexDto retrieve(@Argument final Long id) {
        return service.findById(id);
    }

    @MutationMapping("spexUpdate")
    @RequiresAdminOrEditorOrUser
    public SpexDto update(@Valid @Argument final SpexUpdateDto input) {
        return service.update(input);
    }

    @MutationMapping("spexDelete")
    @RequiresAdminOrEditor
    public void delete(@Argument final Long id) {
        service.deleteById(id);
    }

    @MutationMapping("spexPosterDelete")
    @RequiresAdminOrEditorOrUser
    public void deletePoster(@Argument final Long id) {
        service.deletePoster(id);
    }

    @BatchMapping(typeName = "Spex", field = "parent")
    @RequiresAdminOrEditorOrUser
    public Map<SpexDto, SpexDto> retrieveParents(final List<SpexDto> dtos) {
        final Map<Long, SpexDto> parents = service.findParentsBySpex(ids(dtos));

        return dtos.stream()
                .filter(dto -> parents.containsKey(dto.getId()))
                .collect(Collectors.toMap(Function.identity(), dto -> parents.get(dto.getId()), (a, _) -> a));
    }

    @SchemaMapping(typeName = "Spex", field = "revivalsPaged")
    @RequiresReferenceDataReader
    public CountedWindow<SpexDto> retrieveRevivalsByParent(final SpexDto dto, final ScrollSubrange subrange, final Optional<Sort> sort) {
        return service.findRevivalsByParent(dto.getId(), extractScrollRequest(subrange), sort.orElse(Sort.unsorted()));
    }

    @BatchMapping(typeName = "Spex", field = "revivals")
    @RequiresReferenceDataReader
    public Map<SpexDto, List<SpexDto>> retrieveRevivalsByParents(final List<SpexDto> dtos) {
        final Map<Long, List<SpexDto>> revivals = service.findRevivalsByParents(ids(dtos));

        return dtos.stream()
                .collect(Collectors.toMap(Function.identity(), dto -> revivals.getOrDefault(dto.getId(), List.of()), (a, _) -> a));
    }

    @QueryMapping("spexRevival")
    @RequiresReferenceDataReader
    public SpexDto retrieveRevival(@Argument final Long spexId, @Argument final Long id) {
        return service.findRevivalById(spexId, id);
    }

    @MutationMapping("spexRevivalCreate")
    @RequiresAdminOrEditorOrUserOrRevival
    public SpexDto createRevival(@Argument final Long spexId, @Argument final String year) {
        return service.addRevival(spexId, year);
    }

    @MutationMapping("spexRevivalDelete")
    @RequiresAdminOrEditorOrUser
    public void deleteRevival(@Argument final Long spexId, @Argument final Long id) {
        service.deleteRevival(spexId, id);
    }

    @BatchMapping(typeName = "Spex", field = "category")
    @RequiresAdminOrEditorOrUser
    public Map<SpexDto, SpexCategoryDto> retrieveCategories(final List<SpexDto> dtos) {
        final Map<Long, SpexCategoryDto> categories = service.findCategoriesBySpex(ids(dtos));

        return dtos.stream()
                .filter(dto -> categories.containsKey(dto.getId()))
                .collect(Collectors.toMap(Function.identity(), dto -> categories.get(dto.getId()), (a, _) -> a));
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

    private static List<Long> ids(final List<SpexDto> dtos) {
        return dtos.stream().map(SpexDto::getId).distinct().toList();
    }
}
