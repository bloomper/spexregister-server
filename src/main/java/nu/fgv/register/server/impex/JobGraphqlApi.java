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

package nu.fgv.register.server.impex;

import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.impex.model.JobStatusDto;
import nu.fgv.register.server.util.security.RequiresAdminOrEditor;
import nu.fgv.register.server.util.security.SecurityUtil;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@RequiredArgsConstructor
@Controller
public class JobGraphqlApi {

    private final JobService jobService;
    private final JobProgressService progressService;

    @QueryMapping("jobStatus")
    @RequiresAdminOrEditor
    public Mono<JobStatusDto> status(@Argument final Long id) {
        return jobService.getJobExecution(id)
                .map(jobService::mapToResult);
    }

    @SubscriptionMapping("jobProgress")
    public Flux<JobStatusDto> progress(@Argument final Long id) {
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(context -> Mono.justOrEmpty(context.getAuthentication()))
                .filter(this::hasRequiredAdminOrEditor)
                .switchIfEmpty(Mono.error(new AccessDeniedException("Access denied")))
                .flatMapMany(auth -> jobService.getJobExecution(id)
                        .flatMapMany(execution -> progressService.getStream(id)
                                .map(dto -> {
                                    if (dto.getStatus().equals("COMPLETED") || dto.getStatus().equals("FAILED")) {
                                        return jobService.mapToResult(execution);
                                    }
                                    return dto;
                                })
                        ));
    }

    private boolean hasRequiredAdminOrEditor(final Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .anyMatch(role -> role.equals(SecurityUtil.ROLE_ADMIN) || role.equals(SecurityUtil.ROLE_EDITOR));
    }
}
