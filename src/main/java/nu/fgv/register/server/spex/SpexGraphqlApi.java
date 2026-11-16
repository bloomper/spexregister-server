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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spex.category.SpexCategoryDto;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import nu.fgv.register.server.util.security.RequiresSpexReader;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.query.ScrollSubrange;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static nu.fgv.register.server.util.graphql.GraphqlUtil.extractScrollRequest;

/**
 * The {@code Spex} queries and the fields that need loading, batched where a list is resolved. Read only: Spexipedia
 * keeps the spex and the sync writes the copy here.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class SpexGraphqlApi {

    private final SpexService service;

    private static List<UUID> ids(final List<SpexDto> dtos) {
        return dtos.stream().map(SpexDto::getId).distinct().toList();
    }

    @QueryMapping("spexPaged")
    @RequiresSpexReader
    public CountedWindow<SpexDto> retrieve(final ScrollSubrange subrange, @Argument final Optional<String> filter, final Optional<Sort> sort) {
        return service.find(filter.orElse(""), extractScrollRequest(subrange), sort.orElse(Sort.unsorted()));
    }

    @QueryMapping("spex")
    @RequiresSpexReader
    public SpexDto retrieve(@Argument final UUID id) {
        return service.findById(id);
    }

    @BatchMapping(typeName = "Spex", field = "parent")
    @RequiresAdminOrEditorOrUser
    public Map<SpexDto, SpexDto> retrieveParents(final List<SpexDto> dtos) {
        final Map<UUID, SpexDto> parents = service.findParentsBySpex(ids(dtos));

        return dtos.stream()
                .filter(dto -> parents.containsKey(dto.getId()))
                .collect(Collectors.toMap(Function.identity(), dto -> parents.get(dto.getId()), (a, _) -> a));
    }

    @BatchMapping(typeName = "Spex", field = "revivals")
    @RequiresSpexReader
    public Map<SpexDto, List<SpexDto>> retrieveRevivalsByParents(final List<SpexDto> dtos) {
        final Map<UUID, List<SpexDto>> revivals = service.findRevivalsByParents(ids(dtos));

        return dtos.stream()
                .collect(Collectors.toMap(Function.identity(), dto -> revivals.getOrDefault(dto.getId(), List.of()), (a, _) -> a));
    }

    @BatchMapping(typeName = "Spex", field = "category")
    @RequiresAdminOrEditorOrUser
    public Map<SpexDto, SpexCategoryDto> retrieveCategories(final List<SpexDto> dtos) {
        final Map<UUID, SpexCategoryDto> categories = service.findCategoriesBySpex(ids(dtos));

        return dtos.stream()
                .filter(dto -> categories.containsKey(dto.getId()))
                .collect(Collectors.toMap(Function.identity(), dto -> categories.get(dto.getId()), (a, _) -> a));
    }

}
