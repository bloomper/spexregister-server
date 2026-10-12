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

package nu.fgv.register.server.analytics;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.List;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class AnalyticsGraphqlApiIntegrationTest extends AbstractGraphqlIntegrationTest {

    private static final String TOTALS = """
            query {
                analytics {
                    totals {
                        spexCount
                        spexCountHistory { label count }
                        spexRevivalCount
                        spexRevivalCountHistory { label count }
                        taskCount
                        taskCountByCategory { label count }
                        userCountHistory { label count }
                        spexareCountHistory { label count }
                    }
                }
            }
            """;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public AnalyticsGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
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

        jdbcClient.sql("UPDATE spex SET parent_id = NULL").update();
        JdbcTestUtils.deleteFromTables(jdbcClient, "task_activity", "activity", "spexare", "task", "task_category", "spex", "spex_details");
    }

    @Test
    void should_return_spex_and_revivals_per_production_year() {
        jdbcClient.sql("INSERT INTO spex_details (id, title, created_by, created_at) VALUES (1, 'Bojan', 'system', NOW()), (2, 'Nero', 'system', NOW())").update();
        jdbcClient.sql("INSERT INTO spex (id, year, parent_id, details_id, created_by, created_at) VALUES (1, '1948', NULL, 1, 'system', NOW()), (2, '1990', NULL, 2, 'system', NOW())").update();
        jdbcClient.sql("INSERT INTO spex (id, year, parent_id, details_id, created_by, created_at) VALUES (3, '1990', 1, 1, 'system', NOW()), (4, '2003', 1, 1, 'system', NOW())").update();

        httpGraphQlTester
                .mutate()
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                .build()
                .document(TOTALS)
                .execute()
                .errors()
                .verify()
                .path("analytics.totals.spexCount").entity(Integer.class).isEqualTo(2)
                .path("analytics.totals.spexCountHistory[*].label").entityList(String.class).isEqualTo(List.of("1948", "1990"))
                .path("analytics.totals.spexCountHistory[*].count").entityList(Integer.class).isEqualTo(List.of(1, 1))
                .path("analytics.totals.spexRevivalCount").entity(Integer.class).isEqualTo(2)
                .path("analytics.totals.spexRevivalCountHistory[*].label").entityList(String.class).isEqualTo(List.of("1990", "2003"));
    }

    @Test
    void should_return_tasks_per_category_largest_first() {
        jdbcClient.sql("INSERT INTO task_category (id, name, created_by, created_at) VALUES (1, 'Orkester', 'system', NOW()), (2, 'Kommitté', 'system', NOW())").update();
        jdbcClient.sql("INSERT INTO task (id, name, category_id, created_by, created_at) VALUES (1, 'Trumpet', 1, 'system', NOW()), (2, 'Directeur', 2, 'system', NOW()), (3, 'Ekonomichef', 2, 'system', NOW()), (4, 'Övrigt', NULL, 'system', NOW())").update();

        httpGraphQlTester
                .mutate()
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                .build()
                .document(TOTALS)
                .execute()
                .errors()
                .verify()
                .path("analytics.totals.taskCount").entity(Integer.class).isEqualTo(4)
                .path("analytics.totals.taskCountByCategory[*].label").entityList(String.class).isEqualTo(List.of("Kommitté", "Orkester"))
                .path("analytics.totals.taskCountByCategory[*].count").entityList(Integer.class).isEqualTo(List.of(2, 1));
    }

    @Test
    void should_return_users_per_year_they_were_created() {
        final List<String> years = jdbcClient.sql("SELECT DISTINCT CAST(YEAR(created_at) AS CHAR) FROM user ORDER BY 1").query(String.class).list();

        httpGraphQlTester
                .mutate()
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                .build()
                .document(TOTALS)
                .execute()
                .errors()
                .verify()
                .path("analytics.totals.userCountHistory[*].label").entityList(String.class).isEqualTo(years);
    }
}
