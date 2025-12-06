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

package nu.fgv.register.server.user.authority;

import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.HalEmbeddedResponse;
import nu.fgv.register.server.util.randomizer.LabelsRandomizer;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
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
import static org.jeasy.random.FieldPredicates.named;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class AuthorityApiIntegrationTest extends AbstractIntegrationTest {

    private final EasyRandom random;
    private final AuthorityRepository repository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public AuthorityApiIntegrationTest(final JdbcClient jdbcClient,
                                       final AclCache aclCache,
                                       final Keycloak keycloakAdminClient,
                                       final String keycloakClientId,
                                       final PermissionService permissionService,
                                       final AuthorityRepository repository,
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
        restTestClient = RestTestClient
                .bindToServer()
                .baseUrl("http://localhost:%s/api/users/authorities".formatted(localPort))
                .apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
                .build();
    }

    @AfterEach
    void tearDown() {
        JdbcTestUtils.deleteFromTables(jdbcClient, "authority");
    }

    @Nested
    @DisplayName("Retrieve all")
    class RetrieveAllTests {

        @Test
        void should_return_zero() {
            final List<AuthorityDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<AuthorityDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("authorities");

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_one() {
            persistAuthority(randomizeAuthority());

            final List<AuthorityDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<AuthorityDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("authorities");

            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            IntStream.range(0, size).forEach(i -> persistAuthority(randomizeAuthority()));

            final List<AuthorityDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<AuthorityDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("authorities");

            assertThat(result).hasSize(size + 3); // Including authorities created via Flyway
        }

    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        void should_return_found() {
            final var authority = persistAuthority(randomizeAuthority());

            final AuthorityDto result = Objects.requireNonNull(
                    restTestClient
                            .get()
                            .uri("/{id}", authority.getId())
                            .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .apiVersion("1.0")
                            .exchange()
                            .expectStatus().isOk()
                            .expectBody(AuthorityDto.class)
                            .returnResult()
                            .getResponseBody()
            );

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "label")
                    .contains(authority.getId(), authority.getLabels().get("sv"));
        }

        @Test
        void should_return_404_when_not_found() {
            final ProblemDetail result = Objects.requireNonNull(
                    restTestClient
                            .get()
                            .uri("/{id}", 1L)
                            .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .apiVersion("1.0")
                            .exchange()
                            .expectStatus().isNotFound()
                            .expectBody(ProblemDetail.class)
                            .returnResult()
                            .getResponseBody()
            );

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }

    private Authority randomizeAuthority() {
        return random.nextObject(Authority.class);
    }

    private Authority persistAuthority(final Authority authority) {
        return repository.save(authority);
    }

}