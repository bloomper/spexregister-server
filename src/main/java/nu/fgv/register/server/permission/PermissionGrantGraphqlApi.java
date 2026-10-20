/*
 * Copyright 2026 the original author or authors.
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

package nu.fgv.register.server.permission;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.spex.SpexDto;
import nu.fgv.register.server.spex.SpexService;
import nu.fgv.register.server.spexare.SpexareDto;
import nu.fgv.register.server.spexare.SpexareService;
import nu.fgv.register.server.user.UserDto;
import nu.fgv.register.server.user.UserService;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.security.RequiresAdmin;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.data.query.ScrollSubrange;
import org.springframework.stereotype.Controller;

import java.util.Optional;

import static nu.fgv.register.server.util.graphql.GraphqlUtil.extractScrollRequest;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@RequiredArgsConstructor
@Controller
public class PermissionGrantGraphqlApi {

    private final PermissionGrantService service;
    private final UserService userService;
    private final SpexareService spexareService;
    private final SpexService spexService;

    @QueryMapping("permissionGrantPaged")
    @RequiresAdmin
    public CountedWindow<PermissionGrantDto> retrieve(final ScrollSubrange subrange, @Argument final Optional<String> filter, final Optional<Sort> sort) {
        return service.find(filter.orElse(""), extractScrollRequest(subrange), sort.orElse(Sort.unsorted()));
    }

    @QueryMapping("permissionGrant")
    @RequiresAdmin
    public PermissionGrantDto retrieve(@Argument final Long id) {
        return service.findById(id);
    }

    @MutationMapping("permissionGrantCreate")
    @RequiresAdmin
    public PermissionGrantDto create(@Valid @Argument final PermissionGrantCreateDto input) {
        return service.create(input);
    }

    @MutationMapping("permissionGrantUpdate")
    @RequiresAdmin
    public PermissionGrantDto update(@Valid @Argument final PermissionGrantUpdateDto input) {
        return service.update(input);
    }

    @MutationMapping("permissionGrantDelete")
    @RequiresAdmin
    public void delete(@Argument final Long id) {
        service.deleteById(id);
    }

    @SchemaMapping(typeName = "PermissionGrant", field = "user")
    @RequiresAdmin
    public @Nullable UserDto user(final PermissionGrantDto grant) {
        return grant.getUserId() == null ? null : userService.findById(grant.getUserId());
    }

    @SchemaMapping(typeName = "PermissionGrant", field = "spexare")
    @RequiresAdmin
    public @Nullable SpexareDto spexare(final PermissionGrantDto grant) {
        return grant.getSpexareId() == null ? null : spexareService.findById(grant.getSpexareId());
    }

    @SchemaMapping(typeName = "PermissionGrant", field = "spex")
    @RequiresAdmin
    public @Nullable SpexDto spex(final PermissionGrantDto grant) {
        return grant.getSpexId() == null ? null : spexService.findById(grant.getSpexId());
    }
}
