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

package nu.fgv.register.server.spexare;

import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.reactive.server.WebTestClient;
import tools.jackson.databind.ObjectMapper;

/**
 * An editor may publish and unpublish any spexare, not only the ones they created.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
class SpexarePublishByOtherEditorIntegrationTest extends AbstractGraphqlIntegrationTest {
    private static final String UPDATE = """
            mutation spexareUpdate($id: ID!, $published: Boolean!) {
                spexareUpdate(input: { id: $id, firstName: "Petra", lastName: "Publicera", deceased: false, published: $published }) {
                    id
                    published
                }
            }""";
    private static final String COUNT = "query { spexarePaged(first: 10) { totalCount } }";

    private final SpexareService service;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public SpexarePublishByOtherEditorIntegrationTest(final JdbcClient jdbcClient,
                                                      final Keycloak keycloakAdminClient,
                                                      final String keycloakClientId,
                                                      final ObjectMapper objectMapper,
                                                      final SpexareService service) {
        super(jdbcClient, keycloakAdminClient, keycloakClientId, objectMapper);
        this.service = service;
    }

    @BeforeEach
    void setUp() {
        httpGraphQlTester = HttpGraphQlTester.create(
                WebTestClient.bindToServer()
                        .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath))
                        .build()
        );
    }

    @Test
    void should_let_an_editor_unpublish_and_publish_a_spexare_created_by_another_editor() {
        final Long id = createdByAnotherEditor();

        update(id, false);
        countForUser(0);

        update(id, true);
        countForUser(1);
    }

    private Long createdByAnotherEditor() {
        final var previous = SecurityContextHolder.getContext().getAuthentication();

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("another-editor", "ignored", "ROLE_spexregister_EDITOR"));
        try {
            return service.create(SpexareCreateDto.builder()
                    .firstName("Petra")
                    .lastName("Publicera")
                    .deceased(false)
                    .published(true)
                    .build()).getId();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }

    private void update(final Long id, final boolean published) {
        httpGraphQlTester
                .mutate()
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainEditorAccessToken()))
                .build()
                .document(UPDATE)
                .variable("id", id)
                .variable("published", published)
                .execute()
                .errors()
                .verify()
                .path("spexareUpdate.published").entity(Boolean.class).isEqualTo(published);
    }

    private void countForUser(final int expected) {
        httpGraphQlTester
                .mutate()
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                .build()
                .document(COUNT)
                .execute()
                .errors()
                .verify()
                .path("spexarePaged.totalCount").entity(Integer.class).isEqualTo(expected);
    }
}
