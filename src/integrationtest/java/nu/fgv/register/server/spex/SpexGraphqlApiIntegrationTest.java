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

import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventRepository;
import nu.fgv.register.server.spex.category.SpexCategory;
import nu.fgv.register.server.spex.category.SpexCategoryRepository;
import nu.fgv.register.server.util.AbstractAuditable;
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import nu.fgv.register.server.util.filter.FilterOperation;
import nu.fgv.register.server.util.graphql.CustomErrorType;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.ApiVersionInserter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
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
    private final EventRepository eventRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public SpexGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
                                         final AclCache aclCache,
                                         final Keycloak keycloakAdminClient,
                                         final String keycloakClientId,
                                         final PermissionService permissionService,
                                         final SpexRepository repository,
                                         final SpexDetailsRepository detailsRepository,
                                         final SpexCategoryRepository categoryRepository,
                                         final EventRepository eventRepository,
                                         final ObjectMapper objectMapper) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.repository = repository;
        this.detailsRepository = detailsRepository;
        this.categoryRepository = categoryRepository;
        this.eventRepository = eventRepository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

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
        JdbcTestUtils.deleteFromTables(jdbcClient, "spex", "spex_details", "spex_category", "event", "spex_audit", "spex_details_audit", "spex_category_audit");
    }

    @AfterEach
    void tearDown() {
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
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));

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
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            IntStream.range(0, size).forEach(i -> {
                final var spex = persistSpex(randomizeSpex(category));
                grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
                if (i % 2 == 0) {
                    final var revival = randomizeRevival(spex);
                    persistRevival(revival);
                    grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, revival.getId()));
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
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final List<Long> matching = new ArrayList<>();
            IntStream.range(0, 20).forEach(i -> {
                final var spex = randomizeSpex(category);
                spex.setYear(i % 2 == 0 ? "1996" : "1997");
                final var persisted = persistSpex(spex);
                grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, persisted.getId()));
                if (i % 2 == 0) {
                    matching.add(persisted.getId());
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

        @Test
        void should_exclude_not_permitted_from_total_count() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final List<Long> permitted = new ArrayList<>();
            IntStream.range(0, 20).forEach(i -> {
                final var spex = persistSpex(randomizeSpex(category));
                if (i % 2 == 0) {
                    grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
                    permitted.add(spex.getId());
                } else {
                    grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
                }
            });

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexPagedCursor")
                    .variable("last", 4)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexPaged.totalCount").entity(Integer.class).isEqualTo(10)
                    .path("spexPaged.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(false)
                    .path("spexPaged.edges[*].node.id").entityList(String.class).isEqualTo(asStrings(permitted.subList(6, 10)));
        }

        @Test
        void should_return_last_page_of_revivals() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var parent = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, parent.getId()));
            final List<Long> revivals = IntStream.range(0, 12)
                    .mapToObj(_ -> {
                        final var revival = persistRevival(randomizeRevival(parent));
                        grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, revival.getId()));
                        return revival.getId();
                    })
                    .toList();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexRevivalsPagedCursor")
                    .variable("id", parent.getId())
                    .variable("last", 5)
                    .execute()
                    .errors()
                    .verify()
                    .path("spex.revivalsPaged.totalCount").entity(Integer.class).isEqualTo(12)
                    .path("spex.revivalsPaged.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(false)
                    .path("spex.revivalsPaged.pageInfo.hasPreviousPage").entity(Boolean.class).isEqualTo(true)
                    .path("spex.revivalsPaged.edges[*].node.id").entityList(String.class).isEqualTo(asStrings(revivals.subList(7, 12)));
        }

        private List<String> asStrings(final List<Long> ids) {
            return ids.stream().map(String::valueOf).toList();
        }

        private List<Long> persistPermittedSpex(final int size) {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));

            return IntStream.range(0, size)
                    .mapToObj(_ -> {
                        final var spex = persistSpex(randomizeSpex(category));
                        grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
                        return spex.getId();
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
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));

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
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));

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
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            IntStream.range(0, size).forEach(i -> {
                final var spex = randomizeSpex(category);
                if (i % 2 == 0) {
                    spex.setYear("1996");
                }
                final var spex0 = persistSpex(spex);
                grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex0.getId()));
                if (i % 4 == 0) {
                    final var revival = randomizeRevival(spex0);
                    persistRevival(revival);
                    grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, revival.getId()));
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
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create() {
            final SpexCreateDto dto = random.nextObject(SpexCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexCreate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCreate", result -> result
                            .path("title").entity(String.class).isEqualTo(dto.title())
                            .path("year").entity(String.class).isEqualTo(dto.year())
                    );

            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_return_BAD_REQUEST_when_invalid_input() {
            final SpexCreateDto randDto = random.nextObject(SpexCreateDto.class);
            final var dto = randDto.toBuilder()
                    .title("")
                    .build();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexCreate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> Objects.requireNonNull(error.getMessage()).contains("title"))
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.BAD_REQUEST.toString()))
                    )
                    .path("spexCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted() {
            final SpexCreateDto dto = random.nextObject(SpexCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexCreate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        void should_return_found() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spex")
                    .variable("id", spex.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spex", result -> result
                            .path("id").entity(Long.class).isEqualTo(spex.getId())
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
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spex")
                    .valueIsNull();
        }
    }

    @Nested
    @DisplayName("Update")
    class UpdateTests {

        @Test
        void should_update() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            final SpexDto before = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spex")
                    .variable("id", spex.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spex")
                    .entity(SpexDto.class)
                    .get();

            final SpexUpdateDto dto = SpexUpdateDto.builder()
                    .id(before.getId())
                    .title(before.getTitle() + "_")
                    .year(before.getYear())
                    .build();

            final SpexDto updated = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .verify()
                    .path("spexUpdate", result -> result
                            .path("title").entity(String.class).isEqualTo(dto.title())
                            .path("year").entity(String.class).isEqualTo(dto.year())
                    )
                    .entity(SpexDto.class)
                    .get();

            final SpexDto after = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spex")
                    .variable("id", spex.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spex")
                    .entity(SpexDto.class)
                    .get();

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_return_BAD_REQUEST_when_invalid_input() {
            final SpexUpdateDto randDto = random.nextObject(SpexUpdateDto.class);
            final var dto = randDto.toBuilder()
                    .title("")
                    .build();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> Objects.requireNonNull(error.getMessage()).contains("title"))
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.BAD_REQUEST.toString()))
                    )
                    .path("spexUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            final SpexUpdateDto dto = random.nextObject(SpexUpdateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spexUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_permission() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            final SpexDto before = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spex")
                    .variable("id", spex.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spex")
                    .entity(SpexDto.class)
                    .get();

            final SpexUpdateDto dto = SpexUpdateDto.builder()
                    .id(before.getId())
                    .title(before.getTitle() + "_")
                    .year(before.getYear())
                    .build();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_role() {
            final SpexUpdateDto dto = random.nextObject(SpexUpdateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        void should_delete() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            grantDeletePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexDelete")
                    .variable("id", spex.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexDelete")
                    .variable("id", 123L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spexDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_permission() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexDelete")
                    .variable("id", spex.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexDelete")
                    .variable("id", 123L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Poster")
    class PosterTests {

        @Test
        void should_delete_poster() throws Exception {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var poster = Files.readAllBytes(Paths.get(ResourceUtils.getFile("classpath:test.png").getPath()));

            restTestClient
                    .put()
                    .uri("/{spexId}/poster", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                    .apiVersion("1.0")
                    .body(poster)
                    .exchange()
                    .expectStatus().isNoContent();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spex")
                    .variable("id", spex.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spex", result -> result
                            .path("posterUrl").hasValue()
                    );

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexPosterDelete")
                    .variable("id", spex.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexPosterDelete")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spex")
                    .variable("id", spex.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spex", result -> result
                            .path("posterUrl").valueIsNull()
                    );

            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_deleting_not_permitted_due_to_insufficient_permission() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexPosterDelete")
                    .variable("id", spex.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexPosterDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_deleting_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexPosterDelete")
                    .variable("id", 123L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexPosterDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Revivals")
    class RevivalTests {

        @Test
        void should_return_parent_when_found() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var revival = persistRevival(randomizeRevival(spex));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, revival.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spex")
                    .variable("id", revival.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spex.parent", result -> result
                            .path("year").entity(String.class).isEqualTo(spex.getYear())
                    );
        }

        @Test
        void should_return_found() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var revival = persistRevival(randomizeRevival(spex));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, revival.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexRevival")
                    .variable("spexId", spex.getId())
                    .variable("id", revival.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexRevival", result -> result
                            .path("id").entity(Long.class).isEqualTo(revival.getId())
                            .path("title").entity(String.class).isEqualTo(revival.getDetails().getTitle())
                            .path("year").entity(String.class).isEqualTo(revival.getYear())
                    );
        }

        @Test
        void should_return_NOT_FOUND_when_spex_not_found() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var revival = persistRevival(randomizeRevival(spex));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, revival.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexRevival")
                    .variable("spexId", -1L)
                    .variable("id", revival.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spexRevival")
                    .valueIsNull();
        }

        @Test
        void should_return_NOT_FOUND_when_incorrect_spex() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex1 = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex1.getId()));
            final var spex2 = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex2.getId()));
            final var revival = persistRevival(randomizeRevival(spex2));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, revival.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexRevival")
                    .variable("spexId", spex1.getId())
                    .variable("id", revival.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spexRevival")
                    .valueIsNull();
        }

        @Test
        void should_return_zero() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spex")
                    .variable("id", spex.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spex.revivals")
                    .entityList(SpexDto.class)
                    .hasSize(0);
        }

        @Test
        void should_return_NOT_FOUND_when_non_existent_spex() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spex")
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spex")
                    .valueIsNull();
        }

        @Test
        void should_return_one() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var revival = persistRevival(randomizeRevival(spex));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, revival.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spex")
                    .variable("id", spex.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spex.revivals")
                    .entityList(SpexDto.class)
                    .hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            IntStream.range(0, size).forEach(i -> {
                final var revival = persistRevival(randomizeRevival(spex));
                grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, revival.getId()));
            });

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spex")
                    .variable("id", spex.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spex.revivals")
                    .entityList(SpexDto.class)
                    .hasSize(size);
        }

        @Test
        void should_return_zero_paged() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spex")
                    .variable("id", spex.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spex.revivalsPaged.edges")
                    .entityList(SpexDto.class)
                    .hasSize(0);
        }

        @Test
        void should_return_NOT_FOUND_when_non_existent_spex_paged() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spex")
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spex")
                    .valueIsNull();
        }

        @Test
        void should_return_one_paged() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var revival = persistRevival(randomizeRevival(spex));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, revival.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spex")
                    .variable("id", spex.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spex.revivalsPaged.edges")
                    .entityList(SpexDto.class)
                    .hasSize(1);
        }

        @Test
        void should_return_many_paged() {
            final int size = 42;
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            IntStream.range(0, size).forEach(i -> {
                final var revival = persistRevival(randomizeRevival(spex));
                grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, revival.getId()));
            });

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spex")
                    .variable("id", spex.getId())
                    .variable("first", size)
                    .execute()
                    .errors()
                    .verify()
                    .path("spex.revivalsPaged.edges")
                    .entityList(SpexDto.class)
                    .hasSize(size);
        }

        @Test
        void should_create() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexRevivalCreate")
                    .variable("spexId", spex.getId())
                    .variable("year", "2022")
                    .execute()
                    .errors()
                    .verify()
                    .path("spexRevivalCreate", result -> result
                            .path("year").entity(String.class).isEqualTo("2022")
                    );

            assertThat(repository.count()).isEqualTo(2);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_return_NOT_FOUND_when_adding_and_spex_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexRevivalCreate")
                    .variable("spexId", 1L)
                    .variable("year", "2022")
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spexRevivalCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
        }

        @Test
        void should_return_CONFLICT_when_adding_and_year_already_exists() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var revival = persistRevival(randomizeRevival(spex));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spex.class, revival.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexRevivalCreate")
                    .variable("spexId", spex.getId())
                    .variable("year", revival.getYear())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(CustomErrorType.CONFLICT.toString()))
                    )
                    .path("spexRevivalCreate")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(2);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_delete() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var revival = persistRevival(randomizeRevival(spex));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, revival.getId()));
            grantDeletePermissionToRoleAdmin(toObjectIdentity(Spex.class, revival.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexRevivalDelete")
                    .variable("spexId", spex.getId())
                    .variable("id", revival.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexRevivalDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_return_NOT_FOUND_when_removing_and_spex_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexRevivalDelete")
                    .variable("spexId", 1L)
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spexRevivalDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_removing_and_revival_not_found() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            grantDeletePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexRevivalDelete")
                    .variable("spexId", spex.getId())
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spexRevivalDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_adding_not_permitted_due_to_insufficient_permission() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexRevivalCreate")
                    .variable("spexId", spex.getId())
                    .variable("year", "2022")
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexRevivalCreate")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_adding_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexRevivalCreate")
                    .variable("spexId", 1L)
                    .variable("year", "2022")
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexRevivalCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_deleting_not_permitted_due_to_insufficient_permission() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var revival = persistRevival(randomizeRevival(spex));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, revival.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexRevivalDelete")
                    .variable("spexId", spex.getId())
                    .variable("id", revival.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexRevivalDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(2);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_deleting_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexRevivalDelete")
                    .variable("spexId", 1L)
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexRevivalDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Category")
    class CategoryTests {

        @Test
        void should_return_found() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spex")
                    .variable("id", spex.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spex.category", result -> result
                            .path("id").entity(Long.class).isEqualTo(category.getId())
                            .path("name").entity(String.class).isEqualTo(category.getName())
                            .path("firstYear").entity(String.class).isEqualTo(category.getFirstYear())
                    );

            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_add() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(null));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spex")
                    .variable("id", spex.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spex.category")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexCategoryAdd")
                    .variable("spexId", spex.getId())
                    .variable("id", category.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategoryAdd")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spex")
                    .variable("id", spex.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spex.category", result -> result
                            .path("id").entity(Long.class).isEqualTo(category.getId())
                            .path("name").entity(String.class).isEqualTo(category.getName())
                            .path("firstYear").entity(String.class).isEqualTo(category.getFirstYear())
                    );

            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_return_NOT_FOUND_when_adding_and_spex_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexCategoryAdd")
                    .variable("spexId", 1L)
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spexCategoryAdd")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_adding_and_category_not_found() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexCategoryAdd")
                    .variable("spexId", spex.getId())
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spexCategoryAdd")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_remove() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spex")
                    .variable("id", spex.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spex.category", result -> result
                            .path("id").entity(Long.class).isEqualTo(category.getId())
                            .path("name").entity(String.class).isEqualTo(category.getName())
                            .path("firstYear").entity(String.class).isEqualTo(category.getFirstYear())
                    );

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexCategoryRemove")
                    .variable("spexId", spex.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategoryRemove")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spex")
                    .variable("id", spex.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spex.category")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_return_ok_when_removing_and_no_category() {
            final var spex = persistSpex(randomizeSpex(null));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexCategoryRemove")
                    .variable("spexId", spex.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategoryRemove")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_return_NOT_FOUND_when_removing_and_spex_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexCategoryRemove")
                    .variable("spexId", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spexCategoryRemove")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_adding_not_permitted_due_to_insufficient_permission() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(null));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexCategoryAdd")
                    .variable("spexId", spex.getId())
                    .variable("id", category.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexCategoryAdd")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_adding_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexCategoryAdd")
                    .variable("spexId", 1L)
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexCategoryAdd")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_removing_not_permitted_due_to_insufficient_permission() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexCategoryRemove")
                    .variable("spexId", spex.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexCategoryRemove")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_removing_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/spexCategoryRemove")
                    .variable("spexId", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexCategoryRemove")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
        }

    }

    @Nested
    @DisplayName("Events")
    class EventTests {

        @Test
        void should_return_found() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            final List<EventDto> result = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/spexEvents")
                    .variable("sourceId", spex.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexEvents")
                    .entityList(EventDto.class)
                    .hasSize(1)
                    .get();

            assertThat(eventRepository.count()).isEqualTo(2);
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getEventType()).isEqualTo(Event.EventType.CREATE.name());
            assertThat(result.getFirst().getSourceType()).isEqualTo(Event.SourceType.SPEX.name());
            assertThat(result.getFirst().getCreatedBy()).isEqualTo(spex.getCreatedBy());
        }
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
}
