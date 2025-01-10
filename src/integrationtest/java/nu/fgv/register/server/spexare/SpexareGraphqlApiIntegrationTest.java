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

package nu.fgv.register.server.spexare;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventRepository;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import nu.fgv.register.server.util.randomizer.SocialSecurityNumberRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.inClass;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class SpexareGraphqlApiIntegrationTest extends AbstractGraphqlIntegrationTest {

    private final EasyRandom random;
    private final SpexareRepository repository;
    private final EventRepository eventRepository;

    @Value("${spring.jpa.properties.hibernate.search.backend.directory.root")
    private String indexDataLocation;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public SpexareGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
                                            final AclCache aclCache,
                                            final Keycloak keycloakAdminClient,
                                            final String keycloakClientId,
                                            final PermissionService permissionService,
                                            final ObjectMapper objectMapper,
                                            final SpexareRepository repository,
                                            final EventRepository eventRepository) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.repository = repository;
        this.eventRepository = eventRepository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

        parameters
                .randomize(
                        named("socialSecurityNumber"), new SocialSecurityNumberRandomizer()
                )
                .excludeField(named("partner").and(ofType(Spexare.class)).and(inClass(Spexare.class)))
                .excludeField(named("user").and(ofType(User.class)).and(inClass(Spexare.class)))
                .excludeField(named("activities").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("tags").and(ofType(Set.class)).and(inClass(Spexare.class)))
                .excludeField(named("addresses").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("memberships").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("consents").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("toggles").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("tags").and(ofType(List.class)).and(inClass(Spexare.class)))
                .randomizationDepth(1);
        random = new EasyRandom(parameters);
    }

    @BeforeAll
    public static void beforeClass() {
        basePath = SpexareApi.class.getAnnotation(RequestMapping.class).value()[0];
    }

    @BeforeEach
    void setUp() throws IOException {
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

        jdbcClient.sql("SELECT id FROM spexare WHERE partner_id IS NOT NULL")
                .query()
                .listOfRows()
                .forEach(row ->
                        jdbcClient
                                .sql("UPDATE spexare SET partner_id = NULL WHERE id = :id")
                                .param("id", row.get("id"))
                                .update()
                );
        JdbcTestUtils.deleteFromTables(jdbcClient, "spexare", "event");
        Files.deleteIfExists(Path.of(indexDataLocation, "spexare"));
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
                    .documentName("spexare/spexarePaged")
                    .execute()
                    .errors()
                    .verify()
                    .path("spexarePaged.edges")
                    .entityList(SpexareDto.class)
                    .hasSize(0);
        }

        @Test
        void should_return_one() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexarePaged")
                    .execute()
                    .errors()
                    .verify()
                    .path("spexarePaged.edges")
                    .entityList(SpexareDto.class)
                    .hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            IntStream.range(0, size).forEach(i -> {
                final var spexare = persistSpexare(randomizeSpexare());
                grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            });

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexarePaged")
                    .variable("first", size)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexarePaged.edges")
                    .entityList(SpexareDto.class)
                    .hasSize(size);
        }

    }

    @Nested
    @DisplayName("Retrieve paged with filtering")
    class RetrieveWithFilteringPagedTests {

        @Test
        void should_return_zero() {
            persistSpexare(randomizeSpexare());

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexarePaged")
                    .variable("filter", Spexare_.FIRST_NAME + ":whatever")
                    .execute()
                    .errors()
                    .verify()
                    .path("spexarePaged.edges")
                    .entityList(SpexareDto.class)
                    .hasSize(0);
        }

        @Test
        void should_return_one() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexarePaged")
                    .variable("filter", Spexare_.FIRST_NAME + ":" + spexare.getFirstName())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexarePaged.edges")
                    .entityList(SpexareDto.class)
                    .hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            IntStream.range(0, size).forEach(i -> {
                final var spexare = randomizeSpexare();
                if (i % 2 == 0) {
                    spexare.setFirstName("whatever");
                }
                final var spexare0 = persistSpexare(spexare);
                grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare0.getId()));
            });

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexarePaged")
                    .variable("filter", Spexare_.FIRST_NAME + ":whatever")
                    .variable("first", size)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexarePaged.edges")
                    .entityList(SpexareDto.class)
                    .hasSize(size / 2);
        }

    }

    @Nested
    @DisplayName("Search paged")
    class SearchPagedTests {

        @Test
        void should_return_zero() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexareSearchPaged")
                    .variable("q", "whatever")
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareSearchPaged.edges")
                    .entityList(SpexareDto.class)
                    .hasSize(0);
        }

        @Test
        @Disabled
        void should_return_one() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexareSearchPaged")
                    .variable("q", spexare.getNickName())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareSearchPaged.edges")
                    .entityList(SpexareDto.class)
                    .hasSize(1);
        }

        @Test
        @Disabled
        void should_return_many() {
            final int size = 42;
            IntStream.range(0, size).forEach(i -> {
                final var spexare = persistSpexare(randomizeSpexare());
                spexare.setFirstName("firstName");
                repository.save(spexare);
                grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            });

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexareSearchPaged")
                    .variable("q", "firstName")
                    .variable("limit", size)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareSearchPaged.edges")
                    .entityList(SpexareDto.class)
                    .hasSize(size);
        }

        @Test
        @Disabled
        void should_return_zero_if_not_published_and_not_permitted() {
            final var spexare = persistSpexare(randomizeSpexare(false));
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexareSearchPaged")
                    .variable("q", spexare.getNickName())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareSearchPaged.edges")
                    .entityList(SpexareDto.class)
                    .hasSize(0);
        }

        @Test
        @Disabled
        void should_return_one_if_not_published_and_permitted() {
            final var spexare = persistSpexare(randomizeSpexare(false));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexareSearchPaged")
                    .variable("q", spexare.getNickName())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareSearchPaged.edges")
                    .entityList(SpexareDto.class)
                    .hasSize(1);
        }
    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create() {
            final SpexareCreateDto dto = random.nextObject(SpexareCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexareCreate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareCreate", result -> result
                            .path("firstName").entity(String.class).isEqualTo(dto.getFirstName())
                            .path("lastName").entity(String.class).isEqualTo(dto.getLastName())
                            .path("nickName").entity(String.class).isEqualTo(dto.getNickName())
                    );

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_BAD_REQUEST_when_invalid_input() {
            final SpexareCreateDto dto = random.nextObject(SpexareCreateDto.class);
            dto.setLastName("");

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexareCreate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> Objects.requireNonNull(error.getMessage()).contains("lastName"))
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.BAD_REQUEST.toString()))
                    )
                    .path("spexareCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted() {
            final SpexareCreateDto dto = random.nextObject(SpexareCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexareCreate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexareCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        void should_return_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare", result -> result
                            .path("id").entity(Long.class).isEqualTo(spexare.getId())
                            .path("firstName").entity(String.class).isEqualTo(spexare.getFirstName())
                            .path("lastName").entity(String.class).isEqualTo(spexare.getLastName())
                            .path("nickName").entity(String.class).isEqualTo(spexare.getNickName())
                    );
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spexare")
                    .valueIsNull();
        }
    }

    @Nested
    @DisplayName("Update")
    class UpdateTests {

        @Test
        void should_update() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantAdministrationPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));

            final SpexareDto before = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare")
                    .entity(SpexareDto.class)
                    .get();

            final SpexareUpdateDto dto = SpexareUpdateDto.builder()
                    .id(before.getId())
                    .firstName(before.getFirstName() + "_")
                    .lastName(before.getLastName())
                    .nickName(before.getNickName())
                    .deceased(Boolean.FALSE)
                    .published(Boolean.TRUE)
                    .build();

            final SpexareDto updated = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexareUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareUpdate", result -> result
                            .path("firstName").entity(String.class).isEqualTo(dto.getFirstName())
                            .path("lastName").entity(String.class).isEqualTo(dto.getLastName())
                            .path("nickName").entity(String.class).isEqualTo(dto.getNickName())
                    )
                    .entity(SpexareDto.class)
                    .get();

            final SpexareDto after = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare")
                    .entity(SpexareDto.class)
                    .get();

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_BAD_REQUEST_when_invalid_input() {
            final SpexareUpdateDto dto = random.nextObject(SpexareUpdateDto.class);
            dto.setFirstName("");

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexareUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> Objects.requireNonNull(error.getMessage()).contains("firstName"))
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.BAD_REQUEST.toString()))
                    )
                    .path("spexareUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            final SpexareUpdateDto dto = random.nextObject(SpexareUpdateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexareUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spexareUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_permission() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));

            final SpexareDto before = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare")
                    .entity(SpexareDto.class)
                    .get();

            final SpexareUpdateDto dto = SpexareUpdateDto.builder()
                    .id(before.getId())
                    .firstName(before.getFirstName() + "_")
                    .lastName(before.getLastName())
                    .nickName(before.getNickName())
                    .deceased(Boolean.FALSE)
                    .published(Boolean.TRUE)
                    .build();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexareUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexareUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_role() {
            final SpexareUpdateDto dto = random.nextObject(SpexareUpdateDto.class);

            httpGraphQlTester
                    .mutate()
                    .build()
                    .documentName("spexare/spexareUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexareUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        void should_delete() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantDeletePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexareDelete")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexareDelete")
                    .variable("id", 123)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spexareDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_permission() {
            final var spexare = persistSpexare(randomizeSpexare());

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexareDelete")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexareDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORIDDEN_when_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .build()
                    .documentName("spexare/spexareDelete")
                    .variable("id", 123)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexareDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Image")
    class ImageTests {

        @Test
        void should_delete_image() throws Exception {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var image = Files.readAllBytes(Paths.get(ResourceUtils.getFile("classpath:test.png").getPath()));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(MediaType.IMAGE_PNG_VALUE)
                .body(image)
            .when()
                .put("/{spexareId}/image", spexare.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare", result -> result
                            .path("imageUrl").hasValue()
                    );

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexareImageDelete")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareImageDelete")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare", result -> result
                            .path("imageUrl").valueIsNull()
                    );

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_deleting_not_permitted_due_to_insufficient_permission() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexareImageDelete")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexareImageDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_deleting_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .build()
                    .documentName("spexare/spexareImageDelete")
                    .variable("id", 123L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexareImageDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Partner")
    class PartnerTests {

        @Test
        void should_return() {
            final var partner = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, partner.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, partner.getId()));
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            spexare.setPartner(partner);
            partner.setPartner(spexare);
            repository.save(spexare);
            repository.save(partner);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare.partner", result -> result
                            .path("id").entity(Long.class).isEqualTo(partner.getId())
                            .path("firstName").entity(String.class).isEqualTo(partner.getFirstName())
                            .path("lastName").entity(String.class).isEqualTo(partner.getLastName())
                            .path("nickName").entity(String.class).isEqualTo(partner.getNickName())
                    );
        }

        @Test
        void should_update() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var partner = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, partner.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, partner.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare.partner")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexarePartnerAdd")
                    .variable("spexareId", spexare.getId())
                    .variable("id", partner.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexarePartnerAdd")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare.partner", result -> result
                            .path("id").entity(Long.class).isEqualTo(partner.getId())
                            .path("firstName").entity(String.class).isEqualTo(partner.getFirstName())
                            .path("lastName").entity(String.class).isEqualTo(partner.getLastName())
                            .path("nickName").entity(String.class).isEqualTo(partner.getNickName())
                    );
        }

        @Test
        void should_return_NOT_FOUND_when_updating_and_spexare_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexarePartnerAdd")
                    .variable("spexareId", 1L)
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spexarePartnerAdd")
                    .valueIsNull();
        }

        @Test
        void should_return_NOT_FOUND_when_updating_and_partner_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexarePartnerAdd")
                    .variable("spexareId", spexare.getId())
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spexarePartnerAdd")
                    .valueIsNull();
        }

        @Test
        void should_delete() {
            final var partner = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, partner.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, partner.getId()));
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            spexare.setPartner(partner);
            partner.setPartner(spexare);
            repository.save(spexare);
            repository.save(partner);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare.partner", result -> result
                            .path("id").entity(Long.class).isEqualTo(partner.getId())
                            .path("firstName").entity(String.class).isEqualTo(partner.getFirstName())
                            .path("lastName").entity(String.class).isEqualTo(partner.getLastName())
                            .path("nickName").entity(String.class).isEqualTo(partner.getNickName())
                    );

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexarePartnerRemove")
                    .variable("spexareId", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexarePartnerRemove")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare.partner")
                    .valueIsNull();
        }

        @Test
        void should_return_when_removing_and_no_partner() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare.partner")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexarePartnerRemove")
                    .variable("spexareId", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexarePartnerRemove")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare.partner")
                    .valueIsNull();
        }

        @Test
        void should_return_NOT_FOUND_when_removing_and_spexare_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexarePartnerRemove")
                    .variable("spexareId", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spexarePartnerRemove")
                    .valueIsNull();
        }

    }

    @Nested
    @DisplayName("Events")
    class EventTests {

        @Test
        void should_return_found() {
            final var spexare = persistSpexare(randomizeSpexare());

            final List<EventDto> result = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexareEvents")
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareEvents")
                    .entityList(EventDto.class)
                    .hasSize(1)
                    .get();

            assertThat(eventRepository.count()).isEqualTo(1);
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getEvent()).isEqualTo(Event.EventType.CREATE.name());
            assertThat(result.getFirst().getSource()).isEqualTo(Event.SourceType.SPEXARE.name());
            assertThat(result.getFirst().getCreatedBy()).isEqualTo(spexare.getCreatedBy());
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexareEvents")
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexareEvents")
                    .valueIsNull();
        }
    }

    private Spexare randomizeSpexare() {
        return randomizeSpexare(true);
    }

    private Spexare randomizeSpexare(final boolean published) {
        final var spexare = random.nextObject(Spexare.class);

        spexare.setPublished(published);

        return spexare;
    }

    private Spexare persistSpexare(final Spexare spexare) {
        spexare.setId(null);

        return repository.save(spexare);
    }

}
