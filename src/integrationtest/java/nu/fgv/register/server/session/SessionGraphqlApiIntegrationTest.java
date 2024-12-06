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

package nu.fgv.register.server.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class SessionGraphqlApiIntegrationTest extends AbstractGraphqlIntegrationTest {
    private final EasyRandom random;
    private final EventRepository eventRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public SessionGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
                                            final AclCache aclCache,
                                            final Keycloak keycloakAdminClient,
                                            final String keycloakClientId,
                                            final PermissionService permissionService,
                                            final ObjectMapper objectMapper,
                                            final EventRepository eventRepository) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.eventRepository = eventRepository;

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
    @DisplayName("Events")
    class EventTests {

        @Test
        void should_return_found() {
            final var event = persistEvent(randomizeEvent());

            final List<EventDto> result = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("session/sessionEvents")
                    .execute()
                    .errors()
                    .verify()
                    .path("sessionEvents")
                    .entityList(EventDto.class)
                    .hasSize(1)
                    .get();

            assertThat(eventRepository.count()).isEqualTo(1);
            assertThat(result.getFirst().getEvent()).isEqualTo(event.getEvent().name());
            assertThat(result.getFirst().getSource()).isEqualTo(Event.SourceType.SESSION.name());
            assertThat(result.getFirst().getCreatedBy()).isEqualTo(event.getCreatedBy());
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("session/sessionEvents")
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("sessionEvents")
                    .valueIsNull();
        }
    }

    private Event randomizeEvent() {
        final var event = random.nextObject(Event.class);
        event.setSource(Event.SourceType.SESSION);
        return event;
    }

    private Event persistEvent(final Event event) {
        event.setId(null);

        return eventRepository.save(event);
    }
}
