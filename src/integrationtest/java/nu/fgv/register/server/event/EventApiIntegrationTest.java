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

package nu.fgv.register.server.event;

import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.HalEmbeddedResponse;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.ApiVersionInserter;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class EventApiIntegrationTest extends AbstractIntegrationTest {

    private final EasyRandom random;
    private final EventRepository repository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public EventApiIntegrationTest(final JdbcClient jdbcClient,
                                   final AclCache aclCache,
                                   final Keycloak keycloakAdminClient,
                                   final String keycloakClientId,
                                   final PermissionService permissionService,
                                   final EventRepository repository,
                                   final ObjectMapper objectMapper) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.repository = repository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

        random = new EasyRandom(parameters);
    }

    @BeforeEach
    void setUp() {
        restTestClient = RestTestClient
                .bindToServer()
                .baseUrl("http://localhost:%s/api/events".formatted(localPort))
                .apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
                .build();

        JdbcTestUtils.deleteFromTables(jdbcClient, "event");
    }

    @AfterEach
    void tearDown() {
    }

    @Nested
    @DisplayName("Retrieve all")
    class RetrieveAllTests {

        @Test
        void should_return_zero() {
            final List<EventDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("sourceType", Event.SourceType.NEWS.name())
                                            .build()
                                    )
                                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<EventDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("events");

            assertThat(repository.count()).isZero();
            assertThat(result).isEmpty();
        }

        @Test
        void should_return_one() {
            persistEvent(randomizeEvent());

            final List<EventDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("sourceType", Event.SourceType.NEWS.name())
                                            .build()
                                    )
                                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<EventDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("events");

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            IntStream.range(0, size).forEach(i -> persistEvent(randomizeEvent()));

            final List<EventDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("sourceType", Event.SourceType.NEWS.name())
                                            .build()
                                    )
                                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<EventDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("events");

            assertThat(repository.count()).isEqualTo(size);
            assertThat(result).hasSize(size);
        }

        @Test
        void should_return_403_when_not_permitted() {
            restTestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("sourceType", Event.SourceType.NEWS.name())
                            .build()
                    )
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden();
        }
    }

    private Event randomizeEvent() {
        final var event = random.nextObject(Event.class);

        event.setSourceType(Event.SourceType.NEWS);

        return event;
    }

    private Event persistEvent(final Event event) {
        event.setId(null);

        return repository.save(event);
    }

}
