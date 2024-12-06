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

package nu.fgv.register.server.spexare.membership;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.SpexareDto;
import nu.fgv.register.server.util.graphql.GraphqlUtil;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
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
public class MembershipGraphqlApi {

    private final MembershipService service;

    @QueryMapping("membershipPaged")
    @RequiresAdminOrEditorOrUser
    public Window<MembershipDto> retrieve(@Argument final Long spexareId, final ScrollSubrange subrange, @Argument final Optional<String> filter, final Optional<Sort> sort) {
        final GraphqlUtil.ScrollPositionAndLimitHolder holder = extractScrollPositionAndLimitAndOrder(subrange);

        return service.findBySpexare(spexareId, filter.orElse(""), holder.limit(), sort.orElse(Sort.unsorted()), holder.scrollPosition());
    }

    @MutationMapping("membershipCreate")
    @RequiresAdminOrEditorOrUser
    public MembershipDto create(@Argument final Long spexareId, @Argument final String typeId, @Valid @Argument final MembershipCreateDto input) {
        return service.create(spexareId, typeId, input);
    }

    @QueryMapping("membership")
    @RequiresAdminOrEditorOrUser
    public MembershipDto retrieve(@Argument final Long spexareId, @Argument final Long id) {
        return service.findById(spexareId, id);
    }

    @MutationMapping("membershipDelete")
    @RequiresAdminOrEditorOrUser
    public void delete(@Argument final Long spexareId, @Argument final String typeId, @Argument final Long id) {
        service.deleteById(spexareId, typeId, id);
    }

    @SchemaMapping(typeName = "Spexare", field = "membershipsPaged")
    @RequiresAdminOrEditorOrUser
    public Window<MembershipDto> retrieveBySpexare(final SpexareDto dto, final ScrollSubrange subrange, @Argument final Optional<String> filter, final Optional<Sort> sort) {
        final GraphqlUtil.ScrollPositionAndLimitHolder holder = extractScrollPositionAndLimitAndOrder(subrange);

        return service.findBySpexare(dto.getId(), filter.orElse(""), holder.limit(), sort.orElse(Sort.unsorted()), holder.scrollPosition());
    }

    @SchemaMapping(typeName = "Spexare", field = "memberships")
    @RequiresAdminOrEditorOrUser
    public List<MembershipDto> retrieveBySpexare(final SpexareDto dto) {
        return service.findBySpexare(dto.getId());
    }

    @SchemaMapping(typeName = "SpexarePartner", field = "membershipsPaged")
    @RequiresAdminOrEditorOrUser
    public Window<MembershipDto> retrieveBySpexarePartner(final SpexareDto dto, final ScrollSubrange subrange, @Argument final Optional<String> filter, final Optional<Sort> sort) {
        return retrieveBySpexare(dto, subrange, filter, sort);
    }

    @SchemaMapping(typeName = "SpexarePartner", field = "memberships")
    @RequiresAdminOrEditorOrUser
    public List<MembershipDto> retrieveBySpexarePartner(final SpexareDto dto) {
        return retrieveBySpexare(dto);
    }
}
