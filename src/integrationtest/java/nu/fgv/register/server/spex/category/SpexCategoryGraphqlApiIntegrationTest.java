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

import com.fasterxml.jackson.core.type.TypeReference;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventRepository;
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import nu.fgv.register.server.util.randomizer.YearRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.named;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class SpexCategoryGraphqlApiIntegrationTest extends AbstractGraphqlIntegrationTest {

    private final EasyRandom random;
    private final SpexCategoryRepository repository;
    private final EventRepository eventRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public SpexCategoryGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
                                                 final AclCache aclCache,
                                                 final Keycloak keycloakAdminClient,
                                                 final String keycloakClientId,
                                                 final PermissionService permissionService,
                                                 final SpexCategoryRepository repository,
                                                 final EventRepository eventRepository) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService);
        this.repository = repository;
        this.eventRepository = eventRepository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

        parameters
                .randomize(
                        named("firstYear"), new YearRandomizer()
                );
        random = new EasyRandom(parameters);
    }

    @BeforeAll
    public static void beforeClass() {
        basePath = SpexCategoryApi.class.getAnnotation(RequestMapping.class).value()[0];
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = localPort;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        final RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder();
        requestSpecBuilder.setBasePath(basePath);
        RestAssured.requestSpecification = requestSpecBuilder.build();
        RestAssured.config = config()
                .encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))
                .logConfig(LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails());

        httpGraphQlTester = HttpGraphQlTester.builder(
                        WebTestClient.bindToServer()
                                .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath)))
                .build();

        JdbcTestUtils.deleteFromTables(jdbcClient, "spex_category", "event");
    }

    @AfterEach
    void tearDown() {
        RestAssured.reset();
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

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(MediaType.IMAGE_PNG_VALUE)
                .body(logo)
            .when()
                .put("/{spexCategoryId}/logo", category.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

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

    @Nested
    @DisplayName("Events")
    class EventTests {

        @Test
        void should_return_found() {
            final var category = persistSpexCategory(randomizeSpexCategory());

            final List<EventDto> result = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryEvents")
                    .execute()
                    .errors()
                    .verify()
                    .path("spexCategoryEvents")
                    .entityList(EventDto.class)
                    .hasSize(1)
                    .get();

            assertThat(eventRepository.count()).isEqualTo(1);
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getEvent()).isEqualTo(Event.EventType.CREATE.name());
            assertThat(result.getFirst().getSource()).isEqualTo(Event.SourceType.SPEX_CATEGORY.name());
            assertThat(result.getFirst().getCreatedBy()).isEqualTo(category.getCreatedBy());
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spex/category/spexCategoryEvents")
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexCategoryEvents")
                    .valueIsNull();
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
