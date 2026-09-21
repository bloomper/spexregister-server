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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.web.reactive.server.WebTestClient;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class AnalyticsGraphqlApiIntegrationTest extends AbstractGraphqlIntegrationTest {

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
    }

    @AfterEach
    void tearDown() {
    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {

        @Test
        void should_return_every_section_for_an_admin() {
            execute(obtainAdminAccessToken())
                    .path("analytics.totals.spexareCount").entity(Long.class).get();

            execute(obtainAdminAccessToken())
                    .path("analytics.dataQuality.issues").hasValue()
                    .path("analytics.operations.usersByState").hasValue();
        }

        /**
         * The whole point of a bucket is that it addresses its own drill-down, so an empty or
         * unnamed facet on a breakdown would silently produce dead chart bars.
         */
        @Test
        void should_name_the_drill_down_facet_on_every_participation_bucket() {
            execute(obtainAdminAccessToken())
                    .path("analytics.participation.bySpexYear[*].facet")
                    .entityList(String.class)
                    .satisfies(facets -> assertThat(facets).allMatch("spexYears"::equals));
        }

        /**
         * Only the selected sections are computed, so the home page's few fields do not drag the
         * gap counts and revision queries along behind them.
         */
        @Test
        void should_leave_unselected_sections_unbuilt() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("analytics/analytics-totals")
                    .execute()
                    .errors()
                    .verify()
                    .path("analytics.totals.spexareCount").entity(Long.class).get();
        }

        @Test
        void should_withhold_the_operations_section_from_a_plain_user() {
            execute(obtainUserAccessToken())
                    .path("analytics.operations").valueIsNull()
                    .path("analytics.dataQuality").valueIsNull()
                    .path("analytics.participation").hasValue();
        }

        @Test
        void should_give_an_editor_data_quality_but_not_operations() {
            execute(obtainEditorAccessToken())
                    .path("analytics.dataQuality").hasValue()
                    .path("analytics.operations").valueIsNull();
        }

        private GraphQlTester.Traversable execute(final String token) {
            return httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, token))
                    .build()
                    .documentName("analytics/analytics")
                    .execute()
                    .errors()
                    .verify();
        }
    }

}
