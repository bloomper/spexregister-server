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

package nu.fgv.register.server.user.state;

import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import nu.fgv.register.server.util.randomizer.LabelsRandomizer;
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
import tools.jackson.databind.ObjectMapper;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.named;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class StateGraphqlApiIntegrationTest extends AbstractGraphqlIntegrationTest {

    private final EasyRandom random;
    private final StateRepository repository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public StateGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
                                          final AclCache aclCache,
                                          final Keycloak keycloakAdminClient,
                                          final String keycloakClientId,
                                          final PermissionService permissionService,
                                          final StateRepository repository,
                                          final ObjectMapper objectMapper) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.repository = repository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

        parameters
                .randomize(
                        named("labels"), new LabelsRandomizer()
                )
                .randomizationDepth(1);
        random = new EasyRandom(parameters);
    }

    @BeforeEach
    void setUp() {
        httpGraphQlTester = HttpGraphQlTester.create(
                WebTestClient.bindToServer()
                        .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath))
                        .build()
        );
    }

    @AfterEach
    void tearDown() {
        JdbcTestUtils.deleteFromTables(jdbcClient, "state");
    }

    @Nested
    @DisplayName("Retrieve all")
    class RetrieveAllTests {

        @Test
        void should_return_zero() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("user/state/states")
                    .execute()
                    .errors()
                    .verify()
                    .path("states")
                    .entityList(StateDto.class)
                    .hasSize(0);
        }

        @Test
        void should_return_one() {
            persistState(randomizeState());

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("user/state/states")
                    .execute()
                    .errors()
                    .verify()
                    .path("states")
                    .entityList(StateDto.class)
                    .hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            IntStream.range(0, size).forEach(i -> persistState(randomizeState()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("user/state/states")
                    .execute()
                    .errors()
                    .verify()
                    .path("states")
                    .entityList(StateDto.class)
                    .hasSize(size);
        }

    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        void should_return_found() {
            final var state = persistState(randomizeState());

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("user/state/state")
                    .variable("id", state.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("state", result -> result
                            .path("id").entity(String.class).isEqualTo(state.getId())
                            .path("label").entity(String.class).isEqualTo(state.getLabels().get("sv"))
                    );
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/state/state")
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("state")
                    .valueIsNull();
        }
    }

    private State randomizeState() {
        return random.nextObject(State.class);
    }

    private State persistState(final State state) {
        return repository.save(state);
    }

}
