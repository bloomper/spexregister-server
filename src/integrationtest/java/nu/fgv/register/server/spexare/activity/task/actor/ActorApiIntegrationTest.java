package nu.fgv.register.server.spexare.activity.task.actor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.settings.TypeRepository;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.activity.Activity;
import nu.fgv.register.server.spexare.activity.ActivityRepository;
import nu.fgv.register.server.spexare.activity.spex.SpexActivity;
import nu.fgv.register.server.spexare.activity.task.TaskActivity;
import nu.fgv.register.server.spexare.activity.task.TaskActivityRepository;
import nu.fgv.register.server.task.Task;
import nu.fgv.register.server.task.TaskRepository;
import nu.fgv.register.server.task.category.TaskCategory;
import nu.fgv.register.server.task.category.TaskCategoryRepository;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.randomizer.SocialSecurityNumberRandomizer;
import nu.fgv.register.server.util.randomizer.YearRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

@Disabled
class ActorApiIntegrationTest extends AbstractIntegrationTest {

    private static String basePath;
    private final EasyRandom random;
    @LocalServerPort
    private int localPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ActorRepository repository;

    @Autowired
    private TaskActivityRepository taskActivityRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private SpexareRepository spexareRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCategoryRepository taskCategoryRepository;

    @Autowired
    private TypeRepository typeRepository;

