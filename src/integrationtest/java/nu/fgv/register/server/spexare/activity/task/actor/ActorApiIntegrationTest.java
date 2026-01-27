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
import nu.fgv.register.server.util.AbstractAuditable;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.HalEmbeddedResponse;
import nu.fgv.register.server.util.randomizer.LabelsRandomizer;
import nu.fgv.register.server.util.randomizer.SocialSecurityNumberRandomizer;
import nu.fgv.register.server.util.randomizer.YearRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.ApiVersionInserter;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;
import java.util.Set;
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
class ActorApiIntegrationTest extends AbstractIntegrationTest {

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
    public ActorApiIntegrationTest(final JdbcClient jdbcClient,
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
                .excludeField(named("version").and(ofType(Long.class)).and(inClass(AbstractAuditable.class)))
                .randomizationDepth(1);
        random = new EasyRandom(parameters);
    }

    @BeforeEach
    void setUp() {
        restTestClient = RestTestClient
                .bindToServer()
                .baseUrl("http://localhost:%s/api/spexare/{spexareId}/activities/{activityId}/task-activities/{taskActivityId}/actors".formatted(localPort))
                .apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
                .build();

        JdbcTestUtils.deleteFromTables(jdbcClient, "actor", "type", "task_activity", "activity", "spexare", "task", "task_category", "event", "actor_audit", "type_audit", "task_activity_audit", "activity_audit", "spexare_audit", "task_audit", "task_category_audit");
    }

    @AfterEach
    void tearDown() {
    }

    @Nested
    @DisplayName("Retrieve paged")
    class RetrievePagedTests {

        @Test
        void should_return_404() {
            restTestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder.build(1L, 1L, 1L))
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        void should_return_zero() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            final List<ActorDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder.build(spexare.getId(), activity.getId(), taskActivity.getId()))
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<ActorDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("actors");

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
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            persistActor(randomizeActor(vocal, taskActivity));

