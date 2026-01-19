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

package nu.fgv.register.server.spexare.activity.task.actor;

import nu.fgv.register.server.acl.PermissionService;
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
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import nu.fgv.register.server.util.randomizer.LabelsRandomizer;
import nu.fgv.register.server.util.randomizer.SocialSecurityNumberRandomizer;
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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.inClass;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class ActorGraphqlApiIntegrationTest extends AbstractGraphqlIntegrationTest {

    private final EasyRandom random;
    private final ActorRepository repository;
    private final TaskActivityRepository taskActivityRepository;
    private final ActivityRepository activityRepository;
    private final SpexareRepository spexareRepository;
    private final TaskRepository taskRepository;
    private final TaskCategoryRepository taskCategoryRepository;
    private final TypeRepository typeRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public ActorGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
                                          final AclCache aclCache,
                                          final Keycloak keycloakAdminClient,
                                          final String keycloakClientId,
                                          final PermissionService permissionService,
                                          final ActorRepository repository,
                                          final TaskActivityRepository taskActivityRepository,
                                          final ActivityRepository activityRepository,
                                          final SpexareRepository spexareRepository,
                                          final TaskRepository taskRepository,
                                          final TaskCategoryRepository taskCategoryRepository,
                                          final TypeRepository typeRepository,
                                          final ObjectMapper objectMapper) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.repository = repository;
        this.taskActivityRepository = taskActivityRepository;
        this.activityRepository = activityRepository;
        this.spexareRepository = spexareRepository;
        this.taskRepository = taskRepository;
        this.taskCategoryRepository = taskCategoryRepository;
        this.typeRepository = typeRepository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

        parameters
                .randomize(
                        named("labels"), new LabelsRandomizer()
                )
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

    @BeforeEach
    void setUp() {
        httpGraphQlTester = HttpGraphQlTester.create(
                WebTestClient.bindToServer()
                        .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath))
                        .build()
        );

        JdbcTestUtils.deleteFromTables(jdbcClient, "actor", "type", "task_activity", "activity", "spexare", "task", "task_category", "event");
    }

    @AfterEach
    void tearDown() {
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
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var dto = random.nextObject(ActorCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorCreate")
                    .variable("spexareId", spexare.getId())
                    .variable("activityId", activity.getId())
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", vocal.getId())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .verify()
                    .path("actorCreate", result -> result
                            .path("role").entity(String.class).isEqualTo(dto.role())
                    );

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_NOT_FOUND_when_creating_and_spexare_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var dto = random.nextObject(ActorCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorCreate")
                    .variable("spexareId", 1L)
                    .variable("activityId", activity.getId())
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", vocal.getId())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("actorCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_creating_and_activity_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var dto = random.nextObject(ActorCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorCreate")
                    .variable("spexareId", spexare.getId())
                    .variable("activityId", 1L)
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", vocal.getId())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("actorCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_creating_and_task_activity_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var dto = random.nextObject(ActorCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorCreate")
                    .variable("spexareId", spexare.getId())
                    .variable("activityId", activity.getId())
                    .variable("taskActivityId", 1L)
                    .variable("vocalId", vocal.getId())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("actorCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_creating_and_vocal_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var dto = random.nextObject(ActorCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorCreate")
                    .variable("spexareId", spexare.getId())
                    .variable("activityId", activity.getId())
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", "dummy")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("actorCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_creating_and_incorrect_spexare() {
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
            final var vocal = persistVocal(randomizeVocal());
            final var dto = random.nextObject(ActorCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorCreate")
                    .variable("spexareId", spexare1.getId())
                    .variable("activityId", activity.getId())
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", vocal.getId())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("actorCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_creating_and_incorrect_activity() {
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
            final var vocal = persistVocal(randomizeVocal());
            final var dto = random.nextObject(ActorCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorCreate")
                    .variable("spexareId", spexare.getId())
                    .variable("activityId", activity1.getId())
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", vocal.getId())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("actorCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_permission() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var dto = random.nextObject(ActorCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorCreate")
                    .variable("spexareId", spexare.getId())
                    .variable("activityId", activity.getId())
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", vocal.getId())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("actorCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_role() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var dto = random.nextObject(ActorCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorCreate")
                    .variable("spexareId", spexare.getId())
                    .variable("activityId", activity.getId())
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", vocal.getId())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("actorCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Update")
    class UpdateTests {

        @Test
        void should_update() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var dto = random.nextObject(ActorCreateDto.class);

            final ActorDto before = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorCreate")
                    .variable("spexareId", spexare.getId())
                    .variable("activityId", activity.getId())
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", vocal.getId())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .verify()
                    .path("actorCreate")
                    .entity(ActorDto.class)
                    .get();

            final var updateDto = ActorUpdateDto.builder().id(before.getId()).role(before.getRole() + "_").build();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorUpdate")
                    .variable("spexareId", spexare.getId())
                    .variable("activityId", activity.getId())
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", vocal.getId())
                    .variable("id", before.getId())
                    .variables(objectMapper.convertValue(updateDto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .verify()
                    .path("actorUpdate", result -> result
                            .path("role").entity(String.class).isEqualTo(updateDto.role())
                    );

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_NOT_FOUND_when_updating_non_existing_value() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var dto = random.nextObject(ActorUpdateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorUpdate")
                    .variable("spexareId", spexare.getId())
                    .variable("activityId", activity.getId())
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", vocal.getId())
                    .variable("id", dto.id())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("actorUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_updating_and_spexare_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var dto = random.nextObject(ActorUpdateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorUpdate")
                    .variable("spexareId", 1L)
                    .variable("activityId", activity.getId())
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", vocal.getId())
                    .variable("id", dto.id())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("actorUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_updating_and_activity_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var dto = random.nextObject(ActorUpdateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorUpdate")
                    .variable("spexareId", spexare.getId())
                    .variable("activityId", 1L)
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", vocal.getId())
                    .variable("id", dto.id())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("actorUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_updating_and_task_activity_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var dto = random.nextObject(ActorUpdateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorUpdate")
                    .variable("spexareId", spexare.getId())
                    .variable("activityId", activity.getId())
                    .variable("taskActivityId", 1L)
                    .variable("vocalId", vocal.getId())
                    .variable("id", dto.id())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("actorUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_updating_and_vocal_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var dto = random.nextObject(ActorUpdateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorUpdate")
                    .variable("spexareId", spexare.getId())
                    .variable("activityId", activity.getId())
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", "dummy")
                    .variable("id", dto.id())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("actorUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_updating_and_incorrect_spexare() {
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
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));
            final var randDto = random.nextObject(ActorUpdateDto.class);
            final var dto = randDto.toBuilder()
                    .id(actor.getId())
                    .build();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorUpdate")
                    .variable("spexareId", spexare1.getId())
                    .variable("activityId", activity.getId())
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", vocal.getId())
                    .variable("id", dto.id())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("actorUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_NOT_FOUND_when_updating_and_incorrect_activity() {
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
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));
            final var randDto = random.nextObject(ActorUpdateDto.class);
            final var dto = randDto.toBuilder()
                    .id(actor.getId())
                    .build();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorUpdate")
                    .variable("spexareId", spexare.getId())
                    .variable("activityId", activity1.getId())
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", vocal.getId())
                    .variable("id", dto.id())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("actorUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_NOT_FOUND_when_updating_and_incorrect_task_activity() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity1 = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var taskActivity2 = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity2));
            final var randDto = random.nextObject(ActorUpdateDto.class);
            final var dto = randDto.toBuilder()
                    .id(actor.getId())
                    .build();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorUpdate")
                    .variable("spexareId", spexare.getId())
                    .variable("activityId", activity.getId())
                    .variable("taskActivityId", taskActivity1.getId())
                    .variable("vocalId", vocal.getId())
                    .variable("id", dto.id())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("actorUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_permission() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));
            final var randDto = random.nextObject(ActorUpdateDto.class);
            final var dto = randDto.toBuilder()
                    .id(actor.getId())
                    .build();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorUpdate")
                    .variable("spexareId", spexare.getId())
                    .variable("activityId", activity.getId())
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", vocal.getId())
                    .variable("id", dto.id())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("actorUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_role() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));
            final var randDto = random.nextObject(ActorUpdateDto.class);
            final var dto = randDto.toBuilder()
                    .id(actor.getId())
                    .build();

            httpGraphQlTester
                    .mutate()
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorUpdate")
                    .variable("spexareId", spexare.getId())
                    .variable("activityId", activity.getId())
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", vocal.getId())
                    .variable("id", dto.id())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("actorUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        void should_delete() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorDelete")
                    .variable("spexareId", spexare.getId())
                    .variable("activityId", activity.getId())
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", vocal.getId())
                    .variable("id", actor.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("actorDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_deleting_non_existing_value() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorDelete")
                    .variable("spexareId", spexare.getId())
                    .variable("activityId", activity.getId())
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", vocal.getId())
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("actorDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_deleting_and_spexare_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorDelete")
                    .variable("spexareId", -1L)
                    .variable("activityId", activity.getId())
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", vocal.getId())
                    .variable("id", actor.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("actorDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_NOT_FOUND_when_deleting_and_activity_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorDelete")
                    .variable("spexareId", spexare.getId())
                    .variable("activityId", 1L)
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", vocal.getId())
                    .variable("id", actor.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("actorDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_NOT_FOUND_when_deleting_and_task_activity_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorDelete")
                    .variable("spexareId", spexare.getId())
                    .variable("activityId", activity.getId())
                    .variable("taskActivityId", 1L)
                    .variable("vocalId", vocal.getId())
                    .variable("id", actor.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("actorDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_NOT_FOUND_when_deleting_and_incorrect_spexare() {
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
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorDelete")
                    .variable("spexareId", spexare1.getId())
                    .variable("activityId", activity.getId())
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", vocal.getId())
                    .variable("id", actor.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("actorDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_NOT_FOUND_when_deleting_and_incorrect_activity() {
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
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorDelete")
                    .variable("spexareId", spexare.getId())
                    .variable("activityId", activity1.getId())
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", vocal.getId())
                    .variable("id", actor.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("actorDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_NOT_FOUND_when_deleting_and_incorrect_task_activity() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity1 = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var taskActivity2 = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity2));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorDelete")
                    .variable("spexareId", spexare.getId())
                    .variable("activityId", activity.getId())
                    .variable("taskActivityId", taskActivity1.getId())
                    .variable("vocalId", vocal.getId())
                    .variable("id", actor.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("actorDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_permission() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorDelete")
                    .variable("spexareId", spexare.getId())
                    .variable("activityId", activity.getId())
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", vocal.getId())
                    .variable("id", actor.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("actorDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_role() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));

            httpGraphQlTester
                    .mutate()
                    .build()
                    .documentName("spexare/activity/taskActivity/actor/actorDelete")
                    .variable("spexareId", spexare.getId())
                    .variable("activityId", activity.getId())
                    .variable("taskActivityId", taskActivity.getId())
                    .variable("vocalId", vocal.getId())
                    .variable("id", actor.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("actorDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }
    }

    private Actor randomizeActor(final Type vocal, final TaskActivity taskActivity) {
        final var actor = random.nextObject(Actor.class);
        actor.setTaskActivity(taskActivity);
        actor.setVocal(vocal);
        return actor;
    }

    private Actor persistActor(final Actor actor) {
        actor.setId(null);

        return repository.save(actor);
    }

    private TaskActivity randomizeTaskActivity(final Activity activity, final Task task) {
        final var taskActivity = random.nextObject(TaskActivity.class);
        taskActivity.setActivity(activity);
        taskActivity.setTask(task);
        return taskActivity;
    }

    private TaskActivity persistTaskActivity(final TaskActivity taskActivity) {
        taskActivity.setId(null);

        return taskActivityRepository.save(taskActivity);
    }

    private Activity randomizeActivity(final Spexare spexare) {
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

    private Type randomizeVocal() {
        final var type = random.nextObject(Type.class);
        type.setType(TypeType.VOCAL);
        return type;
    }

    private Type persistVocal(final Type type) {
        return typeRepository.save(type);
    }


}
