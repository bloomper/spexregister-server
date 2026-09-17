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
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class SavedSearchGraphqlApiIntegrationTest extends AbstractGraphqlIntegrationTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public SavedSearchGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
                                                final AclCache aclCache,
                                                final Keycloak keycloakAdminClient,
                                                final String keycloakClientId,
                                                final PermissionService permissionService,
                                                final ObjectMapper objectMapper) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
    }

    @BeforeEach
    void setUp() {
        httpGraphQlTester = HttpGraphQlTester.create(
                WebTestClient.bindToServer()
                        .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath))
                        .build()
        );

        JdbcTestUtils.deleteFromTables(jdbcClient, "saved_search");
    }

    private SavedSearchDto createAs(final String token, final String name, final String query) {
        return httpGraphQlTester
                .mutate()
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, token))
                .build()
                .documentName("savedsearch/savedSearchCreate")
                .variable("name", name)
                .variable("query", query)
                .execute()
                .errors()
                .verify()
                .path("savedSearchCreate")
                .entity(SavedSearchDto.class)
                .get();
    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {

        @Test
        void should_return_zero() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("savedsearch/savedSearches")
                    .execute()
                    .errors()
                    .verify()
                    .path("savedSearches")
                    .entityList(SavedSearchDto.class)
                    .hasSize(0);
        }

        @Test
        void should_return_own() {
            createAs(obtainUserAccessToken(), "Inactive members", "q=inactive");

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("savedsearch/savedSearches")
                    .execute()
                    .errors()
                    .verify()
                    .path("savedSearches")
                    .entityList(SavedSearchDto.class)
                    .hasSize(1);
        }

        @Test
        void should_not_return_another_users() {
            createAs(obtainUserAccessToken(), "Inactive members", "q=inactive");

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainEditorAccessToken()))
                    .build()
                    .documentName("savedsearch/savedSearches")
                    .execute()
                    .errors()
                    .verify()
                    .path("savedSearches")
                    .entityList(SavedSearchDto.class)
                    .hasSize(0);
        }
    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create() {
            final var created = createAs(obtainUserAccessToken(), "Inactive members", "q=inactive");

            assertThat(created.getId()).isNotNull();
            assertThat(created.getName()).isEqualTo("Inactive members");
            assertThat(created.getQuery()).isEqualTo("q=inactive");
        }

        @Test
        void should_reject_a_duplicate_name_for_the_same_user() {
            createAs(obtainUserAccessToken(), "Inactive members", "q=inactive");

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("savedsearch/savedSearchCreate")
                    .variable("name", "Inactive members")
                    .variable("query", "q=other")
                    .execute()
                    .errors()
                    .satisfy(errors -> assertThat(errors).isNotEmpty());
        }

        @Test
        void should_allow_the_same_name_for_different_users() {
            createAs(obtainUserAccessToken(), "Inactive members", "q=inactive");
            final var editorsOwn = createAs(obtainEditorAccessToken(), "Inactive members", "q=inactive");

            assertThat(editorsOwn.getId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Retrieve by id")
    class RetrieveByIdTests {

        @Test
        void should_return_own() {
            final var created = createAs(obtainUserAccessToken(), "Inactive members", "q=inactive");

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("savedsearch/savedSearch")
                    .variable("id", created.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("savedSearch.name")
                    .entity(String.class)
                    .isEqualTo("Inactive members");
        }

        @Test
        void should_not_return_another_users() {
            final var created = createAs(obtainUserAccessToken(), "Inactive members", "q=inactive");

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainEditorAccessToken()))
                    .build()
                    .documentName("savedsearch/savedSearch")
                    .variable("id", created.getId())
                    .execute()
                    .errors()
                    .satisfy(errors -> assertThat(errors).isNotEmpty());
        }
    }

    @Nested
    @DisplayName("Update")
    class UpdateTests {

        @Test
        void should_update_own() {
            final var created = createAs(obtainUserAccessToken(), "Inactive members", "q=inactive");

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("savedsearch/savedSearchUpdate")
                    .variable("id", created.getId())
                    .variable("name", "Dormant members")
                    .variable("query", "q=dormant")
                    .execute()
                    .errors()
                    .verify()
                    .path("savedSearchUpdate.name")
                    .entity(String.class)
                    .isEqualTo("Dormant members");
        }

        @Test
        void should_not_update_another_users() {
            final var created = createAs(obtainUserAccessToken(), "Inactive members", "q=inactive");

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainEditorAccessToken()))
                    .build()
                    .documentName("savedsearch/savedSearchUpdate")
                    .variable("id", created.getId())
                    .variable("name", "Hijacked")
                    .variable("query", "q=hijacked")
                    .execute()
                    .errors()
                    .satisfy(errors -> assertThat(errors).isNotEmpty());
        }
    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        void should_delete_own() {
            final var created = createAs(obtainUserAccessToken(), "Inactive members", "q=inactive");

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("savedsearch/savedSearchDelete")
                    .variable("id", created.getId())
                    .execute()
                    .errors()
                    .verify();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("savedsearch/savedSearches")
                    .execute()
                    .errors()
                    .verify()
                    .path("savedSearches")
                    .entityList(SavedSearchDto.class)
                    .hasSize(0);
        }

        @Test
        void should_not_delete_another_users() {
            final var created = createAs(obtainUserAccessToken(), "Inactive members", "q=inactive");

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainEditorAccessToken()))
                    .build()
                    .documentName("savedsearch/savedSearchDelete")
                    .variable("id", created.getId())
                    .execute()
                    .errors()
                    .satisfy(errors -> assertThat(errors).isNotEmpty());

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("savedsearch/savedSearches")
                    .execute()
                    .errors()
                    .verify()
                    .path("savedSearches")
                    .entityList(SavedSearchDto.class)
                    .hasSize(1);
        }
    }
}
