package nu.fgv.register.server.spexare.activity.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.activity.Activity;
import nu.fgv.register.server.spexare.activity.ActivityRepository;
import nu.fgv.register.server.spexare.activity.spex.SpexActivity;
import nu.fgv.register.server.task.Task;
import nu.fgv.register.server.task.category.TaskCategory;
import nu.fgv.register.server.task.category.TaskCategoryRepository;
import nu.fgv.register.server.task.TaskRepository;
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

public class TaskActivityApiIntegrationTest extends AbstractIntegrationTest {

    private static String basePath;
    private final EasyRandom random;
    @LocalServerPort
    private int localPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskActivityRepository repository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private SpexareRepository spexareRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCategoryRepository taskCategoryRepository;

    public TaskActivityApiIntegrationTest() {
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
        basePath = TaskActivityApi.class.getAnnotation(RequestMapping.class).value()[0];
    }

    @BeforeEach
    public void setUp() {
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
        taskRepository.deleteAll();
        taskCategoryRepository.deleteAll();
    }

    @AfterEach
    public void tearDown() {
        RestAssured.reset();
    }

    @Nested
    @DisplayName("Retrieve paged")
    class RetrievePagedTests {

        @Test
        public void should_return_404() {
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
        public void should_return_zero() {
            var spexare = persistSpexare(randomizeSpexare());
            var activity = persistActivity(randomizeActivity(spexare));

            //@formatter:off
            final List<TaskActivityDto> result =
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
                        .jsonPath().getList("_embedded.task-activities", TaskActivityDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        public void should_return_one() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            final List<TaskActivityDto> result =
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
                        .jsonPath().getList("_embedded.task-activities", TaskActivityDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        public void should_return_many() {
            int size = 42;
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            IntStream.range(0, size).forEach(i -> persistTaskActivity(randomizeTaskActivity(activity, task)));

            //@formatter:off
            final List<TaskActivityDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                        .pathParam("activityId", activity.getId())
                    .when()
                        .queryParam("size", size)
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.task-activities", TaskActivityDto.class);
            //@formatter:on

            assertThat(result).hasSize(size);
        }

        @Test
        public void should_return_zero_when_incorrect_spexare() {
            var spexare1 = persistSpexare(randomizeSpexare());
            var spexare2 = persistSpexare(randomizeSpexare());
            var activity = persistActivity(randomizeActivity(spexare2));

            //@formatter:off
            final List<TaskActivityDto> result =
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
                        .jsonPath().getList("_embedded.task-activities", TaskActivityDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        public void should_return_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            final TaskActivityDto result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                        .pathParam("activityId", activity.getId())
                    .when()
                        .get("/{id}", taskActivity.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(TaskActivityDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id")
                    .isEqualTo(taskActivity.getId());
        }

        @Test
        public void should_return_404_when_not_found() {
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
        public void should_return_404_when_spexare_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
                .pathParam("activityId", activity.getId())
            .when()
                .get("/{id}", taskActivity.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        public void should_return_404_when_activity_not_found() {
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
                .pathParam("activityId", 1L)
            .when()
                .get("/{id}", taskActivity.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        public void should_return_404_when_incorrect_spexare() {
            var spexare1 = persistSpexare(randomizeSpexare());
            var spexare2 = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare2));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .get("/{id}", taskActivity.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        public void should_return_404_when_incorrect_activity() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity1 = persistActivity(randomizeActivity(spexare));
            var activity2 = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity2, task));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                    .pathParam("spexareId", spexare.getId())
                    .pathParam("activityId", activity1.getId())
                .when()
                    .get("/{id}", taskActivity.getId())
                .then()
                    .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }
    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        public void should_create_and_return_201() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .post("/{taskId}", task.getId())
            .then()
                .statusCode(HttpStatus.CREATED.value());
            //@formatter:on

            //@formatter:off
            final List<TaskActivityDto> result =
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
                        .jsonPath().getList("_embedded.task-activities", TaskActivityDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_404_when_creating_and_spexare_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
                .pathParam("activityId", activity.getId())
            .when()
                .post("/{taskId}", task.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(0);
        }

        @Test
        public void should_return_404_when_creating_and_activity_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            persistActivity(randomizeActivity(spexare));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", 1L)
            .when()
                .post("/{taskId}", task.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        public void should_return_404_when_creating_and_task_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var activity = persistActivity(randomizeActivity(spexare));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .post("/{taskId}", 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(0);
        }

        @Test
        public void should_return_409_when_creating_and_incorrect_spexare() {
            var spexare1 = persistSpexare(randomizeSpexare());
            var spexare2 = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare2));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .post("/{taskId}", task.getId())
            .then()
                .statusCode(HttpStatus.CONFLICT.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Update")
    class UpdateTests {

        @Test
        public void should_update_and_return_202() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task1 = persistTask(randomizeTask(category));
            var task2 = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task1));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .put("/{id}/{taskId}", taskActivity.getId(), task2.getId())
            .then()
                .statusCode(HttpStatus.ACCEPTED.value());
            //@formatter:on

            //@formatter:off
            final List<TaskActivityDto> result =
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
                        .jsonPath().getList("_embedded.task-activities", TaskActivityDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_404_when_updating_and_spexare_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
                .pathParam("activityId", activity.getId())
            .when()
                .put("/{id}/{taskId}", taskActivity.getId(), task.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_404_when_updating_and_activity_not_found() {
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
                .pathParam("activityId", 1L)
            .when()
                .put("/{id}/{taskId}", taskActivity.getId(), task.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_404_when_updating_and_spex_not_found() {
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
            .when()
                .put("/{id}/{taskId}", taskActivity.getId(), 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_422_when_updating_and_incorrect_spexare() {
            var spexare1 = persistSpexare(randomizeSpexare());
            var spexare2 = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare2));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .put("/{id}/{taskId}", taskActivity.getId(), task.getId())
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_422_when_updating_and_incorrect_activity() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity1 = persistActivity(randomizeActivity(spexare));
            var activity2 = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity2, task));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity1.getId())
            .when()
                .put("/{id}/{taskId}", taskActivity.getId(), task.getId())
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
        public void should_delete_and_return_204() {
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
            .when()
                .delete("/{id}", taskActivity.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            //@formatter:off
            final List<TaskActivityDto> result =
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
                        .jsonPath().getList("_embedded.task-activities", TaskActivityDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
            assertThat(repository.count()).isEqualTo(0);
        }

        @Test
        public void should_return_404_when_deleting_non_existing_value() {
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

            assertThat(repository.count()).isEqualTo(0);
        }

        @Test
        public void should_return_404_when_deleting_and_spexare_not_found() {
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(null));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
                .pathParam("activityId", activity.getId())
            .when()
                .delete("/{id}", taskActivity.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_404_when_deleting_and_activity_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(null));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", 1L)
            .when()
                .delete("/{id}", taskActivity.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_422_when_deleting_and_incorrect_spexare() {
            var spexare1 = persistSpexare(randomizeSpexare());
            var spexare2 = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity = persistActivity(randomizeActivity(spexare2));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .delete("/{id}", taskActivity.getId())
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_422_when_deleting_and_incorrect_activity() {
            var spexare = persistSpexare(randomizeSpexare());
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));
            var activity1 = persistActivity(randomizeActivity(spexare));
            var activity2 = persistActivity(randomizeActivity(spexare));
            var taskActivity = persistTaskActivity(randomizeTaskActivity(activity2, task));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity1.getId())
            .when()
                .delete("/{id}", taskActivity.getId())
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }
    }

    private TaskActivity randomizeTaskActivity(Activity activity, Task task) {
        var taskActivity = random.nextObject(TaskActivity.class);
        taskActivity.setActivity(activity);
        taskActivity.setTask(task);
        return taskActivity;
    }

    private TaskActivity persistTaskActivity(TaskActivity taskActivity) {
        return repository.save(taskActivity);
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

}
