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

package nu.fgv.register.server.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.impex.JobService;
import nu.fgv.register.server.impex.model.ImpexType;
import nu.fgv.register.server.impex.model.JobReferenceDto;
import nu.fgv.register.server.spexare.SpexareDto;
import nu.fgv.register.server.user.authority.AuthorityDto;
import nu.fgv.register.server.user.state.StateDto;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.security.RequiresAdmin;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.data.query.ScrollSubrange;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static nu.fgv.register.server.util.graphql.GraphqlUtil.extractScrollRequest;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class UserGraphqlApi {

    private final UserService service;
    private final JobService jobService;

    @QueryMapping("userPaged")
    @RequiresAdmin
    public CountedWindow<UserDto> retrieve(final ScrollSubrange subrange, @Argument final Optional<String> filter, final Optional<Sort> sort) {
        return service.find(filter.orElse(""), extractScrollRequest(subrange), sort.orElse(Sort.unsorted()));
    }

    @QueryMapping("userExport")
    @RequiresAdmin
    public JobReferenceDto export(@Nullable @Argument final List<Long> ids, @Nullable @Argument final String filter, @Argument final ImpexType type, final Locale locale) {
        return JobReferenceDto.builder()
                .id(jobService.createExportJob(UserExportService.class, Optional.ofNullable(ids).orElse(Collections.emptyList()), Optional.ofNullable(filter).orElse(""), type, locale))
                .build();
    }

    @MutationMapping("userCreate")
    @RequiresAdmin
    public UserDto create(@Valid @Argument final UserCreateDto input) {
        return service.create(input);
    }

    @QueryMapping
    @Nullable
    public UserDto me(@AuthenticationPrincipal final Jwt jwt) {
        return service.findByExternalId(jwt.getSubject());
    }

    @QueryMapping("user")
    @RequiresAdmin
    public UserDto retrieve(@Argument final Long id) {
        return service.findById(id);
    }

    @MutationMapping("userUpdate")
    @RequiresAdmin
    public UserDto update(@Valid @Argument final UserUpdateDto input) {
        return service.update(input);
    }

    @MutationMapping("userDelete")
    @RequiresAdmin
    public void delete(@Argument final Long id) {
        service.deleteById(id);
    }

    @SchemaMapping(typeName = "User", field = "authorities")
    @RequiresAdmin
    public Set<AuthorityDto> retrieveAuthorities(final UserDto dto) {
        return service.getAuthoritiesByUser(dto.getId());
    }

    @MutationMapping("userAuthorityAdd")
    @RequiresAdmin
    public void addAuthority(@Argument final Long userId, @Argument final String id) {
        service.addAuthority(userId, id);
    }

    @MutationMapping("userAuthoritiesAdd")
    @RequiresAdmin
    public void addAuthorities(@Argument final Long userId, @Argument final List<String> ids) {
        service.addAuthorities(userId, ids);
    }

    @MutationMapping("userAuthorityRemove")
    @RequiresAdmin
    public void removeAuthority(@Argument final Long userId, @Argument final String id) {
        service.removeAuthority(userId, id);
    }

    @MutationMapping("userAuthoritiesRemove")
    @RequiresAdmin
    public void removeAuthorities(@Argument final Long userId, @Argument final List<String> ids) {
        service.removeAuthorities(userId, ids);
    }

    @SchemaMapping(typeName = "User", field = "state")
    @RequiresAdmin
    public StateDto retrieveState(final UserDto dto) {
        return service.getStateByUser(dto.getId());
    }

    @MutationMapping("userStateSet")
    @RequiresAdmin
    public void setState(@Argument final Long userId, @Argument final String id) {
        service.setState(userId, id);
    }

    @SchemaMapping(typeName = "User", field = "spexare")
    @RequiresAdmin
    public @Nullable SpexareDto retrieveSpexare(final UserDto dto) {
        return service.findSpexareByUser(dto.getId())
                .orElse(null);
    }

    @MutationMapping("userSpexareAdd")
    @RequiresAdmin
    public void addSpexare(@Argument final Long userId, @Argument final Long id) {
        service.addSpexare(userId, id);
    }

    @MutationMapping("userSpexareRemove")
    @RequiresAdmin
    public void removeSpexare(@Argument final Long userId) {
        service.removeSpexare(userId);
    }

}
