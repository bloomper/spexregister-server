package nu.fgv.register.server.spexare.activity.spex;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.category.SpexCategory;
import nu.fgv.register.server.spex.category.SpexCategoryRepository;
import nu.fgv.register.server.spex.SpexDetails;
import nu.fgv.register.server.spex.SpexDetailsRepository;
import nu.fgv.register.server.spex.SpexRepository;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;

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

class SpexActivityApiIntegrationTest extends AbstractIntegrationTest {

    private static String basePath;
    private final EasyRandom random;
    @LocalServerPort
    private int localPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpexActivityRepository repository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private SpexareRepository spexareRepository;

    @Autowired
    private SpexRepository spexRepository;

    @Autowired
    private SpexDetailsRepository spexDetailsRepository;

    @Autowired
    private SpexCategoryRepository spexCategoryRepository;

    public SpexActivityApiIntegrationTest() {
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

        repository.deleteAll();
        spexareRepository.deleteAll();
        activityRepository.deleteAll();
        spexRepository.deleteAll();
        spexDetailsRepository.deleteAll();
        spexCategoryRepository.deleteAll();
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
                .pathParam("activityId",1L)
            .when()
                .get()
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return_zero() {
            var spexare = persistSpexare(randomizeSpexare());
            var activity = persistActivity(randomizeActivity(spexare));

            //@formatter:off
            final List<SpexActivityDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
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
        }

        @Test
        void should_return_one() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));
            var activity = persistActivity(randomizeActivity(spexare));
            persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            final List<SpexActivityDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
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

            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            int size = 42;
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));
            var activity = persistActivity(randomizeActivity(spexare));
            IntStream.range(0, size).forEach(i -> persistSpexActivity(randomizeSpexActivity(activity, spex)));

            //@formatter:off
            final List<SpexActivityDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                        .pathParam("activityId", activity.getId())
                        .queryParam("size", size)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spex-activities", SpexActivityDto.class);
            //@formatter:on

            assertThat(result).hasSize(size);
        }

        @Test
        void should_return_zero_when_incorrect_spexare() {
            var spexare1 = persistSpexare(randomizeSpexare());
            var spexare2 = persistSpexare(randomizeSpexare());
            var activity = persistActivity(randomizeActivity(spexare2));

            //@formatter:off
            final List<SpexActivityDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare1.getId())
                        .pathParam("activityId", activity.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spex-activities", SpexActivityDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        void should_return_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

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
            var spexare = persistSpexare(randomizeSpexare());
            var activity = persistActivity(randomizeActivity(spexare));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .get("/{id}", 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return_404_when_spexare_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
                .pathParam("activityId", activity.getId())
            .when()
                .get("/{id}", spexActivity.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return_404_when_activity_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", 1L)
            .when()
                .get("/{id}", spexActivity.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return_404_when_incorrect_spexare() {
            var spexare1 = persistSpexare(randomizeSpexare());
            var spexare2 = persistSpexare(randomizeSpexare());
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));
            var activity = persistActivity(randomizeActivity(spexare2));
            var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .get("/{id}", spexActivity.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return_404_when_incorrect_activity() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));
            var activity1 = persistActivity(randomizeActivity(spexare));
            var activity2 = persistActivity(randomizeActivity(spexare));
            var spexActivity = persistSpexActivity(randomizeSpexActivity(activity2, spex));

            //@formatter:off
            given()
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                    .pathParam("spexareId", spexare.getId())
                    .pathParam("activityId", activity1.getId())
                .when()
                    .get("/{id}", spexActivity.getId())
                .then()
                    .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }
    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create_and_return_201() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));
            var activity = persistActivity(randomizeActivity(spexare));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .post("/{spexId}", spex.getId())
            .then()
                .statusCode(HttpStatus.CREATED.value());
            //@formatter:on

            //@formatter:off
            final List<SpexActivityDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
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

            assertThat(result).hasSize(1);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_creating_and_spexare_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));
            var activity = persistActivity(randomizeActivity(spexare));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
                .pathParam("activityId", activity.getId())
            .when()
                .post("/{spexId}", spex.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_creating_and_activity_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));
            persistActivity(randomizeActivity(spexare));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", 1L)
            .when()
                .post("/{spexId}", spex.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return_404_when_creating_and_spex_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var activity = persistActivity(randomizeActivity(spexare));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .post("/{spexId}", 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_409_when_creating_and_incorrect_spexare() {
            var spexare1 = persistSpexare(randomizeSpexare());
            var spexare2 = persistSpexare(randomizeSpexare());
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));
            var activity = persistActivity(randomizeActivity(spexare2));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .post("/{spexId}", spex.getId())
            .then()
                .statusCode(HttpStatus.CONFLICT.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Update")
    class UpdateTests {

        @Test
        void should_update_and_return_202() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex1 = persistSpex(randomizeSpex(category));
            var spex2 = persistSpex(randomizeSpex(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex1));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .put("/{id}/{spexId}", spexActivity.getId(), spex2.getId())
            .then()
                .statusCode(HttpStatus.ACCEPTED.value());
            //@formatter:on

            //@formatter:off
            final List<SpexActivityDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
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

            assertThat(result).hasSize(1);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_updating_and_spexare_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
                .pathParam("activityId", activity.getId())
            .when()
                .put("/{id}/{spexId}", spexActivity.getId(), spex.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_updating_and_activity_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", 1L)
            .when()
                .put("/{id}/{spexId}", spexActivity.getId(), spex.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_updating_and_spex_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .put("/{id}/{spexId}", spexActivity.getId(), 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_422_when_updating_and_incorrect_spexare() {
            var spexare1 = persistSpexare(randomizeSpexare());
            var spexare2 = persistSpexare(randomizeSpexare());
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));
            var activity = persistActivity(randomizeActivity(spexare2));
            var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .put("/{id}/{spexId}", spexActivity.getId(), spex.getId())
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_422_when_updating_and_incorrect_activity() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));
            var activity1 = persistActivity(randomizeActivity(spexare));
            var activity2 = persistActivity(randomizeActivity(spexare));
            var spexActivity = persistSpexActivity(randomizeSpexActivity(activity2, spex));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity1.getId())
            .when()
                .put("/{id}/{spexId}", spexActivity.getId(), spex.getId())
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        void should_delete_and_return_204() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
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
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
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
            var spexare = persistSpexare(randomizeSpexare());
            var activity = persistActivity(randomizeActivity(spexare));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .delete("/{id}", 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_deleting_and_spexare_not_found() {
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));
            var activity = persistActivity(randomizeActivity(null));
            var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
                .pathParam("activityId", activity.getId())
            .when()
                .delete("/{id}", spexActivity.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_deleting_and_activity_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));
            var activity = persistActivity(randomizeActivity(null));
            var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", 1L)
            .when()
                .delete("/{id}", spexActivity.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_422_when_deleting_and_incorrect_spexare() {
            var spexare1 = persistSpexare(randomizeSpexare());
            var spexare2 = persistSpexare(randomizeSpexare());
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));
            var activity = persistActivity(randomizeActivity(spexare2));
            var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .delete("/{id}", spexActivity.getId())
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_422_when_deleting_and_incorrect_activity() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));
            var activity1 = persistActivity(randomizeActivity(spexare));
            var activity2 = persistActivity(randomizeActivity(spexare));
            var spexActivity = persistSpexActivity(randomizeSpexActivity(activity2, spex));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity1.getId())
            .when()
                .delete("/{id}", spexActivity.getId())
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }
    }

    private SpexActivity randomizeSpexActivity(Activity activity, Spex spex) {
        var spexActivity = random.nextObject(SpexActivity.class);
        spexActivity.setActivity(activity);
        spexActivity.setSpex(spex);
        return spexActivity;
    }

    private SpexActivity persistSpexActivity(SpexActivity spexActivity) {
        return repository.save(spexActivity);
    }

    private Activity randomizeActivity(Spexare spexare) {
        var activity = random.nextObject(Activity.class);
        activity.setSpexare(spexare);
        return activity;
    }

    private Activity persistActivity(Activity activity) {
        return activityRepository.save(activity);
    }

    private Spexare randomizeSpexare() {
        return random.nextObject(Spexare.class);
    }

    private Spexare persistSpexare(Spexare spexare) {
        return spexareRepository.save(spexare);
    }

    private Spex randomizeSpex(SpexCategory category) {
        var spex = random.nextObject(Spex.class);
        spex.setParent(null);
        var details = random.nextObject(SpexDetails.class);
        details.setCategory(category);
        spex.setDetails(details);
        return spex;
    }

    private Spex persistSpex(Spex spex) {
        var details = spexDetailsRepository.save(spex.getDetails());
        spex.setDetails(details);
        return spexRepository.save(spex);
    }

    private SpexCategory randomizeSpexCategory() {
        return random.nextObject(SpexCategory.class);
    }

    private SpexCategory persistSpexCategory(SpexCategory category) {
        return spexCategoryRepository.save(category);
    }

}
