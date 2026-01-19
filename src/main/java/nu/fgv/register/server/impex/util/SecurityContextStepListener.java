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

package nu.fgv.register.server.impex.util;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
public class SecurityContextStepListener implements StepExecutionListener {

    @Override
    public void beforeStep(final StepExecution stepExecution) {
        final String requestor = stepExecution.getJobParameters().getString("requestor");
        final String authorities = stepExecution.getJobParameters().getString("authorities");

        if (requestor != null) {
            final List<SimpleGrantedAuthority> authorityList = (authorities == null || authorities.isEmpty()) ?
                    List.of() :
                    java.util.Arrays.stream(authorities.split(","))
                            .map(SimpleGrantedAuthority::new)
                            .toList();
            final Jwt jwt = Jwt.withTokenValue("mock-token")
                    .header("alg", "none")
                    .claim("sub", requestor)
                    .issuer("mock-issuer")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
            final JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, authorityList, requestor);

            SecurityContextHolder.getContext().setAuthentication(auth);
        }
    }

    @Override
    public @Nullable ExitStatus afterStep(final StepExecution stepExecution) {
        SecurityContextHolder.clearContext();
        return null;
    }
}
