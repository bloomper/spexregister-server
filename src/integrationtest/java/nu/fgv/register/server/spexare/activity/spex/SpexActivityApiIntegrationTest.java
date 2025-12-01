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

package nu.fgv.register.server.spexare.activity.spex;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.SpexDetails;
import nu.fgv.register.server.spex.SpexDetailsRepository;
import nu.fgv.register.server.spex.SpexRepository;
import nu.fgv.register.server.spex.category.SpexCategory;
import nu.fgv.register.server.spex.category.SpexCategoryRepository;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.activity.Activity;
import nu.fgv.register.server.spexare.activity.ActivityRepository;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.randomizer.SocialSecurityNumberRandomizer;
import nu.fgv.register.server.util.randomizer.YearRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Set;

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
class SpexActivityApiIntegrationTest extends AbstractIntegrationTest {

    private final EasyRandom random;
    private final SpexActivityRepository repository;
    private final ActivityRepository activityRepository;
    private final SpexareRepository spexareRepository;
    private final SpexRepository spexRepository;
    private final SpexDetailsRepository spexDetailsRepository;
    private final SpexCategoryRepository spexCategoryRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public SpexActivityApiIntegrationTest(final JdbcClient jdbcClient,
                                          final AclCache aclCache,
                                          final Keycloak keycloakAdminClient,
                                          final String keycloakClientId,
                                          final PermissionService permissionService,
                                          final ObjectMapper objectMapper,
                                          final SpexActivityRepository repository,
                                          final ActivityRepository activityRepository,
                                          final SpexareRepository spexareRepository,
                                          final SpexRepository spexRepository,
                                          final SpexDetailsRepository spexDetailsRepository,
                                          final SpexCategoryRepository spexCategoryRepository) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.repository = repository;
        this.activityRepository = activityRepository;
        this.spexareRepository = spexareRepository;
        this.spexRepository = spexRepository;
        this.spexDetailsRepository = spexDetailsRepository;
        this.spexCategoryRepository = spexCategoryRepository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