    public ActorApiIntegrationTest() {
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
        basePath = ActorApi.class.getAnnotation(RequestMapping.class).value()[0];
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
        taskActivityRepository.deleteAll();
        taskRepository.deleteAll();
        taskCategoryRepository.deleteAll();
        typeRepository.deleteAll();
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
                .pathParam("taskActivityId",1L)
            .when()
                .get()
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return_zero() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            final List<ActorDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                        .pathParam("activityId", activity.getId())
                        .pathParam("taskActivityId", taskActivity.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.actors", ActorDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_one() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            persistActor(randomizeActor(vocal, taskActivity));

            //@formatter:off
            final List<ActorDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                        .pathParam("activityId", activity.getId())
                        .pathParam("taskActivityId", taskActivity.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.actors", ActorDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            int size = 42;
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            IntStream.range(0, size).forEach(i -> persistActor(randomizeActor(vocal, taskActivity)));

            //@formatter:off
            final List<ActorDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                        .pathParam("activityId", activity.getId())
                        .pathParam("taskActivityId", taskActivity.getId())
                        .queryParam("size", size)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.actors", ActorDto.class);
            //@formatter:on

            assertThat(result).hasSize(size);
        }

        @Test
        void should_return_zero_when_incorrect_spexare() {
            var spexare1 = persistSpexare(randomizeSpexare());
            var spexare2 = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare2));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            final List<ActorDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare1.getId())
                        .pathParam("activityId", activity.getId())
                        .pathParam("taskActivityId", taskActivity.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.actors", ActorDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_zero_when_incorrect_activity() {
            var spexare1 = persistSpexare(randomizeSpexare());
            var spexare2 = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity1 = persistActivity(randomizeActivity(spexare1));
            var activity2 = persistActivity(randomizeActivity(spexare2));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity2, task));

            //@formatter:off
            final List<ActorDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare2.getId())
                        .pathParam("activityId", activity1.getId())
                        .pathParam("taskActivityId", taskActivity.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.actors", ActorDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Retrieve paged with filtering")
    class RetrievePagedWithFilteringTests {

        @Test
        void should_return_404() {
            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId",1L)
                .pathParam("activityId",1L)
                .pathParam("taskActivityId",1L)
                .queryParam("filter", Actor_.ROLE + ":whatever")
            .when()
                .get()
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return_zero() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            persistActor(randomizeActor(vocal, taskActivity));

            //@formatter:off
            final List<ActorDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                        .pathParam("activityId", activity.getId())
                        .pathParam("taskActivityId", taskActivity.getId())
                        .queryParam("filter", Actor_.ROLE + ":whatever")
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.actors", ActorDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_one() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var actor = persistActor(randomizeActor(vocal, taskActivity));

            //@formatter:off
            final List<ActorDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                        .pathParam("activityId", activity.getId())
                        .pathParam("taskActivityId", taskActivity.getId())
                        .queryParam("filter", Actor_.ROLE + ":" + actor.getRole())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.actors", ActorDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            int size = 42;
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            IntStream.range(0, size).forEach(i -> {
                var actor = randomizeActor(vocal, taskActivity);
                if (i % 2 == 0) {
                    actor.setRole("whatever");
                }
                persistActor(actor);
            });

            //@formatter:off
            final List<ActorDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                        .pathParam("activityId", activity.getId())
                        .pathParam("taskActivityId", taskActivity.getId())
                        .queryParam("filter", Actor_.ROLE + ":whatever")
                        .queryParam("size", size)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.actors", ActorDto.class);
            //@formatter:on

            assertThat(result).hasSize(size / 2);
        }

        @Test
        void should_return_zero_when_incorrect_spexare() {
            var spexare1 = persistSpexare(randomizeSpexare());
            var spexare2 = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare2));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            final List<ActorDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare1.getId())
                        .pathParam("activityId", activity.getId())
                        .pathParam("taskActivityId", taskActivity.getId())
                        .queryParam("filter", Actor_.ROLE + ":whatever")
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.actors", ActorDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_zero_when_incorrect_activity() {
            var spexare1 = persistSpexare(randomizeSpexare());
            var spexare2 = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity1 = persistActivity(randomizeActivity(spexare1));
            var activity2 = persistActivity(randomizeActivity(spexare2));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity2, task));

            //@formatter:off
            final List<ActorDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare2.getId())
                        .pathParam("activityId", activity1.getId())
                        .pathParam("taskActivityId", taskActivity.getId())
                        .queryParam("filter", Actor_.ROLE + ":whatever")
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.actors", ActorDto.class);
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
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var actor = persistActor(randomizeActor(vocal, taskActivity));

            //@formatter:off
            final ActorDto result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                        .pathParam("activityId", activity.getId())
                        .pathParam("taskActivityId", taskActivity.getId())
                    .when()
                        .get("/{id}", actor.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(ActorDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id")
                    .isEqualTo(actor.getId());
        }

        @Test
        void should_return_404_when_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity.getId())
            .when()
                .get("/{id}", 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return_404_when_spexare_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var actor = persistActor(randomizeActor(vocal, taskActivity));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity.getId())
            .when()
                .get("/{id}", actor.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return_404_when_activity_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var actor = persistActor(randomizeActor(vocal, taskActivity));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", 1L)
                .pathParam("taskActivityId", taskActivity.getId())
            .when()
                .get("/{id}", actor.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return_404_when_task_activity_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var actor = persistActor(randomizeActor(vocal, taskActivity));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", 1L)
            .when()
                .get("/{id}", actor.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return_404_when_incorrect_spexare() {
            var spexare1 = persistSpexare(randomizeSpexare());
            var spexare2 = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare2));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var actor = persistActor(randomizeActor(vocal, taskActivity));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity.getId())
            .when()
                .get("/{id}", actor.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return_404_when_incorrect_activity() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity1 = persistActivity(randomizeActivity(spexare));
            var activity2 = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity2, task));
            var vocal = persistVocal(randomizeVocal());
            var actor = persistActor(randomizeActor(vocal, taskActivity));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                    .pathParam("spexareId", spexare.getId())
                    .pathParam("activityId", activity1.getId())
                    .pathParam("taskActivityId", taskActivity.getId())
                .when()
                    .get("/{id}", actor.getId())
                .then()
                    .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return_404_when_incorrect_task_activity() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity1 = persistTaskActivity(randomizeTaskActivity(activity, task));
            var taskActivity2 = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var actor = persistActor(randomizeActor(vocal, taskActivity2));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity1.getId())
            .when()
                .get("/{id}", actor.getId())
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
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var dto = random.nextObject(ActorCreateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity.getId())
                .body(dto)
            .when()
                .post("/{vocalId}", vocal.getId())
            .then()
                .statusCode(HttpStatus.CREATED.value());
            //@formatter:on

            //@formatter:off
            final List<ActorDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                        .pathParam("activityId", activity.getId())
                        .pathParam("taskActivityId", taskActivity.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.actors", ActorDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_409_when_creating_already_existing_value() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var dto = random.nextObject(ActorCreateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity.getId())
                .body(dto)
            .when()
                .post("/{vocalId}", vocal.getId())
            .then()
                .statusCode(HttpStatus.CREATED.value());
            //@formatter:on

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity.getId())
                .body(dto)
            .when()
                .post("/{vocalId}", vocal.getId())
            .then()
                .statusCode(HttpStatus.CONFLICT.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_creating_and_spexare_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var dto = random.nextObject(ActorCreateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity.getId())
                .body(dto)
            .when()
                .post("/{vocalId}", vocal.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_creating_and_activity_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var dto = random.nextObject(ActorCreateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", 1L)
                .pathParam("taskActivityId", taskActivity.getId())
                .body(dto)
            .when()
                .post("/{vocalId}", vocal.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return_404_when_creating_and_task_activity_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var dto = random.nextObject(ActorCreateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", 1L)
                .body(dto)
            .when()
                .post("/{vocalId}", vocal.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return_404_when_creating_and_vocal_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var dto = random.nextObject(ActorCreateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity.getId())
                .body(dto)
            .when()
                .post("/{vocalId}", "dummy")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_409_when_creating_and_incorrect_spexare() {
            var spexare1 = persistSpexare(randomizeSpexare());
            var spexare2 = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare2));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var dto = random.nextObject(ActorCreateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity.getId())
                .body(dto)
            .when()
                .post("/{vocalId}", vocal.getId())
            .then()
                .statusCode(HttpStatus.CONFLICT.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_409_when_creating_and_incorrect_activity() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity1 = persistActivity(randomizeActivity(spexare));
            var activity2 = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity2, task));
            var vocal = persistVocal(randomizeVocal());
            var dto = random.nextObject(ActorCreateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity1.getId())
                .pathParam("taskActivityId", taskActivity.getId())
                .body(dto)
            .when()
                .post("/{vocalId}", vocal.getId())
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
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var dto = random.nextObject(ActorCreateDto.class);

            //@formatter:off
            final ActorDto before =
                given()
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .contentType(ContentType.JSON)
                    .pathParam("spexareId", spexare.getId())
                    .pathParam("activityId", activity.getId())
                    .pathParam("taskActivityId", taskActivity.getId())
                    .body(dto)
                .when()
                    .post("/{vocalId}", vocal.getId())
                .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .extract().body().as(ActorDto.class);
            //@formatter:on

            var updateDto = ActorUpdateDto.builder().id(before.getId()).role(before.getRole() + "_").build();

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity.getId())
                .body(updateDto)
            .when()
                .put("/{vocalId}/{id}", vocal.getId(), before.getId())
            .then()
                .statusCode(HttpStatus.ACCEPTED.value());
            //@formatter:on

            //@formatter:off
            final List<ActorDto> after =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                        .pathParam("activityId", activity.getId())
                        .pathParam("taskActivityId", taskActivity.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.actors", ActorDto.class);
            //@formatter:on

            assertThat(after).hasSize(1);
            assertThat(after.getFirst())
                    .extracting("id", "role")
                    .contains(before.getId(), updateDto.getRole());
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_updating_non_existing_value() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var dto = random.nextObject(ActorUpdateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity.getId())
                .body(dto)
            .when()
                .put("/{vocalId}/{id}", vocal.getId(), dto.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_updating_and_spexare_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var dto = random.nextObject(ActorUpdateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity.getId())
                .body(dto)
            .when()
                .put("/{vocalId}/{id}", vocal.getId(), dto.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_updating_and_activity_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var dto = random.nextObject(ActorUpdateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", 1L)
                .pathParam("taskActivityId", taskActivity.getId())
                .body(dto)
            .when()
                .put("/{vocalId}/{id}", vocal.getId(), dto.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_updating_and_task_activity_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var dto = random.nextObject(ActorUpdateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", 1L)
                .body(dto)
            .when()
                .put("/{vocalId}/{id}", vocal.getId(), dto.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_updating_and_vocal_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var dto = random.nextObject(ActorUpdateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity.getId())
                .body(dto)
            .when()
                .put("/{vocalId}/{id}", "dummy", dto.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_422_when_updating_and_incorrect_spexare() {
            var spexare1 = persistSpexare(randomizeSpexare());
            var spexare2 = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare2));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var actor = persistActor(randomizeActor(vocal, taskActivity));
            var dto = random.nextObject(ActorUpdateDto.class);
            dto.setId(actor.getId());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity.getId())
                .body(dto)
            .when()
                .put("/{vocalId}/{id}", vocal.getId(), actor.getId())
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_422_when_updating_and_incorrect_activity() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity1 = persistActivity(randomizeActivity(spexare));
            var activity2 = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity2, task));
            var vocal = persistVocal(randomizeVocal());
            var actor = persistActor(randomizeActor(vocal, taskActivity));
            var dto = random.nextObject(ActorUpdateDto.class);
            dto.setId(actor.getId());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity1.getId())
                .pathParam("taskActivityId", taskActivity.getId())
                .body(dto)
            .when()
                .put("/{vocalId}/{id}", vocal.getId(), actor.getId())
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_422_when_updating_and_incorrect_task_activity() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity1 = persistTaskActivity(randomizeTaskActivity(activity, task));
            var taskActivity2 = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var actor = persistActor(randomizeActor(vocal, taskActivity2));
            var dto = random.nextObject(ActorUpdateDto.class);
            dto.setId(actor.getId());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity1.getId())
                .body(dto)
            .when()
                .put("/{vocalId}/{id}", vocal.getId(), actor.getId())
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Partial update")
    class PartialUpdateTests {

        @Test
        void should_update_and_return_202() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var dto = random.nextObject(ActorCreateDto.class);

            //@formatter:off
            final ActorDto before =
                given()
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .contentType(ContentType.JSON)
                    .pathParam("spexareId", spexare.getId())
                    .pathParam("activityId", activity.getId())
                    .pathParam("taskActivityId", taskActivity.getId())
                    .body(dto)
                .when()
                    .post("/{vocalId}", vocal.getId())
                .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .extract().body().as(ActorDto.class);
            //@formatter:on

            var updateDto = ActorUpdateDto.builder().id(before.getId()).role(before.getRole() + "_").build();

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity.getId())
                .body(updateDto)
            .when()
                .patch("/{vocalId}/{id}", vocal.getId(), before.getId())
            .then()
                .statusCode(HttpStatus.ACCEPTED.value());
            //@formatter:on

            //@formatter:off
            final List<ActorDto> after =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                        .pathParam("activityId", activity.getId())
                        .pathParam("taskActivityId", taskActivity.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.actors", ActorDto.class);
            //@formatter:on

            assertThat(after).hasSize(1);
            assertThat(after.getFirst())
                    .extracting("id", "role")
                    .contains(before.getId(), updateDto.getRole());
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_updating_non_existing_value() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var dto = random.nextObject(ActorUpdateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity.getId())
                .body(dto)
            .when()
                .patch("/{vocalId}/{id}", vocal.getId(), dto.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_updating_and_spexare_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var dto = random.nextObject(ActorUpdateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity.getId())
                .body(dto)
            .when()
                .patch("/{vocalId}/{id}", vocal.getId(), dto.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_updating_and_activity_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var dto = random.nextObject(ActorUpdateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", 1L)
                .pathParam("taskActivityId", taskActivity.getId())
                .body(dto)
            .when()
                .patch("/{vocalId}/{id}", vocal.getId(), dto.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_updating_and_task_activity_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var dto = random.nextObject(ActorUpdateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", 1L)
                .body(dto)
            .when()
                .patch("/{vocalId}/{id}", vocal.getId(), dto.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_updating_and_vocal_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var dto = random.nextObject(ActorUpdateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity.getId())
                .body(dto)
            .when()
                .patch("/{vocalId}/{id}", "dummy", dto.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_422_when_updating_and_incorrect_spexare() {
            var spexare1 = persistSpexare(randomizeSpexare());
            var spexare2 = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare2));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var actor = persistActor(randomizeActor(vocal, taskActivity));
            var dto = random.nextObject(ActorUpdateDto.class);
            dto.setId(actor.getId());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity.getId())
                .body(dto)
            .when()
                .patch("/{vocalId}/{id}", vocal.getId(), actor.getId())
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_422_when_updating_and_incorrect_activity() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity1 = persistActivity(randomizeActivity(spexare));
            var activity2 = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity2, task));
            var vocal = persistVocal(randomizeVocal());
            var actor = persistActor(randomizeActor(vocal, taskActivity));
            var dto = random.nextObject(ActorUpdateDto.class);
            dto.setId(actor.getId());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity1.getId())
                .pathParam("taskActivityId", taskActivity.getId())
                .body(dto)
            .when()
                .patch("/{vocalId}/{id}", vocal.getId(), actor.getId())
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_422_when_updating_and_incorrect_task_activity() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity1 = persistTaskActivity(randomizeTaskActivity(activity, task));
            var taskActivity2 = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var actor = persistActor(randomizeActor(vocal, taskActivity2));
            var dto = random.nextObject(ActorUpdateDto.class);
            dto.setId(actor.getId());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity1.getId())
                .body(dto)
            .when()
                .patch("/{vocalId}/{id}", vocal.getId(), actor.getId())
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
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var actor = persistActor(randomizeActor(vocal, taskActivity));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity.getId())
            .when()
                .delete("/{vocalId}/{id}", vocal.getId(), actor.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            //@formatter:off
            final List<ActorDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                        .pathParam("activityId", activity.getId())
                        .pathParam("taskActivityId", taskActivity.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.actors", ActorDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_deleting_non_existing_value() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity.getId())
            .when()
                .delete("/{vocalId}/{id}", vocal.getId(), 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_deleting_and_spexare_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var actor = persistActor(randomizeActor(vocal, taskActivity));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", -1L)
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity.getId())
            .when()
                .delete("/{vocalId}/{id}", vocal.getId(), actor.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_deleting_and_activity_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var actor = persistActor(randomizeActor(vocal, taskActivity));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", 1L)
                .pathParam("taskActivityId", taskActivity.getId())
            .when()
                .delete("/{vocalId}/{id}", vocal.getId(), actor.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_deleting_and_task_activity_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var actor = persistActor(randomizeActor(vocal, taskActivity));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", 1L)
            .when()
                .delete("/{vocalId}/{id}", vocal.getId(), actor.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_422_when_deleting_and_incorrect_spexare() {
            var spexare1 = persistSpexare(randomizeSpexare());
            var spexare2 = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare2));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var actor = persistActor(randomizeActor(vocal, taskActivity));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity.getId())
            .when()
                .delete("/{vocalId}/{id}", vocal.getId(), actor.getId())
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_422_when_deleting_and_incorrect_activity() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity1 = persistActivity(randomizeActivity(spexare));
            var activity2 = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity2, task));
            var vocal = persistVocal(randomizeVocal());
            var actor = persistActor(randomizeActor(vocal, taskActivity));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity1.getId())
                .pathParam("taskActivityId", taskActivity.getId())
            .when()
                .delete("/{vocalId}/{id}", vocal.getId(), actor.getId())
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_422_when_deleting_and_incorrect_task_activity() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity1 = persistTaskActivity(randomizeTaskActivity(activity, task));
            var taskActivity2 = persistTaskActivity(randomizeTaskActivity(activity, task));
            var vocal = persistVocal(randomizeVocal());
            var actor = persistActor(randomizeActor(vocal, taskActivity2));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
                .pathParam("taskActivityId", taskActivity1.getId())
            .when()
                .delete("/{vocalId}/{id}", vocal.getId(), actor.getId())
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }
    }

    private Actor randomizeActor(Type vocal, TaskActivity taskActivity) {
        var actor = random.nextObject(Actor.class);
        actor.setTaskActivity(taskActivity);
        actor.setVocal(vocal);
        return actor;
    }

    private Actor persistActor(Actor actor) {
        return repository.save(actor);
    }

    private TaskActivity randomizeTaskActivity(Activity activity, Task task) {
        var taskActivity = random.nextObject(TaskActivity.class);
        taskActivity.setActivity(activity);
        taskActivity.setTask(task);
        return taskActivity;
    }

    private TaskActivity persistTaskActivity(TaskActivity taskActivity) {
        return taskActivityRepository.save(taskActivity);
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

    private Task randomizeTask(TaskCategory category) {
        var task = random.nextObject(Task.class);
        task.setCategory(category);
        return task;
    }

    private Task persistTask(Task task) {
        return taskRepository.save(task);
    }

    private TaskCategory randomizeTaskCategory() {
        return random.nextObject(TaskCategory.class);
    }

    private TaskCategory persistTaskCategory(TaskCategory category) {
        return taskCategoryRepository.save(category);
    }

    private Type randomizeVocal() {
        var type = random.nextObject(Type.class);
        type.setType(TypeType.VOCAL);
        return type;
    }

    private Type persistVocal(Type type) {
        return typeRepository.save(type);
    }


}
