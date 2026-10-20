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

package nu.fgv.register.server.security;

import nu.fgv.register.server.spex.SpexCreateDto;
import nu.fgv.register.server.spex.SpexDto;
import nu.fgv.register.server.spex.SpexService;
import nu.fgv.register.server.task.TaskCreateDto;
import nu.fgv.register.server.task.TaskDto;
import nu.fgv.register.server.task.TaskService;
import nu.fgv.register.server.task.category.TaskCategoryCreateDto;
import nu.fgv.register.server.task.category.TaskCategoryDto;
import nu.fgv.register.server.task.category.TaskCategoryService;
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@code REVIVAL} role, held only by Nyuppsättning's service account, reads spexes, tasks and task categories
 * through the normal operations and nothing else.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
class RevivalRoleIntegrationTest extends AbstractGraphqlIntegrationTest {
    private static final String REVIVAL_CLIENT_ID = "fgv-revival-service";

    private final SpexService spexService;
    private final TaskService taskService;
    private final TaskCategoryService taskCategoryService;
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public RevivalRoleIntegrationTest(final JdbcClient jdbcClient,
                                      final Keycloak keycloakAdminClient,
                                      final String keycloakClientId,
                                      final ObjectMapper objectMapper,
                                      final SpexService spexService,
                                      final TaskService taskService,
                                      final TaskCategoryService taskCategoryService) {
        super(jdbcClient, keycloakAdminClient, keycloakClientId, objectMapper);
        this.spexService = spexService;
        this.taskService = taskService;
        this.taskCategoryService = taskCategoryService;
    }