        parameters
                .randomize(
                        named("year"), new YearRandomizer()
                )
                .randomize(
                        named("firstYear"), new YearRandomizer()
                )
                .randomize(
                        named("socialSecurityNumber"), new SocialSecurityNumberRandomizer()
                )
                .excludeField(named("partner").and(ofType(Spexare.class)).and(inClass(Spexare.class)))
                .excludeField(named("user").and(ofType(User.class)).and(inClass(Spexare.class)))
                .excludeField(named("tags").and(ofType(Set.class)).and(inClass(Spexare.class)))
                .excludeField(named("activities").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("memberships").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("consents").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("toggles").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("tags").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("spexActivity").and(ofType(SpexActivity.class)).and(inClass(Activity.class)))
                .excludeField(named("taskActivities").and(ofType(Set.class)).and(inClass(Activity.class)))
                .randomizationDepth(1);
        random = new EasyRandom(parameters);
    }

    @BeforeAll
    public static void beforeClass() {
        basePath = SpexActivityApi.class.getAnnotation(RequestMapping.class).value()[0];
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

        JdbcTestUtils.deleteFromTables(jdbcClient, "spex_activity", "activity", "spexare", "spex", "spex_details", "spex_category", "event");
    }

    @AfterEach
    void tearDown() {
        RestAssured.reset();
    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {

        @Test
        void should_return_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            final SpexActivityDto result =
                given()
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .contentType(ContentType.JSON)
                    .pathParam("spexareId", spexare.getId())
                    .pathParam("activityId", activity.getId())
                .when()
                    .get()
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .extract().body().as(SpexActivityDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id")
                    .isEqualTo(spexActivity.getId());
        }

        @Test
        void should_return_404_when_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .get()
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_spexare_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
                .pathParam("activityId", activity.getId())
            .when()
                .get()
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_activity_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", 1L)
            .when()
                .get()
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_incorrect_spexare() {
            final var spexare1 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare1.getId()));
            final var spexare2 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare2.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare2));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .get()
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_incorrect_activity() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var activity1 = persistActivity(randomizeActivity(spexare));
            final var activity2 = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity2, spex));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity1.getId())
            .when()
                .get()
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }

    @Nested
    @DisplayName("Retrieve by id")
    class RetrieveByIdTests {
        @Test
        void should_return_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            final SpexActivityDto result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                        .pathParam("activityId", activity.getId())
                    .when()
                        .get("/{id}", spexActivity.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(SpexActivityDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id")
                    .isEqualTo(spexActivity.getId());
        }

        @Test
        void should_return_404_when_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .get("/{id}", 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_spexare_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
                .pathParam("activityId", activity.getId())
            .when()
                .get("/{id}", spexActivity.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_activity_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", 1L)
            .when()
                .get("/{id}", spexActivity.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_incorrect_spexare() {
            final var spexare1 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare1.getId()));
            final var spexare2 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare2.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare2));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .get("/{id}", spexActivity.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_incorrect_activity() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var activity1 = persistActivity(randomizeActivity(spexare));
            final var activity2 = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity2, spex));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity1.getId())
            .when()
                .get("/{id}", spexActivity.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create_and_return_201() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .post("/{spexId}", spex.getId())
            .then()
                .statusCode(HttpStatus.CREATED.value());
            //@formatter:on

            //@formatter:off
            final SpexActivityDto result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                        .pathParam("activityId", activity.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(SpexActivityDto.class);
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
        }

        @Test
        void should_return_404_when_creating_and_spexare_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
                .pathParam("activityId", activity.getId())
            .when()
                .post("/{spexId}", spex.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_creating_and_activity_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            persistActivity(randomizeActivity(spexare));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", 1L)
            .when()
                .post("/{spexId}", spex.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_creating_and_spex_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .post("/{spexId}", 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_409_when_creating_and_incorrect_spexare() {
            final var spexare1 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare1.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare1.getId()));
            final var spexare2 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare2.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare2.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare2));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .post("/{spexId}", spex.getId())
            .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_permission() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .post("/{spexId}", spex.getId())
            .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_401_when_not_permitted_due_to_insufficient_role() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .post("/{spexId}", spex.getId())
            .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Update")
    class UpdateTests {

        @Test
        void should_update_and_return_204() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex1 = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex1.getId()));
            final var spex2 = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex2.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex1));

            //@formatter:off
            final SpexActivityDto result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .put("/{id}/{spexId}", spexActivity.getId(), spex2.getId())
            .then()
                .statusCode(HttpStatus.OK.value())
                .extract().body().as(SpexActivityDto.class);
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id")
                    .isEqualTo(spexActivity.getId());
        }

        @Test
        void should_return_404_when_updating_and_spexare_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
                .pathParam("activityId", activity.getId())
            .when()
                .put("/{id}/{spexId}", spexActivity.getId(), spex.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_updating_and_activity_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", 1L)
            .when()
                .put("/{id}/{spexId}", spexActivity.getId(), spex.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_updating_and_spex_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .put("/{id}/{spexId}", spexActivity.getId(), 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_updating_and_incorrect_spexare() {
            final var spexare1 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare1.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare1.getId()));
            final var spexare2 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare2.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare2.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare2));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .put("/{id}/{spexId}", spexActivity.getId(), spex.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_updating_and_incorrect_activity() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity1 = persistActivity(randomizeActivity(spexare));
            final var activity2 = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity2, spex));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity1.getId())
            .when()
                .put("/{id}/{spexId}", spexActivity.getId(), spex.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_permission() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex1 = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex1.getId()));
            final var spex2 = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex2.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex1));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .put("/{id}/{spexId}", spexActivity.getId(), spex2.getId())
            .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_401_when_not_permitted_due_to_insufficient_role() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex1 = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex1.getId()));
            final var spex2 = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex2.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex1));

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .put("/{id}/{spexId}", spexActivity.getId(), spex2.getId())
            .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        @Disabled
        void should_delete_and_return_204() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .delete("/{id}", spexActivity.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            //@formatter:off
            final List<SpexActivityDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                        .pathParam("activityId", activity.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spex-activities", SpexActivityDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_deleting_non_existing_value() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .delete("/{id}", 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_deleting_and_spexare_not_found() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(null));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
                .pathParam("activityId", activity.getId())
            .when()
                .delete("/{id}", spexActivity.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_deleting_and_activity_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(null));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", 1L)
            .when()
                .delete("/{id}", spexActivity.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_deleting_and_incorrect_spexare() {
            final var spexare1 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare1.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare1.getId()));
            final var spexare2 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare2.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare2.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare2));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .delete("/{id}", spexActivity.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_deleting_and_incorrect_activity() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity1 = persistActivity(randomizeActivity(spexare));
            final var activity2 = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity2, spex));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity1.getId())
            .when()
                .delete("/{id}", spexActivity.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_permission() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .delete("/{id}", spexActivity.getId())
            .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_401_when_not_permitted_due_to_insufficient_role() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .delete("/{id}", spexActivity.getId())
            .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }
    }

    private SpexActivity randomizeSpexActivity(final Activity activity, final Spex spex) {
        final var spexActivity = random.nextObject(SpexActivity.class);
        spexActivity.setActivity(activity);
        spexActivity.setSpex(spex);
        return spexActivity;
    }

    private SpexActivity persistSpexActivity(final SpexActivity spexActivity) {
        spexActivity.setId(null);

        return repository.save(spexActivity);
    }

    private Activity randomizeActivity(@Nullable final Spexare spexare) {
        final var activity = random.nextObject(Activity.class);

        activity.setSpexare(spexare);

        return activity;
    }

    private Activity persistActivity(final Activity activity) {
        activity.setId(null);

        return activityRepository.save(activity);
    }

    private Spexare randomizeSpexare() {
        return random.nextObject(Spexare.class);
    }

    private Spexare persistSpexare(final Spexare spexare) {
        spexare.setId(null);

        return spexareRepository.save(spexare);
    }

    private Spex randomizeSpex(final SpexCategory category) {
        final var spex = random.nextObject(Spex.class);
        spex.setParent(null);
        final var details = random.nextObject(SpexDetails.class);
        details.setCategory(category);
        spex.setDetails(details);
        return spex;
    }

    private Spex persistSpex(final Spex spex) {
        spex.setId(null);
        spex.getDetails().setId(null);
        final var details = spexDetailsRepository.save(spex.getDetails());
        spex.setDetails(details);
        return spexRepository.save(spex);
    }

    private SpexCategory randomizeSpexCategory() {
        return random.nextObject(SpexCategory.class);
    }

    private SpexCategory persistSpexCategory(final SpexCategory category) {
        category.setId(null);

        return spexCategoryRepository.save(category);
    }

}
