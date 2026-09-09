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

package nu.fgv.register.server.spexare.toggle;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.SpexareDto;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.data.query.ScrollSubrange;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;

import static nu.fgv.register.server.util.graphql.GraphqlUtil.extractScrollRequest;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class ToggleGraphqlApi {

    private final ToggleService service;

    @MutationMapping("toggleCreate")
    @RequiresAdminOrEditorOrUser
    public ToggleDto create(@Argument final Long spexareId, @Argument final String typeId, @Valid @Argument final ToggleCreateDto input) {
        return service.create(spexareId, typeId, input);
    }

    @MutationMapping("toggleUpdate")
    @RequiresAdminOrEditorOrUser
    public ToggleDto update(@Argument final Long spexareId, @Argument final String typeId, @Valid @Argument final ToggleUpdateDto input) {
        return service.update(spexareId, typeId, input.id(), input);
    }

    @MutationMapping("toggleDelete")
    @RequiresAdminOrEditorOrUser
    public void delete(@Argument final Long spexareId, @Argument final String typeId, @Argument final Long id) {
        service.deleteById(spexareId, typeId, id);
    }

    @SchemaMapping(typeName = "Spexare", field = "togglesPaged")
    @RequiresAdminOrEditorOrUser
    public CountedWindow<ToggleDto> retrieveBySpexare(final SpexareDto dto, final ScrollSubrange subrange, final Optional<Sort> sort) {
        return service.findBySpexare(dto.getId(), extractScrollRequest(subrange), sort.orElse(Sort.unsorted()));
    }

    @SchemaMapping(typeName = "Spexare", field = "toggles")
    @RequiresAdminOrEditorOrUser
    public List<ToggleDto> retrieveBySpexare(final SpexareDto dto) {
        return service.findBySpexare(dto.getId());
    }

}