    @BeforeEach
    void setUp() {
        httpGraphQlTester = HttpGraphQlTester.create(
                WebTestClient.bindToServer()
                        .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath))
                        .build()
        );

        jdbcClient.sql("UPDATE spex SET parent_id = NULL").update();
        jdbcClient.sql("UPDATE task SET category_id = NULL").update();
        jdbcClient.sql("DELETE FROM spex").update();
        jdbcClient.sql("DELETE FROM spex_details").update();
        jdbcClient.sql("DELETE FROM task").update();
        jdbcClient.sql("DELETE FROM task_category").update();
    }

    @Test
    void should_read_spexes_and_their_revivals() {
        final SpexDto original = asAdmin(() -> spexService.create(SpexCreateDto.builder().year("1956").title("Ali Baba").build()));
        final SpexDto revival = asAdmin(() -> spexService.addRevival(original.getId(), "2028"));

        revivalTester()
                .document("""
                        query { spexPaged(first: 500, filter: "parent:NULL", sort: ["year"], direction: ASC) {
                            edges { node { id title revivals { id year } } }
                            totalCount
                        } }""")
                .execute()
                .errors()
                .verify()
                .path("spexPaged.totalCount").entity(Integer.class).isEqualTo(1)
                .path("spexPaged.edges[0].node.title").entity(String.class).isEqualTo("Ali Baba")
                .path("spexPaged.edges[0].node.revivals[0].year").entity(String.class).isEqualTo("2028");

        revivalTester()
                .document("query spex($id: ID!) { spex(id: $id) { id title revival } }")
                .variable("id", original.getId())
                .execute()
                .errors()
                .verify()
                .path("spex.revival").entity(Boolean.class).isEqualTo(false);

        revivalTester()
                .document("query spexRevival($spexId: ID!, $id: ID!) { spexRevival(spexId: $spexId, id: $id) { id revival } }")
                .variable("spexId", original.getId())
                .variable("id", revival.getId())
                .execute()
                .errors()
                .verify()
                .path("spexRevival.revival").entity(Boolean.class).isEqualTo(true);
    }

    @Test
    void should_read_tasks_and_task_categories() {
        final TaskCategoryDto category = asAdmin(() -> taskCategoryService.create(TaskCategoryCreateDto.builder().name("Ensemble").actorPresent(true).build()));
        final TaskDto task = asAdmin(() -> taskService.create(TaskCreateDto.builder().name("Sångare").build()));
        asAdmin(() -> {
            taskService.addCategory(task.getId(), category.getId());
            return null;
        });

        revivalTester()
                .document("query { taskPaged(first: 100) { edges { node { id name category { id name } } } totalCount } }")
                .execute()
                .errors()
                .verify()
                .path("taskPaged.totalCount").entity(Integer.class).isEqualTo(1)
                .path("taskPaged.edges[0].node.category.name").entity(String.class).isEqualTo("Ensemble");

        revivalTester()
                .document("query task($id: ID!) { task(id: $id) { name } }")
                .variable("id", task.getId())
                .execute()
                .errors()
                .verify()
                .path("task.name").entity(String.class).isEqualTo("Sångare");

        revivalTester()
                .document("query { taskCategoryPaged(first: 100) { totalCount } }")
                .execute()
                .errors()
                .verify()
                .path("taskCategoryPaged.totalCount").entity(Integer.class).isEqualTo(1);

        revivalTester()
                .document("query taskCategory($id: ID!) { taskCategory(id: $id) { name } }")
                .variable("id", category.getId())
                .execute()
                .errors()
                .verify()
                .path("taskCategory.name").entity(String.class).isEqualTo("Ensemble");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "query { spexarePaged(first: 10) { totalCount } }",
            "query { spexCategoryPaged(first: 10) { totalCount } }",
            "query { tagPaged(first: 10) { totalCount } }",
            "query { newsPaged(first: 10) { totalCount } }",
            "query { userPaged(first: 10) { totalCount } }",
            "mutation { spexCreate(input: { year: \"1999\", title: \"Inte tillåten\" }) { id } }",
            "mutation { taskCreate(input: { name: \"Inte tillåten\" }) { id } }"
    })
    void should_refuse_everything_else(final String document) {
        revivalTester()
                .document(document)
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors)
                        .anyMatch(error -> ErrorType.FORBIDDEN.toString().equals(String.valueOf(error.getExtensions().get("classification")))));
    }

    @Test
    void should_refuse_the_category_of_a_spex() {
        asAdmin(() -> spexService.create(SpexCreateDto.builder().year("1956").title("Ali Baba").build()));

        revivalTester()
                .document("query { spexPaged(first: 10) { edges { node { id category { id } } } } }")
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors)
                        .anyMatch(error -> ErrorType.FORBIDDEN.toString().equals(String.valueOf(error.getExtensions().get("classification")))));
    }

    private HttpGraphQlTester revivalTester() {
        final String token = obtainServiceAccessToken();

        return httpGraphQlTester
                .mutate()
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, token))
                .build();
    }

    private String obtainServiceAccessToken() {
        final MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("grant_type", "client_credentials");
        formData.add("client_id", REVIVAL_CLIENT_ID);
        formData.add("client_secret", serviceClientSecret());

        @SuppressWarnings("unchecked") final Map<String, Object> response = WebClient.builder().build()
                .post()
                .uri(issuerUri + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return "Bearer " + response.get("access_token");
    }

    /**
     * The secret of {@code fgv-revival-service} in the imported test realm. The test's admin client may not read secrets.
     */
    private String serviceClientSecret() {
        try {
            final JsonNode realmJson = objectMapper.readTree(new ClassPathResource("keycloak/fgv.json").getContentAsString(StandardCharsets.UTF_8));

            for (final JsonNode client : realmJson.get("clients")) {
                if (REVIVAL_CLIENT_ID.equals(client.get("clientId").asString())) {
                    return client.get("secret").asString();
                }
            }
            throw new IllegalStateException("No client " + REVIVAL_CLIENT_ID + " in the test realm");
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private <T> T asAdmin(final Supplier<T> action) {
        final var previous = SecurityContextHolder.getContext().getAuthentication();

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin-creator", "ignored", "ROLE_spexregister_ADMIN"));
        try {
            return action.get();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }
}
