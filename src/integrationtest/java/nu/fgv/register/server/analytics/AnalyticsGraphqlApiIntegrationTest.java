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

import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
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

    private static final String SECTIONS = """
            query {
                analytics {
                    participation {
                        bySpexYear { key count facet }
                        topTask { label count }
                        byTaskCategory { label count }
                    }
                    lifecycle {
                        newcomersByYear { key count }
                        oneTimers
                        returning
                        neverActive
                    }
                    dataQuality {
                        total
                        issues { key count }
                    }
                    operations {
                        usersWithoutSpexare
                    }
                }
            }
            """;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public AnalyticsGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
                                              final Keycloak keycloakAdminClient,
                                              final String keycloakClientId,
                                              final ObjectMapper objectMapper) {
        super(jdbcClient, keycloakAdminClient, keycloakClientId, objectMapper);
    }

    @BeforeEach
    void setUp() {
        httpGraphQlTester = HttpGraphQlTester.create(
                WebTestClient.bindToServer()
                        .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath))
                        .build()
        );

        jdbcClient.sql("UPDATE spex SET parent_id = NULL").update();
        JdbcTestUtils.deleteFromTables(jdbcClient, "actor", "task_activity", "spex_activity", "activity", "spexare", "task", "task_category", "spex", "spex_details");
    }

    @Test
    void should_chart_activities_added_through_the_api() {
        jdbcClient.sql("INSERT INTO spex_details (uuid, id, title, created_by, created_at, version) VALUES (UUID(), 1, 'Bojan', 'system', NOW(), 0), (UUID(), 2, 'Nero', 'system', NOW(), 0)").update();
        jdbcClient.sql("INSERT INTO spex (uuid, id, year, parent_id, details_id, created_by, created_at, version) VALUES (UUID(), 1, '2010', NULL, 1, 'system', NOW(), 0), (UUID(), 2, '2015', NULL, 2, 'system', NOW(), 0)").update();
        jdbcClient.sql("INSERT INTO task_category (uuid, id, name, actor_present, created_by, created_at, version) VALUES (UUID(), 1, 'Orkester', FALSE, 'system', NOW(), 0)").update();
        jdbcClient.sql("INSERT INTO task (uuid, id, name, category_id, created_by, created_at, version) VALUES (UUID(), 1, 'Trumpet', 1, 'system', NOW(), 0), (UUID(), 2, 'Kontrabas', 1, 'system', NOW(), 0)").update();

        final String admin = obtainAdminAccessToken();
        final String ada = createSpexare(admin, "Ada", true);
        addSpex(admin, ada, 1);
        addTask(admin, ada, addSpex(admin, ada, 2), 1);
        final String bo = createSpexare(admin, "Bo", true);
        final String boActivity = addSpex(admin, bo, 2);
        removeTask(admin, bo, boActivity, addTask(admin, bo, boActivity, 2));
        createSpexare(admin, "Cy", true);
        addSpex(admin, createSpexare(admin, "Dag", false), 1);

        httpGraphQlTester
                .mutate()
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                .build()
                .document(SECTIONS)
                .execute()
                .errors()
                .verify()
                .path("analytics.participation.bySpexYear[*].key").entityList(String.class).isEqualTo(List.of("2010", "2015"))
                .path("analytics.participation.bySpexYear[*].count").entityList(Integer.class).isEqualTo(List.of(1, 2))
                .path("analytics.participation.bySpexYear[*].facet").entityList(String.class).isEqualTo(List.of("spexYears", "spexYears"))
                .path("analytics.participation.topTask[*].label").entityList(String.class).isEqualTo(List.of("Trumpet"))
                .path("analytics.participation.byTaskCategory[*].count").entityList(Integer.class).isEqualTo(List.of(1))
                .path("analytics.lifecycle.newcomersByYear[*].count").entityList(Integer.class).isEqualTo(List.of(1, 1))
                .path("analytics.lifecycle.oneTimers").entity(Integer.class).isEqualTo(1)
                .path("analytics.lifecycle.returning").entity(Integer.class).isEqualTo(1)
                .path("analytics.lifecycle.neverActive").entity(Integer.class).isEqualTo(1)
                .path("analytics.dataQuality").valueIsNull()
                .path("analytics.operations").valueIsNull();

        httpGraphQlTester
                .mutate()
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainEditorAccessToken()))
                .build()
                .document(SECTIONS)
                .execute()
                .errors()
                .verify()
                .path("analytics.dataQuality.total").entity(Integer.class).isEqualTo(4)
                .path("analytics.dataQuality.issues[?(@.key == 'noActivity')].count").entityList(Integer.class).isEqualTo(List.of(1));
    }

    @Test
    void should_return_spex_and_revivals_per_production_year() {
        jdbcClient.sql("INSERT INTO spex_details (uuid, id, title, created_by, created_at) VALUES (UUID(), 1, 'Bojan', 'system', NOW()), (UUID(), 2, 'Nero', 'system', NOW())").update();
        jdbcClient.sql("INSERT INTO spex (uuid, id, year, parent_id, details_id, created_by, created_at) VALUES (UUID(), 1, '1948', NULL, 1, 'system', NOW()), (UUID(), 2, '1990', NULL, 2, 'system', NOW())").update();
        jdbcClient.sql("INSERT INTO spex (uuid, id, year, parent_id, details_id, created_by, created_at) VALUES (UUID(), 3, '1990', 1, 1, 'system', NOW()), (UUID(), 4, '2003', 1, 1, 'system', NOW())").update();

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
        jdbcClient.sql("INSERT INTO task_category (uuid, id, name, created_by, created_at) VALUES (UUID(), 1, 'Orkester', 'system', NOW()), (UUID(), 2, 'Kommitté', 'system', NOW())").update();
        jdbcClient.sql("INSERT INTO task (uuid, id, name, category_id, created_by, created_at) VALUES (UUID(), 1, 'Trumpet', 1, 'system', NOW()), (UUID(), 2, 'Directeur', 2, 'system', NOW()), (UUID(), 3, 'Ekonomichef', 2, 'system', NOW()), (UUID(), 4, 'Övrigt', NULL, 'system', NOW())").update();

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

    @Test
    void should_show_demographics_to_everyone_and_operations_only_to_admins() {
        final String admin = obtainAdminAccessToken();

        createSpexare(admin, "Ada", true);
        createSpexare(admin, "Bo", true);

        final String query = """
                query {
                    analytics {
                        demographics {
                            byCountry { key count }
                            byStatus { key label count facet }
                            consentCompletion { key granted denied missing }
                        }
                        operations {
                            usersByState { key count }
                            usersWithoutSpexare
                            spexareWithoutUser
                            revisionsByMonth { key count }
                            revisionsBySource { key count }
                            topEditors { label count }
                        }
                    }
                }
                """;

        httpGraphQlTester
                .mutate()
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, admin))
                .build()
                .document(query)
                .execute()
                .errors()
                .verify()
                // The search index is shared with the other tests, so only the shape is checked
                .path("analytics.demographics.byStatus[*].count").entityList(Integer.class).satisfies(counts -> org.assertj.core.api.Assertions.assertThat(counts).allMatch(count -> count >= 2))
                .path("analytics.demographics.byStatus[*].facet").entityList(String.class).isEqualTo(List.of("deceased"))
                .path("analytics.operations.spexareWithoutUser").entity(Integer.class).isEqualTo(2)
                .path("analytics.operations.revisionsBySource").entityList(Object.class).satisfies(list -> org.assertj.core.api.Assertions.assertThat(list).isNotEmpty())
                .path("analytics.operations.topEditors").entityList(Object.class).satisfies(list -> org.assertj.core.api.Assertions.assertThat(list).isNotEmpty());

        httpGraphQlTester
                .mutate()
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainEditorAccessToken()))
                .build()
                .document(query)
                .execute()
                .errors()
                .verify()
                .path("analytics.demographics.byStatus").entityList(Object.class).hasSize(1)
                .path("analytics.operations").valueIsNull();
    }

    private String createSpexare(final String token, final String firstName, final boolean published) {
        return httpGraphQlTester
                .mutate()
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, token))
                .build()
                .documentName("spexare/spexareCreate")
                .variable("firstName", firstName)
                .variable("lastName", "Analytiksson")
                .variable("deceased", false)
                .variable("published", published)
                .execute()
                .errors()
                .verify()
                .path("spexareCreate.id").entity(String.class).get();
    }

    private String addTask(final String token, final String spexareId, final String activityId, final long taskId) {
        return httpGraphQlTester
                .mutate()
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, token))
                .build()
                .documentName("spexare/activity/taskActivity/taskActivityCreate")
                .variable("spexareId", spexareId)
                .variable("activityId", activityId)
                .variable("taskId", uuidOf("task", taskId))
                .execute()
                .errors()
                .verify()
                .path("taskActivityCreate.id").entity(String.class).get();
    }

    private void removeTask(final String token, final String spexareId, final String activityId, final String taskActivityId) {
        httpGraphQlTester
                .mutate()
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, token))
                .build()
                .documentName("spexare/activity/taskActivity/taskActivityDelete")
                .variable("spexareId", spexareId)
                .variable("activityId", activityId)
                .variable("id", taskActivityId)
                .execute()
                .errors()
                .verify();
    }

    private String uuidOf(final String table, final long id) {
        return jdbcClient.sql("SELECT uuid FROM %s WHERE id = ?".formatted(table)).param(id).query(String.class).single();
    }

    private String addSpex(final String token, final String spexareId, final long spexId) {
        final String activityId = httpGraphQlTester
                .mutate()
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, token))
                .build()
                .documentName("spexare/activity/activityCreate")
                .variable("spexareId", spexareId)
                .execute()
                .errors()
                .verify()
                .path("activityCreate.id").entity(String.class).get();

        httpGraphQlTester
                .mutate()
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, token))
                .build()
                .documentName("spexare/activity/spexActivity/spexActivityCreate")
                .variable("spexareId", spexareId)
                .variable("activityId", activityId)
                .variable("spexId", uuidOf("spex", spexId))
                .execute()
                .errors()
                .verify();

        return activityId;
    }
}
