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

package nu.fgv.register.server.util.graphql;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.graphql.server.WebSocketGraphQlInterceptor;
import org.springframework.graphql.server.WebSocketGraphQlRequest;
import org.springframework.graphql.server.WebSocketSessionInfo;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.springframework.security.core.context.ReactiveSecurityContextHolder.withSecurityContext;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SecurityContextWebSocketGraphQlInterceptor implements WebSocketGraphQlInterceptor {

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    @Override
    public Mono<Object> handleConnectionInitialization(final WebSocketSessionInfo sessionInfo, final Map<String, Object> connectionInitPayload) {
        final Object header = connectionInitPayload.get("Authorization");

        if (header instanceof final String authToken && authToken.startsWith("Bearer ")) {
            final String token = authToken.substring(7);

            try {
                final Jwt jwt = jwtDecoder.decode(token);
                final Authentication authentication = jwtAuthenticationConverter.convert(jwt);
                final SecurityContext securityContext = new SecurityContextImpl(authentication);

                sessionInfo.getAttributes().put(SecurityContext.class.getName(), securityContext);

                return Mono.just((Object) connectionInitPayload)
                        .contextWrite(withSecurityContext(Mono.just(securityContext)));
            } catch (final Exception e) {
                return Mono.error(new AccessDeniedException("Access denied"));
            }
        }

        return Mono.just(connectionInitPayload);
    }

    @Override
    public Mono<WebGraphQlResponse> intercept(final WebGraphQlRequest request, final Chain chain) {
        if (request instanceof final WebSocketGraphQlRequest wsRequest) {
            final Object sessionAttribute = wsRequest.getSessionInfo().getAttributes().get(SecurityContext.class.getName());

            if (sessionAttribute instanceof final SecurityContext securityContext) {
                return chain.next(request)
                        .contextWrite(withSecurityContext(Mono.just(securityContext)));
            }
        }

        return chain.next(request);
    }

    @Override
    public void handleConnectionClosed(final WebSocketSessionInfo sessionInfo, final int statusCode, final Map<String, Object> connectionInitPayload) {
        sessionInfo.getAttributes().remove(SecurityContext.class.getName());
    }

}
