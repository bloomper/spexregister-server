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

package nu.fgv.register.server.mcp;

import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class McpIntegrationTest extends AbstractIntegrationTest {

    private static final String MCP_PATH = "/api/mcp";
    private static final Set<String> EXPECTED_TOOLS = Set.of(
            "list_tasks", "get_task", "create_task", "update_task", "set_task_category", "delete_task",
            "list_task_categories", "get_task_category", "create_task_category", "update_task_category", "delete_task_category",
            "list_spex", "get_spex", "create_spex", "update_spex", "set_spex_category", "delete_spex",
            "add_spex_revival", "delete_spex_revival",
            "list_spex_categories", "get_spex_category", "create_spex_category", "update_spex_category", "delete_spex_category",
            "list_news", "get_news", "create_news", "update_news", "delete_news",
            "list_tags", "get_tag", "create_tag", "update_tag", "delete_tag",
            "get_user_statistics"
    );

    private final AtomicInteger requestId = new AtomicInteger();

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public McpIntegrationTest(final JdbcClient jdbcClient,
                              final AclCache aclCache,
                              final Keycloak keycloakAdminClient,
                              final String keycloakClientId,
                              final PermissionService permissionService,
                              final ObjectMapper objectMapper) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
    }

    @BeforeEach
    void setUp() {
        restTestClient = RestTestClient
                .bindToServer()
                .baseUrl("http://localhost:%s".formatted(localPort))
                .build();

        jdbcClient.sql("DELETE FROM spex WHERE parent_id IS NOT NULL").update();
        JdbcTestUtils.deleteFromTables(jdbcClient, "tag", "task", "task_category", "news", "spex", "spex_details", "spex_category",
                "tag_audit", "task_audit", "task_category_audit", "news_audit", "spex_audit", "spex_details_audit", "spex_category_audit",
                "revchanges", "revinfo");
    }

    private JsonNode rpc(final String token, final String method, final Map<String, Object> params) {
        final Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "id", requestId.incrementAndGet(),
                "method", method,
                "params", params
        );

        final byte[] body = Objects.requireNonNull(restTestClient
                .post()
                .uri(MCP_PATH)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(byte[].class)
                .returnResult()
                .getResponseBody());

        return objectMapper.readTree(body);
    }

    private JsonNode callTool(final String token, final String tool, final Map<String, Object> arguments) {
        return rpc(token, "tools/call", Map.of("name", tool, "arguments", arguments)).get("result");
    }

    private JsonNode toolContent(final JsonNode result) {
        assertThat(result.path("isError").asBoolean(false))
                .as("tool error: %s", result)
                .isFalse();

        return objectMapper.readTree(result.get("content").get(0).get("text").asString());
    }

    private @Nullable String latestRevisionSource() {
        return jdbcClient.sql("SELECT source FROM revinfo ORDER BY id DESC LIMIT 1")
                .query(String.class)
                .optional()
                .orElse(null);
    }

    @Nested
    @DisplayName("Security")
    class SecurityTests {

        @Test
        void should_return_401_with_resource_metadata_when_unauthenticated() {
            restTestClient
                    .post()
                    .uri(MCP_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("jsonrpc", "2.0", "id", 1, "method", "tools/list"))
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectHeader().value(HttpHeaders.WWW_AUTHENTICATE, value -> assertThat(value).contains("resource_metadata="));
        }

        @Test
        void should_expose_protected_resource_metadata() {
            final byte[] body = restTestClient
                    .get()
                    .uri("/.well-known/oauth-protected-resource" + MCP_PATH)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(byte[].class)
                    .returnResult()
                    .getResponseBody();

            final JsonNode metadata = objectMapper.readTree(body);

            assertThat(metadata.get("resource").asString()).endsWith(MCP_PATH);
            assertThat(metadata.get("authorization_servers").get(0).asString()).endsWith("/realms/fgv");
        }

        @Test
        void should_reject_token_issued_to_web_client() {
            restTestClient
                    .post()
                    .uri(MCP_PATH)
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                    .body(Map.of("jsonrpc", "2.0", "id", 1, "method", "tools/list"))
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        void should_reject_mcp_token_on_regular_api() {
            restTestClient
                    .get()
                    .uri("/api/tags")
                    .header(HttpHeaders.AUTHORIZATION, obtainMcpAccessToken(TEST_ADMIN))
                    .header("X-API-Version", "1.0")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
    }

    @Nested
    @DisplayName("Tools")
    class ToolTests {

        @Test
        void should_expose_only_allow_listed_tools() {
            final JsonNode tools = rpc(obtainMcpAccessToken(TEST_USER), "tools/list", Map.of()).get("result").get("tools");
            final Set<String> names = new HashSet<>();

            tools.forEach(tool -> names.add(tool.get("name").asString()));

            assertThat(names).containsExactlyInAnyOrderElementsOf(EXPECTED_TOOLS);
        }

        @Test
        void should_create_and_list_tag_and_audit_as_mcp() {
            final JsonNode created = toolContent(callTool(obtainMcpAccessToken(TEST_EDITOR), "create_tag", Map.of("name", "Musikal")));

            assertThat(created.get("name").asString()).isEqualTo("Musikal");
            assertThat(latestRevisionSource()).isEqualTo("MCP");

            final JsonNode listed = toolContent(callTool(obtainMcpAccessToken(TEST_USER), "list_tags", Map.of("filter", "name:Musikal")));

            assertThat(listed.get("totalElements").asLong()).isEqualTo(1);
            assertThat(listed.get("items").get(0).get("id").asLong()).isEqualTo(created.get("id").asLong());
        }

        @Test
        void should_return_tool_error_when_role_is_insufficient() {
            final JsonNode result = callTool(obtainMcpAccessToken(TEST_USER), "create_task", Map.of("name", "Regissör"));

            assertThat(result.get("isError").asBoolean()).isTrue();
            assertThat(JdbcTestUtils.countRowsInTable(jdbcClient, "task")).isZero();
        }

        @Test
        void should_return_tool_error_when_input_is_invalid() {
            final JsonNode result = callTool(obtainMcpAccessToken(TEST_ADMIN), "create_spex", Map.of("year", "86", "title", "Fel år"));

            assertThat(result.get("isError").asBoolean()).isTrue();
            assertThat(JdbcTestUtils.countRowsInTable(jdbcClient, "spex")).isZero();
        }

        @Test
        void should_partially_update_task_category() {
            final String token = obtainMcpAccessToken(TEST_ADMIN);
            final JsonNode category = toolContent(callTool(token, "create_task_category", Map.of("name", "Scen", "actorPresent", true)));

            final JsonNode updated = toolContent(callTool(token, "update_task_category", Map.of("id", category.get("id").asLong(), "name", "Scenen")));

            assertThat(updated.get("name").asString()).isEqualTo("Scenen");
            assertThat(updated.get("actorPresent").asBoolean()).isTrue();
        }

        @Test
        void should_create_task_in_category() {
            final String token = obtainMcpAccessToken(TEST_ADMIN);
            final JsonNode category = toolContent(callTool(token, "create_task_category", Map.of("name", "Orkester")));

            final JsonNode task = toolContent(callTool(token, "create_task", Map.of("name", "Trumpet", "categoryId", category.get("id").asLong())));

            assertThat(task.get("task").get("name").asString()).isEqualTo("Trumpet");
            assertThat(task.get("category").get("name").asString()).isEqualTo("Orkester");
        }

        @Test
        void should_list_spex_without_revivals_by_default() {
            final String adminToken = obtainMcpAccessToken(TEST_ADMIN);
            final JsonNode spex = toolContent(callTool(adminToken, "create_spex", Map.of("year", "1986", "title", "Napoleon")));
            final long spexId = spex.get("spex").get("id").asLong();

            toolContent(callTool(adminToken, "add_spex_revival", Map.of("spexId", spexId, "year", "2006")));

            final String userToken = obtainMcpAccessToken(TEST_USER);
            final JsonNode originals = toolContent(callTool(userToken, "list_spex", Map.of()));
            final JsonNode all = toolContent(callTool(userToken, "list_spex", Map.of("includeRevivals", true)));
            final JsonNode details = toolContent(callTool(userToken, "get_spex", Map.of("id", spexId)));

            assertThat(originals.get("totalElements").asLong()).isEqualTo(1);
            assertThat(all.get("totalElements").asLong()).isEqualTo(2);
            assertThat(details.get("revivals").get(0).get("year").asString()).isEqualTo("2006");
        }

        @Test
        void should_create_news_with_dates() {
            final JsonNode news = toolContent(callTool(obtainMcpAccessToken(TEST_EDITOR), "create_news", Map.of(
                    "subject", "Premiär",
                    "text", "Välkomna",
                    "visibleFrom", "2020-01-01"
            )));

            assertThat(news.get("visibleFrom").asString()).isEqualTo("2020-01-01");
            assertThat(news.get("published").asBoolean()).isTrue();
        }

        @Test
        void should_return_user_statistics_for_admin_only() {
            final JsonNode statistics = toolContent(callTool(obtainMcpAccessToken(TEST_ADMIN), "get_user_statistics", Map.of()));

            assertThat(statistics.has("usersByState")).isTrue();
            assertThat(statistics.has("pendingApproval")).isTrue();
            assertThat(callTool(obtainMcpAccessToken(TEST_EDITOR), "get_user_statistics", Map.of()).get("isError").asBoolean()).isTrue();
        }

        @Test
        void should_not_leak_audit_fields() {
            final JsonNode tag = toolContent(callTool(obtainMcpAccessToken(TEST_EDITOR), "create_tag", Map.of("name", "Revy")));

            assertThat(List.copyOf(tag.propertyNames())).containsExactlyInAnyOrder("id", "name");
        }
    }
}
