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

package nu.fgv.register.server.spexare.tag;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.tag.TagDto;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.util.AbstractIntegrationTest;
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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.inClass;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Disabled
class TaggingApiIntegrationTest extends AbstractIntegrationTest {

    private static String basePath;
    private final EasyRandom random;
    @LocalServerPort
    private int localPort;

    private final TaggingRepository repository;
    private final SpexareRepository spexareRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public TaggingApiIntegrationTest(final JdbcClient jdbcClient,
                                     final AclCache aclCache,
                                     final Keycloak keycloakAdminClient,
                                     final String keycloakClientId,
                                     final PermissionService permissionService,
                                     final TaggingRepository repository,
                                     final SpexareRepository spexareRepository) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService);
        this.repository = repository;
        this.spexareRepository = spexareRepository;

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
        basePath = TaggingApi.class.getAnnotation(RequestMapping.class).value()[0];
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

        repository.deleteAll();
        spexareRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        RestAssured.reset();
    }

    @Nested
    @DisplayName("Retrieve paged")
    class RetrievePagedTests {

        @Test
        void should_return_404() {
            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId",1L)
            .when()
                .get()
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return_zero() {
            final var spexare = persistSpexare(randomizeSpexare());

            //@formatter:off
            final List<TagDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.tags", TagDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_one() {
            final var spexare = persistSpexare(randomizeSpexare());
            final var tag = persistTag(randomizeTag());
            spexare.setTags(Set.of(tag));
            persistSpexare(spexare);

            //@formatter:off
            final List<TagDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.tags", TagDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            final var spexare = persistSpexare(randomizeSpexare());
            final var taggings = new ArrayList<Tag>();
            IntStream.range(0, size).forEach(i -> {
                final var tag = persistTag(randomizeTag());
                taggings.add(tag);
            });
            spexare.setTags(Set.copyOf(taggings));
            persistSpexare(spexare);

            //@formatter:off
            final List<TagDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                        .queryParam("size", size)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.tags", TagDto.class);
            //@formatter:on

            assertThat(result).hasSize(size);
        }

    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create_and_return_201() {
            final var spexare = persistSpexare(randomizeSpexare());
            final var tag = persistTag(randomizeTag());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .post("/{id}", tag.getId())
            .then()
                .statusCode(HttpStatus.CREATED.value());
            //@formatter:on

            //@formatter:off
            final List<TagDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.tags", TagDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_409_when_creating_already_existing_value() {
            final var spexare = persistSpexare(randomizeSpexare());
            final var tag = persistTag(randomizeTag());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .post("/{id}", tag.getId())
            .then()
                .statusCode(HttpStatus.CREATED.value());
            //@formatter:on

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .post("/{id}", tag.getId())
            .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        }

        @Test
        void should_return_404_when_creating_and_spexare_not_found() {
            final var tag = persistTag(randomizeTag());

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
            .when()
                .post("/{id}", tag.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        void should_delete_and_return_204() {
            final var spexare = persistSpexare(randomizeSpexare());
            final var tag = persistTag(randomizeTag());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .post("/{id}", tag.getId())
            .then()
                .statusCode(HttpStatus.CREATED.value());
            //@formatter:on

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .delete("/{id}", tag.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            //@formatter:off
            final List<TagDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.tags", TagDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_422_when_deleting_non_existing_value() {
            final var spexare = persistSpexare(randomizeSpexare());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .delete("/{id}", 1L)
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_deleting_and_spexare_not_found() {
            final var tag = persistTag(randomizeTag());

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", -1L)
            .when()
                .delete("/{id}", tag.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

    }

    private Tag randomizeTag() {
        return random.nextObject(Tag.class);
    }

    private Tag persistTag(final Tag tag) {
        return repository.save(tag);
    }

    private Spexare randomizeSpexare() {
        return random.nextObject(Spexare.class);
    }

    private Spexare persistSpexare(final Spexare spexare) {
        return spexareRepository.save(spexare);
    }

}
