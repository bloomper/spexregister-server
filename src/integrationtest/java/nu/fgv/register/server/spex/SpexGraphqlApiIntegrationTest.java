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

package nu.fgv.register.server.spex;

import nu.fgv.register.server.spex.category.SpexCategory;
import nu.fgv.register.server.spex.category.SpexCategoryRepository;
import nu.fgv.register.server.util.AbstractAuditable;
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import nu.fgv.register.server.util.filter.FilterOperation;
import nu.fgv.register.server.util.randomizer.RandomizerSupport;
import nu.fgv.register.server.util.randomizer.YearRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.ApiVersionInserter;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.inClass;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class SpexGraphqlApiIntegrationTest extends AbstractGraphqlIntegrationTest {

    private final EasyRandom random;
    private final SpexRepository repository;
    private final SpexDetailsRepository detailsRepository;
    private final SpexCategoryRepository categoryRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public SpexGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
                                         final Keycloak keycloakAdminClient,
                                         final String keycloakClientId,
                                         final SpexRepository repository,
                                         final SpexDetailsRepository detailsRepository,
                                         final SpexCategoryRepository categoryRepository,
                                         final ObjectMapper objectMapper) {
        super(jdbcClient, keycloakAdminClient, keycloakClientId, objectMapper);
        this.repository = repository;
        this.detailsRepository = detailsRepository;
        this.categoryRepository = categoryRepository;

        final EasyRandomParameters parameters = RandomizerSupport.parameters();

        parameters
                .randomize(
                        named("year"), new YearRandomizer()
                )
                .randomize(
                        named("firstYear"), new YearRandomizer()
                )
                .excludeField(named("version").and(ofType(Long.class)).and(inClass(AbstractAuditable.class)))
                .randomizationDepth(1);
        random = new EasyRandom(parameters);
    }

    @BeforeEach
    void setUp() {
        restTestClient = RestTestClient
                .bindToServer()
                .baseUrl("http://localhost:%s/api/spex".formatted(localPort))
                .apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
                .build();

        httpGraphQlTester = HttpGraphQlTester.create(
                WebTestClient.bindToServer()
                        .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath))
                        .build()
        );

        jdbcClient.sql("SELECT id FROM spex WHERE parent_id IS NOT NULL")
                .query()
                .listOfRows()
                .forEach(row ->
                        jdbcClient
                                .sql("UPDATE spex SET parent_id = NULL WHERE id = :id")
                                .param("id", row.get("id"))
                                .update()
                );
        JdbcTestUtils.deleteFromTables(jdbcClient, "spex", "spex_details", "spex_category", "spex_audit", "spex_details_audit", "spex_category_audit");
    }

    @AfterEach
    void tearDown() {
    }

    private Spex randomizeSpex(@Nullable final SpexCategory category) {
        final var spex = random.nextObject(Spex.class);
        spex.setParent(null);
        final var details = random.nextObject(SpexDetails.class);
        details.setCategory(category);
        spex.setDetails(details);
        return spex;
    }

    private Spex randomizeRevival(final Spex parent) {
        final var revival = random.nextObject(Spex.class);
        revival.setParent(parent);
        revival.setDetails(parent.getDetails());
        return revival;
    }

    private SpexCategory randomizeSpexCategory() {
        return random.nextObject(SpexCategory.class);
    }

    private Spex persistSpex(final Spex spex) {
        spex.setId(null);
        spex.getDetails().setId(null);
        final var details = detailsRepository.save(spex.getDetails());
        spex.setDetails(details);
        return repository.save(spex);
    }

    private Spex persistRevival(final Spex spex) {
        spex.setId(null);

        return repository.save(spex);
    }

    private SpexCategory persistSpexCategory(final SpexCategory category) {
        category.setId(null);

        return categoryRepository.save(category);
    }

    @Nested
    @DisplayName("Retrieve paged")
    class RetrievePagedTests {

        @Test
        void should_return_zero() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexPaged")
                    .execute()
                    .errors()
                    .verify()
                    .path("spexPaged.edges")
                    .entityList(SpexDto.class)
                    .hasSize(0);
        }

        @Test
        void should_return_one() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            final var spex = persistSpex(randomizeSpex(category));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexPaged")
                    .execute()
                    .errors()
                    .verify()
                    .path("spexPaged.edges")
                    .entityList(SpexDto.class)
                    .hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            final var category = persistSpexCategory(randomizeSpexCategory());
            IntStream.range(0, size).forEach(i -> {
                final var spex = persistSpex(randomizeSpex(category));
                if (i % 2 == 0) {
                    final var revival = randomizeRevival(spex);
                    persistRevival(revival);
                }
            });

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexPaged")
                    .variable("first", size)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexPaged.edges")
                    .entityList(SpexDto.class)
                    .hasSize(size);
        }

    }

    @Nested
    @DisplayName("Retrieve paged backwards")
    class RetrievePagedBackwardsTests {

        @Test
        void should_return_last_page() {
            final var ids = persistPermittedSpex(25);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexPagedCursor")
                    .variable("last", 10)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexPaged.totalCount").entity(Integer.class).isEqualTo(25)
                    .path("spexPaged.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(false)
                    .path("spexPaged.pageInfo.hasPreviousPage").entity(Boolean.class).isEqualTo(true)
                    .path("spexPaged.edges[*].node.id").entityList(String.class).isEqualTo(asStrings(ids.subList(15, 25)));
        }

        @Test
        void should_return_everything_when_fewer_than_requested() {
            final var ids = persistPermittedSpex(5);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexPagedCursor")
                    .variable("last", 10)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexPaged.totalCount").entity(Integer.class).isEqualTo(5)
                    .path("spexPaged.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(false)
                    .path("spexPaged.pageInfo.hasPreviousPage").entity(Boolean.class).isEqualTo(false)
                    .path("spexPaged.edges[*].node.id").entityList(String.class).isEqualTo(asStrings(ids));
        }

        @Test
        void should_return_zero() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexPagedCursor")
                    .variable("last", 10)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexPaged.totalCount").entity(Integer.class).isEqualTo(0)
                    .path("spexPaged.edges").entityList(SpexDto.class).hasSize(0);
        }

        @Test
        void should_page_backwards_from_last_page() {
            final var ids = persistPermittedSpex(25);

            final String startCursor = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexPagedCursor")
                    .variable("last", 10)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexPaged.pageInfo.startCursor")
                    .entity(String.class)
                    .get();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexPagedCursor")
                    .variable("last", 10)
                    .variable("before", startCursor)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexPaged.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(true)
                    .path("spexPaged.pageInfo.hasPreviousPage").entity(Boolean.class).isEqualTo(true)
                    .path("spexPaged.edges[*].node.id").entityList(String.class).isEqualTo(asStrings(ids.subList(5, 15)));
        }

        @Test
        void should_round_trip_forwards_to_last_page() {
            final var ids = persistPermittedSpex(25);

            final String endCursor = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexPagedCursor")
                    .variable("first", 15)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexPaged.pageInfo.endCursor")
                    .entity(String.class)
                    .get();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexPagedCursor")
                    .variable("first", 10)
                    .variable("after", endCursor)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexPaged.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(false)
                    .path("spexPaged.edges[*].node.id").entityList(String.class).isEqualTo(asStrings(ids.subList(15, 25)));
        }

        @Test
        void should_only_count_and_return_filtered() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            final List<UUID> matching = new ArrayList<>();
            IntStream.range(0, 20).forEach(i -> {
                final var spex = randomizeSpex(category);
                spex.setYear(i % 2 == 0 ? "1996" : "1997");
                final var persisted = persistSpex(spex);
                if (i % 2 == 0) {
                    matching.add(persisted.getUuid());
                }
            });

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexPagedCursor")
                    .variable("last", 4)
                    .variable("filter", Spex_.YEAR + ":1996")
                    .execute()
                    .errors()
                    .verify()
                    .path("spexPaged.totalCount").entity(Integer.class).isEqualTo(10)
                    .path("spexPaged.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(false)
                    .path("spexPaged.edges[*].node.id").entityList(String.class).isEqualTo(asStrings(matching.subList(6, 10)));
        }

        private List<String> asStrings(final List<UUID> ids) {
            return ids.stream().map(String::valueOf).toList();
        }

        private List<UUID> persistPermittedSpex(final int size) {
            final var category = persistSpexCategory(randomizeSpexCategory());

            return IntStream.range(0, size)
                    .mapToObj(_ -> {
                        final var spex = persistSpex(randomizeSpex(category));
                        return spex.getUuid();
                    })
                    .toList();
        }

    }

    @Nested
    @DisplayName("Retrieve paged with filtering")
    class RetrievePagedWithFilteringTests {

        @Test
        void should_return_zero() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            final var spex = persistSpex(randomizeSpex(category));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexPaged")
                    .variable("filter", Spex_.YEAR + ":whatever")
                    .execute()
                    .errors()
                    .verify()
                    .path("spexPaged.edges")
                    .entityList(SpexDto.class)
                    .hasSize(0);
        }

        @Test
        void should_return_one() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            final var spex = persistSpex(randomizeSpex(category));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexPaged")
                    .variable("filter", Spex_.DETAILS + "." + SpexDetails_.TITLE + ":" + spex.getDetails().getTitle())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexPaged.edges")
                    .entityList(SpexDto.class)
                    .hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            final var category = persistSpexCategory(randomizeSpexCategory());
            IntStream.range(0, size).forEach(i -> {
                final var spex = randomizeSpex(category);
                if (i % 2 == 0) {
                    spex.setYear("1996");
                } else {
                    spex.setYear("1997");
                }
                final var spex0 = persistSpex(spex);
                if (i % 4 == 0) {
                    final var revival = randomizeRevival(spex0);
                    persistRevival(revival);
                }
            });

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexPaged")
                    .variable("first", size)
                    .variable("filter", "( " + Spex_.YEAR + ":1996 AND " + Spex_.PARENT + ":" + FilterOperation.NULL + " )")
                    .execute()
                    .errors()
                    .verify()
                    .path("spexPaged.edges")
                    .entityList(SpexDto.class)
                    .hasSize(size / 2);
        }

    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        void should_return_found() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            final var spex = persistSpex(randomizeSpex(category));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spex")
                    .variable("id", spex.getUuid())
                    .execute()
                    .errors()
                    .verify()
                    .path("spex", result -> result
                            .path("id").entity(UUID.class).isEqualTo(spex.getUuid())
                            .path("title").entity(String.class).isEqualTo(spex.getDetails().getTitle())
                            .path("year").entity(String.class).isEqualTo(spex.getYear())
                    );
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spex")
                    .variable("id", UUID.randomUUID())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spex")
                    .valueIsNull();
        }
    }

}
