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

package nu.fgv.register.server.task;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventRepository;
import nu.fgv.register.server.task.category.TaskCategory;
import nu.fgv.register.server.task.category.TaskCategoryRepository;
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
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
import org.springframework.lang.Nullable;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class TaskGraphqlApiIntegrationTest extends AbstractGraphqlIntegrationTest {

    private final EasyRandom random;
    private final TaskRepository repository;
    private final TaskCategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public TaskGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
                                         final AclCache aclCache,
                                         final Keycloak keycloakAdminClient,
                                         final String keycloakClientId,
                                         final PermissionService permissionService,
                                         final ObjectMapper objectMapper,
                                         final TaskRepository repository,
                                         final TaskCategoryRepository categoryRepository,
                                         final EventRepository eventRepository) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.eventRepository = eventRepository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

        random = new EasyRandom(parameters);
    }

    @BeforeEach
    void setUp() {
        httpGraphQlTester = HttpGraphQlTester.builder(
                        WebTestClient.bindToServer()
                                .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath)))
                .build();

        JdbcTestUtils.deleteFromTables(jdbcClient, "task", "task_category", "event");
    }

    @AfterEach
    void tearDown() {
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
                    .documentName("task/taskPaged")
                    .execute()
                    .errors()
                    .verify()
                    .path("taskPaged.edges")
                    .entityList(TaskDto.class)
                    .hasSize(0);
        }

        @Test
        void should_return_one() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Task.class, task.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("task/taskPaged")
                    .execute()
                    .errors()
                    .verify()
                    .path("taskPaged.edges")
                    .entityList(TaskDto.class)
                    .hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));
            IntStream.range(0, size).forEach(i -> {
                final var task = randomizeTask(category);
                if (i % 2 == 0) {
                    task.setName("whatever");
                }
                final var task0 = persistTask(task);
                grantReadPermissionToRoleUser(toObjectIdentity(Task.class, task0.getId()));
            });

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("task/taskPaged")
                    .variable("first", size)
                    .execute()
                    .errors()
                    .verify()
                    .path("taskPaged.edges")
                    .entityList(TaskDto.class)
                    .hasSize(size);
        }

    }

    @Nested
    @DisplayName("Retrieve paged with filtering")
    class RetrievePagedWithFilteringTests {

        @Test
        void should_return_zero() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Task.class, task.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("task/taskPaged")
                    .variable("filter", Task_.NAME + ":whatever")
                    .execute()
                    .errors()
                    .verify()
                    .path("taskPaged.edges")
                    .entityList(TaskDto.class)
                    .hasSize(0);
        }

        @Test
        void should_return_one() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Task.class, task.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("task/taskPaged")
                    .variable("filter", Task_.NAME + ":" + task.getName())
                    .execute()
                    .errors()
                    .verify()
                    .path("taskPaged.edges")
                    .entityList(TaskDto.class)
                    .hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));
            IntStream.range(0, size).forEach(i -> {
                final var task = randomizeTask(category);
                if (i % 2 == 0) {
                    task.setName("whatever");
                }
                final var task0 = persistTask(task);
                grantReadPermissionToRoleUser(toObjectIdentity(Task.class, task0.getId()));
            });

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("task/taskPaged")
                    .variable("first", size)
                    .variable("filter", Task_.NAME + ":whatever")
                    .execute()
                    .errors()
                    .verify()
                    .path("taskPaged.edges")
                    .entityList(TaskDto.class)
                    .hasSize(size / 2);
        }

    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create() throws Exception {
            final TaskCreateDto dto = random.nextObject(TaskCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("task/taskCreate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .verify()
                    .path("taskCreate", result -> result
                            .path("name").entity(String.class).isEqualTo(dto.getName())
                    );

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_BAD_REQUEST_when_invalid_input() {
            final TaskCreateDto dto = random.nextObject(TaskCreateDto.class);
            dto.setName("");

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("task/taskCreate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> Objects.requireNonNull(error.getMessage()).contains("name"))
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.BAD_REQUEST.toString()))
                    )
                    .path("taskCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted() {
            final TaskCreateDto dto = random.nextObject(TaskCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("task/taskCreate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("taskCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        void should_return_found() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Task.class, task.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("task/task")
                    .variable("id", task.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("task", result -> result
                            .path("id").entity(Long.class).isEqualTo(task.getId())
                            .path("name").entity(String.class).isEqualTo(task.getName())
                    );
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("task/task")
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("task")
                    .valueIsNull();
        }
    }

    @Nested
    @DisplayName("Update")
    class UpdateTests {

        @Test
        void should_update() throws Exception {
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Task.class, task.getId()));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));

            final TaskDto before = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("task/task")
                    .variable("id", task.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("task")
                    .entity(TaskDto.class)
                    .get();

            final TaskUpdateDto dto = TaskUpdateDto.builder()
                    .id(before.getId())
                    .name(before.getName() + "_")
                    .build();

            final TaskDto updated = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("task/taskUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .verify()
                    .path("taskUpdate", result -> result
                            .path("name").entity(String.class).isEqualTo(dto.getName())
                    )
                    .entity(TaskDto.class)
                    .get();

            final TaskDto after = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("task/task")
                    .variable("id", task.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("task")
                    .entity(TaskDto.class)
                    .get();

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_BAD_REQUEST_when_invalid_input() {
            final TaskUpdateDto dto = random.nextObject(TaskUpdateDto.class);
            dto.setName("");

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("task/taskUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> Objects.requireNonNull(error.getMessage()).contains("name"))
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.BAD_REQUEST.toString()))
                    )
                    .path("taskUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            final TaskUpdateDto dto = random.nextObject(TaskUpdateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("task/taskUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("taskUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_permission() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));

            final TaskDto before = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("task/task")
                    .variable("id", task.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("task")
                    .entity(TaskDto.class)
                    .get();

            final TaskUpdateDto dto = TaskUpdateDto.builder()
                    .id(before.getId())
                    .name(before.getName() + "_")
                    .build();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("task/taskUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("taskUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_role() {
            final TaskUpdateDto dto = random.nextObject(TaskUpdateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("task/taskUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("taskUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        void should_delete() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            grantDeletePermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("task/taskDelete")
                    .variable("id", task.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("taskDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("task/taskDelete")
                    .variable("id", 123L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("taskDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_permission() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("task/taskDelete")
                    .variable("id", task.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("taskDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("task/taskDelete")
                    .variable("id", 123L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("taskDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Category")
    class CategoryTests {

        @Test
        void should_return_found() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Task.class, task.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("task/task")
                    .variable("id", task.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("task.category", result -> result
                            .path("id").entity(Long.class).isEqualTo(category.getId())
                            .path("name").entity(String.class).isEqualTo(category.getName())
                            .path("actorPresent").entity(Boolean.class).isEqualTo(category.getActorPresent())
                    );

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_add() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(null));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("task/task")
                    .variable("id", task.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("task.category")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("task/taskCategoryAdd")
                    .variable("taskId", task.getId())
                    .variable("id", category.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("taskCategoryAdd")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("task/task")
                    .variable("id", task.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("task.category", result -> result
                            .path("id").entity(Long.class).isEqualTo(category.getId())
                            .path("name").entity(String.class).isEqualTo(category.getName())
                            .path("actorPresent").entity(Boolean.class).isEqualTo(category.getActorPresent())
                    );

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_NOT_FOUND_when_adding_and_task_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("task/taskCategoryAdd")
                    .variable("taskId", 1L)
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("taskCategoryAdd")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_adding_and_category_not_found() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("task/taskCategoryAdd")
                    .variable("taskId", task.getId())
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("taskCategoryAdd")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_remove() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("task/task")
                    .variable("id", task.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("task.category", result -> result
                            .path("id").entity(Long.class).isEqualTo(category.getId())
                            .path("name").entity(String.class).isEqualTo(category.getName())
                            .path("actorPresent").entity(Boolean.class).isEqualTo(category.getActorPresent())
                    );

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("task/taskCategoryRemove")
                    .variable("taskId", task.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("taskCategoryRemove")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("task/task")
                    .variable("id", task.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("task.category")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_ok_when_removing_and_no_category() {
            final var task = persistTask(randomizeTask(null));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("task/taskCategoryRemove")
                    .variable("taskId", task.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("taskCategoryRemove")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_NOT_FOUND_when_removing_and_task_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("task/taskCategoryRemove")
                    .variable("taskId", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("taskCategoryRemove")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_adding_not_permitted_due_to_insufficient_permission() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(null));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("task/taskCategoryAdd")
                    .variable("taskId", task.getId())
                    .variable("id", category.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("taskCategoryAdd")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_adding_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("task/taskCategoryAdd")
                    .variable("taskId", 1L)
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("taskCategoryAdd")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_removing_not_permitted_due_to_insufficient_permission() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            final var task = persistTask(randomizeTask(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Task.class, task.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("task/taskCategoryRemove")
                    .variable("taskId", task.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("taskCategoryRemove")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_removing_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("task/taskCategoryRemove")
                    .variable("taskId", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("taskCategoryRemove")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

    }

    @Nested
    @DisplayName("Events")
    class EventTests {

        @Test
        void should_return_found() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            final var task = persistTask(randomizeTask(category));

            final List<EventDto> result = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("task/taskEvents")
                    .execute()
                    .errors()
                    .verify()
                    .path("taskEvents")
                    .entityList(EventDto.class)
                    .hasSize(1)
                    .get();

            assertThat(eventRepository.count()).isEqualTo(2);
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getEvent()).isEqualTo(Event.EventType.CREATE.name());
            assertThat(result.getFirst().getSource()).isEqualTo(Event.SourceType.TASK.name());
            assertThat(result.getFirst().getCreatedBy()).isEqualTo(task.getCreatedBy());
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("task/taskEvents")
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("taskEvents")
                    .valueIsNull();
        }
    }

    private Task randomizeTask(@Nullable final TaskCategory category) {
        final var task = random.nextObject(Task.class);
        task.setCategory(category);
        return task;
    }

    private TaskCategory randomizeTaskCategory() {
        return random.nextObject(TaskCategory.class);
    }

    private Task persistTask(final Task task) {
        task.setId(null);

        return repository.save(task);
    }

    private TaskCategory persistTaskCategory(final TaskCategory category) {
        category.setId(null);

        return categoryRepository.save(category);
    }
}
