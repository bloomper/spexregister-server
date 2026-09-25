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

import graphql.ExecutionInput;
import graphql.ExecutionResultImpl;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.graphql.server.WebSocketGraphQlRequest;
import org.springframework.graphql.server.WebSocketSessionInfo;
import org.springframework.graphql.support.DefaultExecutionGraphQlResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.util.LinkedMultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class SecurityContextWebSocketGraphQlInterceptorTest {

    @Test
    void should_end_a_subscription_with_an_error_when_the_token_expires() {
        final Flux<?> stream = subscribe(Instant.now().plusSeconds(2), Flux.interval(Duration.ofMillis(100)));

        assertThrows(AccessDeniedException.class, () -> stream.blockLast(Duration.ofSeconds(5)));
    }

    @Test
    void should_let_a_subscription_that_ends_before_the_token_complete() {
        final Flux<?> stream = subscribe(Instant.now().plusSeconds(60), Flux.just(1, 2));

        assertThat(stream.collectList().block(Duration.ofSeconds(5)), is(equalTo(List.of(1, 2))));
    }

    @Test
    void should_refuse_a_request_once_the_token_has_expired() {
        final Map<String, Object> attributes = new HashMap<>();
        final SecurityContextWebSocketGraphQlInterceptor interceptor = connect(Instant.now().minusSeconds(1), attributes);

        assertThrows(AccessDeniedException.class, () -> interceptor.intercept(request(attributes), chainOf(Flux.just(1))).block());
    }

    private static Flux<?> subscribe(final Instant expiresAt, final Flux<?> events) {
        final Map<String, Object> attributes = new HashMap<>();
        final WebGraphQlResponse response = connect(expiresAt, attributes).intercept(request(attributes), chainOf(events)).block();

        return Flux.from((Publisher<?>) response.getData());
    }

    private static SecurityContextWebSocketGraphQlInterceptor connect(final Instant expiresAt, final Map<String, Object> attributes) {
        final JwtDecoder decoder = mock(JwtDecoder.class);

        when(decoder.decode(anyString())).thenReturn(Jwt.withTokenValue("token").header("alg", "none").subject("someone")
                .issuedAt(expiresAt.minusSeconds(300)).expiresAt(expiresAt).build());

        final SecurityContextWebSocketGraphQlInterceptor interceptor = new SecurityContextWebSocketGraphQlInterceptor(decoder, new JwtAuthenticationConverter());

        interceptor.handleConnectionInitialization(sessionOf(attributes), Map.of("Authorization", "Bearer token")).block();
        return interceptor;
    }

    private static WebSocketSessionInfo sessionOf(final Map<String, Object> attributes) {
        final WebSocketSessionInfo session = mock(WebSocketSessionInfo.class);

        when(session.getAttributes()).thenReturn(attributes);
        return session;
    }

    private static WebSocketGraphQlRequest request(final Map<String, Object> attributes) {
        return new WebSocketGraphQlRequest(URI.create("ws://localhost/api/graphql"), new HttpHeaders(), new LinkedMultiValueMap<>(), null,
                Map.of(), Map.of("query", "subscription { jobProgress(id: 1) { id } }"), "1", Locale.ENGLISH, sessionOf(attributes));
    }

    private static WebGraphQlInterceptor.Chain chainOf(final Flux<?> events) {
        return request -> Mono.just(new WebGraphQlResponse(new DefaultExecutionGraphQlResponse(
                ExecutionInput.newExecutionInput("subscription { jobProgress(id: 1) { id } }").build(),
                ExecutionResultImpl.newExecutionResult().data(events).build())));
    }
}
