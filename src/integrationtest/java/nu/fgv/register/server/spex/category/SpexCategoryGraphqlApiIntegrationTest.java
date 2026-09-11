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

package nu.fgv.register.server.spex.category;

import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.util.AbstractAuditable;
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import nu.fgv.register.server.util.randomizer.YearRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
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
class SpexCategoryGraphqlApiIntegrationTest extends AbstractGraphqlIntegrationTest {

    private final EasyRandom random;
    private final SpexCategoryRepository repository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public SpexCategoryGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
                                                 final AclCache aclCache,
                                                 final Keycloak keycloakAdminClient,
                                                 final String keycloakClientId,
                                                 final PermissionService permissionService,
                                                 final SpexCategoryRepository repository,
                                                 final ObjectMapper objectMapper) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.repository = repository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

        parameters
                .randomize(
                        named("firstYear"), new YearRandomizer()
                )
                .excludeField(named("version").and(ofType(Long.class)).and(inClass(AbstractAuditable.class)));
        random = new EasyRandom(parameters);
    }

    @BeforeEach
    void setUp() {
        restTestClient = RestTestClient
                .bindToServer()
                .baseUrl("http://localhost:%s/api/spex/categories".formatted(localPort))
                .apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
                .build();

        httpGraphQlTester = HttpGraphQlTester.create(
                WebTestClient.bindToServer()
                        .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath))
                        .build()
        );

        JdbcTestUtils.deleteFromTables(jdbcClient, "spex_category", "spex_category_audit");
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
                    .documentName("spex/category/spexCategoryPaged")
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategoryPaged.edges")
                    .entityList(SpexCategoryDto.class)
                    .hasSize(0);
        }

        @Test
        void should_return_one() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryPaged")
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategoryPaged.edges")
                    .entityList(SpexCategoryDto.class)
                    .hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            IntStream.range(0, size).forEach(i -> {
                final var category = persistSpexCategory(randomizeSpexCategory());
                grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            });

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryPaged")
                    .variable("first", size)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategoryPaged.edges")
                    .entityList(SpexCategoryDto.class)
                    .hasSize(size);
        }
    }

    @Nested
    @DisplayName("Retrieve paged backwards")
    class RetrievePagedBackwardsTests {

        @Test
        void should_return_last_page() {
            final var ids = persistPermittedSpexCategories(25);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryPagedCursor")
                    .variable("last", 10)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategoryPaged.totalCount").entity(Integer.class).isEqualTo(25)
                    .path("spexCategoryPaged.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(false)
                    .path("spexCategoryPaged.pageInfo.hasPreviousPage").entity(Boolean.class).isEqualTo(true)
                    .path("spexCategoryPaged.edges[*].node.id").entityList(String.class).isEqualTo(asStrings(ids.subList(15, 25)));
        }

        @Test
        void should_return_everything_when_fewer_than_requested() {
            final var ids = persistPermittedSpexCategories(5);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryPagedCursor")
                    .variable("last", 10)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategoryPaged.totalCount").entity(Integer.class).isEqualTo(5)
                    .path("spexCategoryPaged.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(false)
                    .path("spexCategoryPaged.pageInfo.hasPreviousPage").entity(Boolean.class).isEqualTo(false)
                    .path("spexCategoryPaged.edges[*].node.id").entityList(String.class).isEqualTo(asStrings(ids));
        }

        @Test
        void should_return_zero() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryPagedCursor")
                    .variable("last", 10)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategoryPaged.totalCount").entity(Integer.class).isEqualTo(0)
                    .path("spexCategoryPaged.edges").entityList(SpexCategoryDto.class).hasSize(0);
        }

        @Test
        void should_page_backwards_from_last_page() {
            final var ids = persistPermittedSpexCategories(25);

            final String startCursor = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryPagedCursor")
                    .variable("last", 10)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategoryPaged.pageInfo.startCursor")
                    .entity(String.class)
                    .get();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryPagedCursor")
                    .variable("last", 10)
                    .variable("before", startCursor)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategoryPaged.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(true)
                    .path("spexCategoryPaged.pageInfo.hasPreviousPage").entity(Boolean.class).isEqualTo(true)
                    .path("spexCategoryPaged.edges[*].node.id").entityList(String.class).isEqualTo(asStrings(ids.subList(5, 15)));
        }

        @Test
        void should_round_trip_forwards_to_last_page() {
            final var ids = persistPermittedSpexCategories(25);

            final String endCursor = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryPagedCursor")
                    .variable("first", 15)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategoryPaged.pageInfo.endCursor")
                    .entity(String.class)
                    .get();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryPagedCursor")
                    .variable("first", 10)
                    .variable("after", endCursor)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategoryPaged.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(false)
                    .path("spexCategoryPaged.edges[*].node.id").entityList(String.class).isEqualTo(asStrings(ids.subList(15, 25)));
        }

        private List<String> asStrings(final List<Long> ids) {
            return ids.stream().map(String::valueOf).toList();
        }

        private List<Long> persistPermittedSpexCategories(final int size) {
            return IntStream.range(0, size)
                    .mapToObj(_ -> {
                        final var category = persistSpexCategory(randomizeSpexCategory());
                        grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
                        return category.getId();
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

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryPaged")
                    .variable("filter", SpexCategory_.NAME + ":whatever")
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategoryPaged.edges")
                    .entityList(SpexCategoryDto.class)
                    .hasSize(0);
        }

        @Test
        void should_return_one() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryPaged")
                    .variable("filter", SpexCategory_.NAME + ":" + category.getName())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategoryPaged.edges")
                    .entityList(SpexCategoryDto.class)
                    .hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            IntStream.range(0, size).forEach(i -> {
                final var category = randomizeSpexCategory();
                if (i % 2 == 0) {
                    category.setName("whatever");
                }
                final var category0 = persistSpexCategory(category);
                grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category0.getId()));
            });

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryPaged")
                    .variable("first", size)
                    .variable("filter", SpexCategory_.NAME + ":whatever")
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategoryPaged.edges")
                    .entityList(SpexCategoryDto.class)
                    .hasSize(size / 2);
        }
    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create() {
            final SpexCategoryCreateDto dto = random.nextObject(SpexCategoryCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryCreate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategoryCreate", result -> result
                            .path("name").entity(String.class).isEqualTo(dto.name())
                            .path("firstYear").entity(String.class).isEqualTo(dto.firstYear())
                    );

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_BAD_REQUEST_when_invalid_input() {
            final SpexCategoryCreateDto randDto = random.nextObject(SpexCategoryCreateDto.class);
            final var dto = randDto.toBuilder()
                    .name("")
                    .build();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryCreate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> Objects.requireNonNull(error.getMessage()).contains("name"))
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.BAD_REQUEST.toString()))
                    )
                    .path("spexCategoryCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted() {
            final SpexCategoryCreateDto dto = random.nextObject(SpexCategoryCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryCreate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexCategoryCreate")
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

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategory")
                    .variable("id", category.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategory", result -> result
                            .path("id").entity(Long.class).isEqualTo(category.getId())
                            .path("name").entity(String.class).isEqualTo(category.getName())
                            .path("firstYear").entity(String.class).isEqualTo(category.getFirstYear())
                    );
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategory")
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spexCategory")
                    .valueIsNull();
        }
    }

    @Nested
    @DisplayName("Update")
    class UpdateTests {

        @Test
        void should_update() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));

            final SpexCategoryDto before = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategory")
                    .variable("id", category.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategory")
                    .entity(SpexCategoryDto.class)
                    .get();

            final SpexCategoryUpdateDto dto = SpexCategoryUpdateDto.builder()
                    .id(before.getId())
                    .name(before.getName() + "_")
                    .firstYear(before.getFirstYear())
                    .build();

            final SpexCategoryDto updated = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategoryUpdate", result -> result
                            .path("name").entity(String.class).isEqualTo(dto.name())
                            .path("firstYear").entity(String.class).isEqualTo(dto.firstYear())
                    )
                    .entity(SpexCategoryDto.class)
                    .get();

            final SpexCategoryDto after = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategory")
                    .variable("id", category.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategory")
                    .entity(SpexCategoryDto.class)
                    .get();

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_BAD_REQUEST_when_invalid_input() {
            final SpexCategoryUpdateDto randDto = random.nextObject(SpexCategoryUpdateDto.class);
            final var dto = randDto.toBuilder()
                    .name("")
                    .build();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> Objects.requireNonNull(error.getMessage()).contains("name"))
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.BAD_REQUEST.toString()))
                    )
                    .path("spexCategoryUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            final SpexCategoryUpdateDto dto = random.nextObject(SpexCategoryUpdateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spexCategoryUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_permission() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));

            final SpexCategoryDto before = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategory")
                    .variable("id", category.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategory")
                    .entity(SpexCategoryDto.class)
                    .get();

            final SpexCategoryUpdateDto dto = SpexCategoryUpdateDto.builder()
                    .id(before.getId())
                    .name(before.getName() + "_")
                    .firstYear(before.getFirstYear())
                    .build();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexCategoryUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_role() {
            final SpexCategoryUpdateDto dto = random.nextObject(SpexCategoryUpdateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexCategoryUpdate")
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
            grantDeletePermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryDelete")
                    .variable("id", category.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategoryDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryDelete")
                    .variable("id", 123L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spexCategoryDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_permission() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryDelete")
                    .variable("id", category.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexCategoryDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryDelete")
                    .variable("id", 123L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexCategoryDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Logo")
    class LogoTests {

        @Test
        void should_delete_logo() throws Exception {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var logo = Files.readAllBytes(Paths.get(ResourceUtils.getFile("classpath:test.png").getPath()));

            restTestClient
                    .put()
                    .uri("/{spexCategoryId}/logo", category.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                    .apiVersion("1.0")
                    .body(logo)
                    .exchange()
                    .expectStatus().isNoContent();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategory")
                    .variable("id", category.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategory", result -> result
                            .path("logoUrl").hasValue()
                    );

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryLogoDelete")
                    .variable("id", category.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategoryLogoDelete")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategory")
                    .variable("id", category.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategory", result -> result
                            .path("logoUrl").valueIsNull()
                    );
        }

        @Test
        void should_return_FORBIDDEN_when_deleting_not_permitted_due_to_insufficient_permission() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryLogoDelete")
                    .variable("id", category.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexCategoryLogoDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_deleting_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryLogoDelete")
                    .variable("id", 123L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexCategoryLogoDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    private SpexCategory randomizeSpexCategory() {
        return random.nextObject(SpexCategory.class);
    }

    private SpexCategory persistSpexCategory(final SpexCategory category) {
        category.setId(null);

        return repository.save(category);
    }

}
