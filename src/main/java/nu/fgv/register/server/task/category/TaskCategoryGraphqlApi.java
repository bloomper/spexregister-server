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

package nu.fgv.register.server.task.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.security.RequiresReferenceDataReader;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.query.ScrollSubrange;
import org.springframework.stereotype.Controller;

import java.util.Optional;
import java.util.UUID;

import static nu.fgv.register.server.util.graphql.GraphqlUtil.extractScrollRequest;

/**
 * The {@code TaskCategory} queries. Read only: Spexipedia keeps the categories and the sync writes the copy here.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class TaskCategoryGraphqlApi {

    private final TaskCategoryService service;

    @QueryMapping("taskCategoryPaged")
    @RequiresReferenceDataReader
    public CountedWindow<TaskCategoryDto> retrieve(final ScrollSubrange subrange, @Argument final Optional<String> filter, final Optional<Sort> sort) {
        return service.find(filter.orElse(""), extractScrollRequest(subrange), sort.orElse(Sort.unsorted()));
    }

    @QueryMapping("taskCategory")
    @RequiresReferenceDataReader
    public TaskCategoryDto retrieve(@Argument final UUID id) {
        return service.findById(id);
    }

}