            final List<ActorDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder.build(spexare.getId(), activity.getId(), taskActivity.getId()))
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<ActorDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("actors");

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
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            IntStream.range(0, size).forEach(i -> persistActor(randomizeActor(vocal, taskActivity)));

            final List<ActorDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("size", size)
                                            .build(spexare.getId(), activity.getId(), taskActivity.getId())
                                    )
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<ActorDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("actors");

            assertThat(result).hasSize(size);
        }

        @Test
        void should_return_zero_when_incorrect_spexare() {
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

            final List<ActorDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder.build(spexare1.getId(), activity.getId(), taskActivity.getId()))
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<ActorDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("actors");

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_zero_when_incorrect_activity() {
            final var spexare1 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare1.getId()));
            final var spexare2 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare2.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Task.class, task.getId()));
            final var activity1 = persistActivity(randomizeActivity(spexare1));
            final var activity2 = persistActivity(randomizeActivity(spexare2));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity2, task));

            final List<ActorDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder.build(spexare2.getId(), activity1.getId(), taskActivity.getId()))
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<ActorDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("actors");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Retrieve paged with filtering")
    class RetrievePagedWithFilteringTests {

        @Test
        void should_return_404() {
            restTestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("filter", Actor_.ROLE + ":whatever")
                            .build(1L, 1L, 1L))
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        void should_return_zero() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            persistActor(randomizeActor(vocal, taskActivity));

            final List<ActorDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("filter", Actor_.ROLE + ":whatever")
                                            .build(spexare.getId(), activity.getId(), taskActivity.getId()))
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<ActorDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("actors");

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
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));

            final List<ActorDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("filter", Actor_.ROLE + ":" + actor.getRole())
                                            .build(spexare.getId(), activity.getId(), taskActivity.getId()))
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<ActorDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("actors");

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
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            IntStream.range(0, size).forEach(i -> {
                final var actor = randomizeActor(vocal, taskActivity);
                if (i % 2 == 0) {
                    actor.setRole("whatever");
                }
                persistActor(actor);
            });

            final List<ActorDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("filter", Actor_.ROLE + ":whatever")
                                            .queryParam("size", size)
                                            .build(spexare.getId(), activity.getId(), taskActivity.getId()))
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<ActorDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("actors");

            assertThat(result).hasSize(size / 2);
        }

        @Test
        void should_return_zero_when_incorrect_spexare() {
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

            final List<ActorDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("filter", Actor_.ROLE + ":whatever")
                                            .build(spexare1.getId(), activity.getId(), taskActivity.getId()))
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<ActorDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("actors");

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_zero_when_incorrect_activity() {
            final var spexare1 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare1.getId()));
            final var spexare2 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare2.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Task.class, task.getId()));
            final var activity1 = persistActivity(randomizeActivity(spexare1));
            final var activity2 = persistActivity(randomizeActivity(spexare2));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity2, task));

            final List<ActorDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("filter", Actor_.ROLE + ":whatever")
                                            .build(spexare2.getId(), activity1.getId(), taskActivity.getId()))
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<ActorDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("actors");

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
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));

            final ActorDto result = restTestClient
                    .get()
                    .uri("/{id}", spexare.getId(), activity.getId(), taskActivity.getId(), actor.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(ActorDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id")
                    .isEqualTo(actor.getId());
        }

        @Test
        void should_return_404_when_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));

            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/{id}", spexare.getId(), activity.getId(), taskActivity.getId(), 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

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
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));

            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/{id}", 1L, activity.getId(), taskActivity.getId(), actor.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

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
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));

            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/{id}", spexare.getId(), 1L, taskActivity.getId(), actor.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_task_activity_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));

            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/{id}", spexare.getId(), activity.getId(), 1L, actor.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

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
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));

            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/{id}", spexare1.getId(), activity.getId(), taskActivity.getId(), actor.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

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
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));

            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/{id}", spexare.getId(), activity1.getId(), taskActivity.getId(), actor.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_incorrect_task_activity() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Task.class, task.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity1 = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var taskActivity2 = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity2));

            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/{id}", spexare.getId(), activity.getId(), taskActivity1.getId(), actor.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

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
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var dto = random.nextObject(ActorCreateDto.class);

            restTestClient
                    .post()
                    .uri("/{vocalId}", spexare.getId(), activity.getId(), taskActivity.getId(), vocal.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isCreated();

            final List<ActorDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder.build(spexare.getId(), activity.getId(), taskActivity.getId()))
                                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<ActorDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("actors");

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
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var dto = random.nextObject(ActorCreateDto.class);

            final ProblemDetail result = restTestClient
                    .post()
                    .uri("/{vocalId}", 1L, activity.getId(), taskActivity.getId(), vocal.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

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
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var dto = random.nextObject(ActorCreateDto.class);

            final ProblemDetail result = restTestClient
                    .post()
                    .uri("/{vocalId}", spexare.getId(), 1L, taskActivity.getId(), vocal.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_creating_and_task_activity_not_found() {
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

            final ProblemDetail result = restTestClient
                    .post()
                    .uri("/{vocalId}", spexare.getId(), activity.getId(), 1L, vocal.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_creating_and_vocal_not_found() {
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

            final ProblemDetail result = restTestClient
                    .post()
                    .uri("/{vocalId}", spexare.getId(), activity.getId(), taskActivity.getId(), "dummy")
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_creating_and_incorrect_spexare() {
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

            final ProblemDetail result = restTestClient
                    .post()
                    .uri("/{vocalId}", spexare1.getId(), activity.getId(), taskActivity.getId(), vocal.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_creating_and_incorrect_activity() {
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

            final ProblemDetail result = restTestClient
                    .post()
                    .uri("/{vocalId}", spexare.getId(), activity1.getId(), taskActivity.getId(), vocal.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
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
            final var vocal = persistVocal(randomizeVocal());
            final var dto = random.nextObject(ActorCreateDto.class);

            final ProblemDetail result = restTestClient
                    .post()
                    .uri("/{vocalId}", spexare.getId(), activity.getId(), taskActivity.getId(), vocal.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

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
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var dto = random.nextObject(ActorCreateDto.class);

            restTestClient
                    .post()
                    .uri(uriBuilder -> uriBuilder.build(spexare.getId(), activity.getId(), taskActivity.getId()))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isUnauthorized();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Update")
    class UpdateTests {

        @Test
        void should_update_and_return_200() {
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

            final ActorDto before = restTestClient
                    .post()
                    .uri("/{vocalId}", spexare.getId(), activity.getId(), taskActivity.getId(), vocal.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(ActorDto.class)
                    .returnResult()
                    .getResponseBody();

            final var updateDto = ActorUpdateDto.builder().id(before.getId()).role(before.getRole() + "_").build();

            restTestClient
                    .put()
                    .uri("/{vocalId}/{id}", spexare.getId(), activity.getId(), taskActivity.getId(), vocal.getId(), before.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(updateDto)
                    .exchange()
                    .expectStatus().isOk();

            final List<ActorDto> after = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder.build(spexare.getId(), activity.getId(), taskActivity.getId()))
                                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<ActorDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("actors");

            assertThat(after).hasSize(1);
            assertThat(after.getFirst())
                    .extracting("id", "role")
                    .contains(before.getId(), updateDto.role());
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_updating_non_existing_value() {
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

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{vocalId}/{id}", spexare.getId(), activity.getId(), taskActivity.getId(), vocal.getId(), dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
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
            final var vocal = persistVocal(randomizeVocal());
            final var dto = random.nextObject(ActorUpdateDto.class);

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{vocalId}/{id}", 1L, activity.getId(), taskActivity.getId(), vocal.getId(), dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
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
            final var vocal = persistVocal(randomizeVocal());
            final var dto = random.nextObject(ActorUpdateDto.class);

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{vocalId}/{id}", spexare.getId(), 1L, taskActivity.getId(), vocal.getId(), dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_updating_and_task_activity_not_found() {
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

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{vocalId}/{id}", spexare.getId(), activity.getId(), 1L, vocal.getId(), dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_updating_and_vocal_not_found() {
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

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{vocalId}/{id}", spexare.getId(), activity.getId(), taskActivity.getId(), "dummy", dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
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
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));
            final var randDto = random.nextObject(ActorUpdateDto.class);
            final var dto = randDto.toBuilder()
                    .id(actor.getId())
                    .build();

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{vocalId}/{id}", spexare1.getId(), activity.getId(), taskActivity.getId(), vocal.getId(), actor.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

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
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));
            final var randDto = random.nextObject(ActorUpdateDto.class);
            final var dto = randDto.toBuilder()
                    .id(actor.getId())
                    .build();

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{vocalId}/{id}", spexare.getId(), activity1.getId(), taskActivity.getId(), vocal.getId(), actor.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_updating_and_incorrect_task_activity() {
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

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{vocalId}/{id}", spexare.getId(), activity.getId(), taskActivity1.getId(), vocal.getId(), actor.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

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
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));
            final var randDto = random.nextObject(ActorUpdateDto.class);
            final var dto = randDto.toBuilder()
                    .id(actor.getId())
                    .build();

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{vocalId}/{id}", spexare.getId(), activity.getId(), taskActivity.getId(), vocal.getId(), actor.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

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
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));
            final var randDto = random.nextObject(ActorUpdateDto.class);
            final var dto = randDto.toBuilder()
                    .id(actor.getId())
                    .build();

            restTestClient
                    .put()
                    .uri("/{vocalId}/{id}", spexare.getId(), activity.getId(), taskActivity.getId(), vocal.getId(), actor.getId())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isUnauthorized();

            assertThat(repository.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Partial update")
    class PartialUpdateTests {

        @Test
        void should_update_and_return_200() {
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

            final ActorDto before = restTestClient
                    .post()
                    .uri("/{vocalId}", spexare.getId(), activity.getId(), taskActivity.getId(), vocal.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(ActorDto.class)
                    .returnResult()
                    .getResponseBody();

            final var updateDto = ActorUpdateDto.builder().id(before.getId()).role(before.getRole() + "_").build();

            restTestClient
                    .patch()
                    .uri("/{vocalId}/{id}", spexare.getId(), activity.getId(), taskActivity.getId(), vocal.getId(), before.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(updateDto)
                    .exchange()
                    .expectStatus().isOk();

            final List<ActorDto> after = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder.build(spexare.getId(), activity.getId(), taskActivity.getId()))
                                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<ActorDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("actors");

            assertThat(after).hasSize(1);
            assertThat(after.getFirst())
                    .extracting("id", "role")
                    .contains(before.getId(), updateDto.role());
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_updating_non_existing_value() {
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

            final ProblemDetail result = restTestClient
                    .patch()
                    .uri("/{vocalId}/{id}", spexare.getId(), activity.getId(), taskActivity.getId(), vocal.getId(), dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
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
            final var vocal = persistVocal(randomizeVocal());
            final var dto = random.nextObject(ActorUpdateDto.class);

            final ProblemDetail result = restTestClient
                    .patch()
                    .uri("/{vocalId}/{id}", 1L, activity.getId(), taskActivity.getId(), vocal.getId(), dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
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
            final var vocal = persistVocal(randomizeVocal());
            final var dto = random.nextObject(ActorUpdateDto.class);

            final ProblemDetail result = restTestClient
                    .patch()
                    .uri("/{vocalId}/{id}", spexare.getId(), 1L, taskActivity.getId(), vocal.getId(), dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_updating_and_task_activity_not_found() {
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

            final ProblemDetail result = restTestClient
                    .patch()
                    .uri("/{vocalId}/{id}", spexare.getId(), activity.getId(), 1L, vocal.getId(), dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_updating_and_vocal_not_found() {
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

            final ProblemDetail result = restTestClient
                    .patch()
                    .uri("/{vocalId}/{id}", spexare.getId(), activity.getId(), taskActivity.getId(), "dummy", dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
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
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));
            final var randDto = random.nextObject(ActorUpdateDto.class);
            final var dto = randDto.toBuilder()
                    .id(actor.getId())
                    .build();

            final ProblemDetail result = restTestClient
                    .patch()
                    .uri("/{vocalId}/{id}", spexare1.getId(), activity.getId(), taskActivity.getId(), vocal.getId(), actor.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

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
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));
            final var randDto = random.nextObject(ActorUpdateDto.class);
            final var dto = randDto.toBuilder()
                    .id(actor.getId())
                    .build();

            final ProblemDetail result = restTestClient
                    .patch()
                    .uri("/{vocalId}/{id}", spexare.getId(), activity1.getId(), taskActivity.getId(), vocal.getId(), actor.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_updating_and_incorrect_task_activity() {
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

            final ProblemDetail result = restTestClient
                    .patch()
                    .uri("/{vocalId}/{id}", spexare.getId(), activity.getId(), taskActivity1.getId(), vocal.getId(), actor.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

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
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));
            final var randDto = random.nextObject(ActorUpdateDto.class);
            final var dto = randDto.toBuilder()
                    .id(actor.getId())
                    .build();

            final ProblemDetail result = restTestClient
                    .patch()
                    .uri("/{vocalId}/{id}", spexare.getId(), activity.getId(), taskActivity.getId(), vocal.getId(), actor.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

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
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));
            final var randDto = random.nextObject(ActorUpdateDto.class);
            final var dto = randDto.toBuilder()
                    .id(actor.getId())
                    .build();

            restTestClient
                    .patch()
                    .uri("/{vocalId}/{id}", spexare.getId(), activity.getId(), taskActivity.getId(), vocal.getId(), actor.getId())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isUnauthorized();

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
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));

            restTestClient
                    .delete()
                    .uri("/{vocalId}/{id}", spexare.getId(), activity.getId(), taskActivity.getId(), vocal.getId(), actor.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNoContent();

            final List<ActorDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder.build(spexare.getId(), activity.getId(), taskActivity.getId()))
                                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<ActorDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("actors");

            assertThat(result).isEmpty();
            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_deleting_non_existing_value() {
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

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{vocalId}/{id}", spexare.getId(), activity.getId(), taskActivity.getId(), vocal.getId(), 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_deleting_and_spexare_not_found() {
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

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{vocalId}/{id}", 1L, activity.getId(), taskActivity.getId(), vocal.getId(), actor.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

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
            final var activity = persistActivity(randomizeActivity(spexare));
            final var taskActivity = persistTaskActivity(randomizeTaskActivity(activity, task));
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{vocalId}/{id}", spexare.getId(), 1L, taskActivity.getId(), vocal.getId(), actor.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_deleting_and_task_activity_not_found() {
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

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{vocalId}/{id}", spexare.getId(), activity.getId(), 1L, vocal.getId(), actor.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

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
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{vocalId}/{id}", spexare1.getId(), activity.getId(), taskActivity.getId(), vocal.getId(), actor.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

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
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{vocalId}/{id}", spexare.getId(), activity1.getId(), taskActivity.getId(), vocal.getId(), actor.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_deleting_and_incorrect_task_activity() {
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

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{vocalId}/{id}", spexare.getId(), activity.getId(), taskActivity1.getId(), vocal.getId(), actor.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

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
            final var vocal = persistVocal(randomizeVocal());
            final var actor = persistActor(randomizeActor(vocal, taskActivity));

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{vocalId}/{id}", spexare.getId(), activity.getId(), taskActivity.getId(), vocal.getId(), actor.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_401_when_not_permitted_due_to_insufficient_role() {
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

            restTestClient
                    .delete()
                    .uri("/{vocalId}/{id}", spexare.getId(), activity.getId(), taskActivity.getId(), vocal.getId(), actor.getId())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isUnauthorized();

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
