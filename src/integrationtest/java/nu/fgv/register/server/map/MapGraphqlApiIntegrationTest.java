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


package nu.fgv.register.server.map;

import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class MapGraphqlApiIntegrationTest extends AbstractGraphqlIntegrationTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public MapGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
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
        JdbcTestUtils.deleteFromTables(jdbcClient, "spexare_location", "spexare");

        final long published = spexare("Ada", true, false);
        final long unpublished = spexare("Bea", false, false);
        final long deceased = spexare("Cid", true, true);
        final long notFound = spexare("Dag", true, false);

        location(published, 57.69, 11.97);
        location(unpublished, 59.33, 18.07);
        location(deceased, 55.60, 13.00);
        jdbcClient.sql("INSERT INTO spexare_location (spexare_id, query, geocoded_at) VALUES (?, 'nowhere', NOW())")
                .param(notFound)
                .update();
    }

    private long spexare(final String firstName, final boolean published, final boolean deceased) {
        final String uuid = UUID.randomUUID().toString();

        jdbcClient.sql("""
                        INSERT INTO spexare (uuid, first_name, last_name, published, deceased, created_by, created_at)
                        VALUES (?, ?, 'Spexare', ?, ?, 'test', NOW())
                        """)
                .params(uuid, firstName, published, deceased)
                .update();

        return jdbcClient.sql("SELECT id FROM spexare WHERE uuid = ?").param(uuid).query(Long.class).single();
    }

    private void location(final long spexareId, final double latitude, final double longitude) {
        jdbcClient.sql("INSERT INTO spexare_location (spexare_id, query, latitude, longitude, geocoded_at) VALUES (?, 'q', ?, ?, NOW())")
                .params(spexareId, latitude, longitude)
                .update();
    }

    @Test
    void should_return_the_located_published_spexare_to_a_user() {
        httpGraphQlTester
                .mutate()
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                .build()
                .documentName("map/spexareLocations")
                .execute()
                .errors()
                .verify()
                .path("spexareLocations")
                .entityList(SpexareLocationDto.class)
                .satisfies(locations -> assertThat(locations)
                        .singleElement()
                        .satisfies(location -> {
                            assertThat(location.firstName()).isEqualTo("Ada");
                            assertThat(location.latitude()).isEqualTo(57.69);
                            assertThat(location.longitude()).isEqualTo(11.97);
                        }));
    }

    @Test
    void should_return_unpublished_spexare_to_an_editor() {
        httpGraphQlTester
                .mutate()
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainEditorAccessToken()))
                .build()
                .documentName("map/spexareLocations")
                .execute()
                .errors()
                .verify()
                .path("spexareLocations[*].firstName")
                .entityList(String.class)
                .containsExactly("Ada", "Bea");
    }

    @Test
    void should_refuse_without_a_token() {
        httpGraphQlTester
                .documentName("map/spexareLocations")
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors)
                        .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.UNAUTHORIZED.toString())
                                || error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString())));
    }
}
