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

package nu.fgv.register.server.savedsearch;

import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.ApiVersionInserter;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class SavedSearchApiIntegrationTest extends AbstractIntegrationTest {

    private final SavedSearchRepository repository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public SavedSearchApiIntegrationTest(final JdbcClient jdbcClient,
                                         final AclCache aclCache,
                                         final Keycloak keycloakAdminClient,
                                         final String keycloakClientId,
                                         final PermissionService permissionService,
                                         final SavedSearchRepository repository,
                                         final ObjectMapper objectMapper) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.repository = repository;
    }

    @BeforeEach
    void setUp() {
        restTestClient = RestTestClient
                .bindToServer()
                .baseUrl("http://localhost:%s/api/saved-searches".formatted(localPort))
                .apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
                .build();

        JdbcTestUtils.deleteFromTables(jdbcClient, "saved_search");
    }

    private SavedSearchDto createAs(final String token, final String name, final String query) {
        return Objects.requireNonNull(
                restTestClient
                        .post()
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .apiVersion("1.0")
                        .body(SavedSearchCreateDto.builder().name(name).query(query).build())
                        .exchange()
                        .expectStatus().isCreated()
                        .expectBody(SavedSearchDto.class)
                        .returnResult()
                        .getResponseBody()
        );
    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create_and_return_201() {
            final var created = createAs(obtainUserAccessToken(), "Inactive members", "q=inactive");

            assertThat(created).extracting("name").isEqualTo("Inactive members");
            assertThat(created).extracting("query").isEqualTo("q=inactive");
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_409_when_the_name_is_already_used_by_the_same_user() {
            createAs(obtainUserAccessToken(), "Inactive members", "q=inactive");

            restTestClient
                    .post()
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(SavedSearchCreateDto.builder().name("Inactive members").query("q=other").build())
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        void should_allow_the_same_name_for_a_different_user() {
            createAs(obtainUserAccessToken(), "Inactive members", "q=inactive");
            createAs(obtainEditorAccessToken(), "Inactive members", "q=inactive");

            assertThat(repository.count()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {

        @Test
        void should_return_own() {
            createAs(obtainUserAccessToken(), "Inactive members", "q=inactive");

            restTestClient
                    .get()
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$._embedded.savedSearches.length()").isEqualTo(1);
        }

        @Test
        void should_not_return_another_users() {
            createAs(obtainUserAccessToken(), "Inactive members", "q=inactive");

            restTestClient
                    .get()
                    .header(HttpHeaders.AUTHORIZATION, obtainEditorAccessToken())
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$._embedded").doesNotExist();
        }
    }

    @Nested
    @DisplayName("Retrieve by id")
    class RetrieveByIdTests {

        @Test
        void should_return_own() {
            final var created = createAs(obtainUserAccessToken(), "Inactive members", "q=inactive");

            restTestClient
                    .get()
                    .uri("/{id}", created.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.name").isEqualTo("Inactive members");
        }

        @Test
        void should_return_404_for_another_users() {
            final var created = createAs(obtainUserAccessToken(), "Inactive members", "q=inactive");

            restTestClient
                    .get()
                    .uri("/{id}", created.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainEditorAccessToken())
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        void should_delete_own_and_return_204() {
            final var created = createAs(obtainUserAccessToken(), "Inactive members", "q=inactive");

            restTestClient
                    .delete()
                    .uri("/{id}", created.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_for_another_users_and_leave_it_alone() {
            final var created = createAs(obtainUserAccessToken(), "Inactive members", "q=inactive");

            restTestClient
                    .delete()
                    .uri("/{id}", created.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainEditorAccessToken())
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound();

            assertThat(repository.count()).isEqualTo(1);
        }
    }
}
