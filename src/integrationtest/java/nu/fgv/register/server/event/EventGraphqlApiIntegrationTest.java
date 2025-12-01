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
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class EventGraphqlApiIntegrationTest extends AbstractGraphqlIntegrationTest {
    private final EasyRandom random;
    private final EventRepository repository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public EventGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
                                          final AclCache aclCache,
                                          final Keycloak keycloakAdminClient,
                                          final String keycloakClientId,
                                          final PermissionService permissionService,
                                          final EventRepository repository) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService);
        this.repository = repository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

        random = new EasyRandom(parameters);
    }

    @BeforeEach
    void setUp() {
        httpGraphQlTester = HttpGraphQlTester.builder(
                        WebTestClient.bindToServer()
                                .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath)))
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
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("event/events")
                    .execute()
                    .errors()
                    .verify()
                    .path("events")
                    .entityList(EventDto.class)
                    .hasSize(0);

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_one() {
            persistEvent(randomizeEvent());

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("event/events")
                    .execute()
                    .errors()
                    .verify()
                    .path("events")
                    .entityList(EventDto.class)
                    .hasSize(1);

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            IntStream.range(0, size).forEach(i -> persistEvent(randomizeEvent()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("event/events")
                    .execute()
                    .errors()
                    .verify()
                    .path("events")
                    .entityList(EventDto.class)
                    .hasSize(size);

            assertThat(repository.count()).isEqualTo(size);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("event/events")
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("events")
                    .valueIsNull();
        }
    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        void should_return_found() {
            final var event = persistEvent(randomizeEvent());

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("event/event")
                    .variable("id", event.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("event", result -> result
                            .path("id").entity(Long.class).isEqualTo(event.getId())
                            .path("event").entity(Event.EventType.class).isEqualTo(event.getEvent())
                            .path("source").entity(Event.SourceType.class).isEqualTo(event.getSource())
                    );
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("event/event")
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("event")
                    .valueIsNull();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("event/event")
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("event")
                    .valueIsNull();
        }
    }

    private Event randomizeEvent() {
        return random.nextObject(Event.class);
    }

    private Event persistEvent(final Event event) {
        event.setId(null);

        return repository.save(event);
    }
}
