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

package nu.fgv.register.server.spexare.activity.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.activity.Activity;
import nu.fgv.register.server.spexare.activity.ActivityRepository;
import nu.fgv.register.server.spexare.activity.spex.SpexActivity;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.lang.Nullable;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
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
class TaskActivityApiIntegrationTest extends AbstractIntegrationTest {

    private final EasyRandom random;
    private final TaskActivityRepository repository;
    private final ActivityRepository activityRepository;
    private final SpexareRepository spexareRepository;
    private final TaskRepository taskRepository;
    private final TaskCategoryRepository taskCategoryRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public TaskActivityApiIntegrationTest(final JdbcClient jdbcClient,
                                          final AclCache aclCache,
                                          final Keycloak keycloakAdminClient,
                                          final String keycloakClientId,
                                          final PermissionService permissionService,
                                          final ObjectMapper objectMapper,
                                          final TaskActivityRepository repository,
                                          final ActivityRepository activityRepository,
                                          final SpexareRepository spexareRepository,
                                          final TaskRepository taskRepository,
                                          final TaskCategoryRepository taskCategoryRepository) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.repository = repository;
        this.activityRepository = activityRepository;
        this.spexareRepository = spexareRepository;
        this.taskRepository = taskRepository;
        this.taskCategoryRepository = taskCategoryRepository;

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
    void setUp() {
        RestAssured.port = localPort;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        final RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder();
        requestSpecBuilder.setBasePath(basePath);
        RestAssured.requestSpecification = requestSpecBuilder.build();
        RestAssured.config = config()
                .encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))
                .logConfig(LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails());

        JdbcTestUtils.deleteFromTables(jdbcClient, "task_activity", "activity", "spexare", "task", "task_category", "event");
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
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));

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
        void should_return_one() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
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
        void should_return_many() {
            final int size = 42;
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            IntStream.range(0, size).forEach(i -> persistTaskActivity(randomizeTaskActivity(activity, task)));

            //@formatter:off
            final List<TaskActivityDto> result =
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
                        .jsonPath().getList("_embedded.task-activities", TaskActivityDto.class);
            //@formatter:on

            assertThat(result).hasSize(size);
        }

        @Test
        void should_return_zero_when_incorrect_spexare() {
            final var spexare1 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare1.getId()));
            final var spexare2 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare2.getId()));
            final var activity = persistActivity(randomizeActivity(spexare2));

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
        void should_return_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

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
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
                .pathParam("activityId", activity.getId())
            .when()
                .get("/{id}", taskActivity.getId())
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
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", 1L)
            .when()
                .get("/{id}", taskActivity.getId())
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
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare2));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .get("/{id}", taskActivity.getId())
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
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Task.class, task.getId()));
            final var activity1 = persistActivity(randomizeActivity(spexare));
            final var activity2 = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity2, task));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity1.getId())
            .when()
                .get("/{id}", taskActivity.getId())
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
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
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
                        .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
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
        void should_return_404_when_creating_and_spexare_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
                .pathParam("activityId", activity.getId())
            .when()
                .post("/{taskId}", task.getId())
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
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            persistActivity(randomizeActivity(spexare));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", 1L)
            .when()
                .post("/{taskId}", task.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_creating_and_task_not_found() {
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
                .post("/{taskId}", 1L)
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
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare2));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .post("/{taskId}", task.getId())
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
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .post("/{taskId}", task.getId())
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
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .post("/{taskId}", task.getId())
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
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task1 = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task1.getId()));
            final var task2 = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task2.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task1));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .put("/{id}/{taskId}", taskActivity.getId(), task2.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            //@formatter:off
            final List<TaskActivityDto> result =
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
                        .jsonPath().getList("_embedded.task-activities", TaskActivityDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_updating_and_spexare_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
                .pathParam("activityId", activity.getId())
            .when()
                .put("/{id}/{taskId}", taskActivity.getId(), task.getId())
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
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", 1L)
            .when()
                .put("/{id}/{taskId}", taskActivity.getId(), task.getId())
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
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .put("/{id}/{taskId}", taskActivity.getId(), 1L)
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
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare2));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .put("/{id}/{taskId}", taskActivity.getId(), task.getId())
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
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity1 = persistActivity(randomizeActivity(spexare));
            final var activity2 = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity2, task));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity1.getId())
            .when()
                .put("/{id}/{taskId}", taskActivity.getId(), task.getId())
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
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task1 = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task1.getId()));
            final var task2 = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task2.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task1));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .put("/{id}/{taskId}", taskActivity.getId(), task2.getId())
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
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task1 = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task1.getId()));
            final var task2 = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task2.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task1));

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .put("/{id}/{taskId}", taskActivity.getId(), task2.getId())
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
        void should_delete_and_return_204() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
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
                        .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
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
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(null));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
                .pathParam("activityId", activity.getId())
            .when()
                .delete("/{id}", taskActivity.getId())
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
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(null));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", 1L)
            .when()
                .delete("/{id}", taskActivity.getId())
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
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare2));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .delete("/{id}", taskActivity.getId())
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
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity1 = persistActivity(randomizeActivity(spexare));
            final var activity2 = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity2, task));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity1.getId())
            .when()
                .delete("/{id}", taskActivity.getId())
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
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .delete("/{id}", taskActivity.getId())
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
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .pathParam("activityId", activity.getId())
            .when()
                .delete("/{id}", taskActivity.getId())
            .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }
    }

    private TaskActivity randomizeTaskActivity(final Activity activity, final Task task) {
        final var taskActivity = random.nextObject(TaskActivity.class);
        taskActivity.setActivity(activity);
        taskActivity.setTask(task);
        return taskActivity;
    }

    private TaskActivity persistTaskActivity(final TaskActivity taskActivity) {
        taskActivity.setId(null);

        return repository.save(taskActivity);
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

    private Task randomizeTask(final TaskCategory category) {
        final var task = random.nextObject(Task.class);
        task.setCategory(category);
        return task;
    }

    private Task persistTask(final Task task) {
        task.setId(null);

        return taskRepository.save(task);
    }

    private TaskCategory randomizeTaskCategory() {
        return random.nextObject(TaskCategory.class);
    }

    private TaskCategory persistTaskCategory(final TaskCategory category) {
        category.setId(null);

        return taskCategoryRepository.save(category);
    }

}
