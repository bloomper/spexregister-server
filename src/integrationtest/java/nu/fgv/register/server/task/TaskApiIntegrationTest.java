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

import nu.fgv.register.server.task.category.TaskCategory;
import nu.fgv.register.server.task.category.TaskCategoryDto;
import nu.fgv.register.server.task.category.TaskCategoryRepository;
import nu.fgv.register.server.util.AbstractAuditable;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.HalEmbeddedResponse;
import nu.fgv.register.server.util.randomizer.RandomizerSupport;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
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
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.ApiVersionInserter;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.inClass;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class TaskApiIntegrationTest extends AbstractIntegrationTest {

    private final EasyRandom random;
    private final TaskRepository repository;
    private final TaskCategoryRepository categoryRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public TaskApiIntegrationTest(final JdbcClient jdbcClient,
                                  final Keycloak keycloakAdminClient,
                                  final String keycloakClientId,
                                  final TaskRepository repository,
                                  final TaskCategoryRepository categoryRepository,
                                  final ObjectMapper objectMapper) {
        super(jdbcClient, keycloakAdminClient, keycloakClientId, objectMapper);
        this.repository = repository;
        this.categoryRepository = categoryRepository;

        final EasyRandomParameters parameters = RandomizerSupport.parameters();

        parameters
                .excludeField(named("version").and(ofType(Long.class)).and(inClass(AbstractAuditable.class)));
        random = new EasyRandom(parameters);
    }

    @BeforeEach
    void setUp() {
        restTestClient = RestTestClient
                .bindToServer()
                .baseUrl("http://localhost:%s/api/tasks".formatted(localPort))
                .apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
                .build();

        JdbcTestUtils.deleteFromTables(jdbcClient, "task", "task_category", "task_audit", "task_category_audit");
    }

    @AfterEach
    void tearDown() {
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

    @Nested
    @DisplayName("Retrieve paged")
    class RetrievePagedTests {

        @Test
        void should_return_zero() {
            final List<TaskDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<TaskDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("tasks");

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_one() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            final var task = persistTask(randomizeTask(category));

            final List<TaskDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<TaskDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("tasks");

            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            final var category = persistTaskCategory(randomizeTaskCategory());
            IntStream.range(0, size).forEach(i -> {
                final var task = persistTask(randomizeTask(category));
            });

            final List<TaskDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("size", size)
                                            .build()
                                    )
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<TaskDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("tasks");

            assertThat(result).hasSize(size);
        }

    }

    @Nested
    @DisplayName("Retrieve paged with filtering")
    class RetrievePagedWithFilteringTests {

        @Test
        void should_return_zero() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            final var task = persistTask(randomizeTask(category));

            final List<TaskDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("filter", Task_.NAME + ":whatever")
                                            .build()
                                    )
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<TaskDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("tasks");

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_one() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            final var task = persistTask(randomizeTask(category));

            final List<TaskDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("filter", Task_.NAME + ":" + task.getName())
                                            .build()
                                    )
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<TaskDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("tasks");

            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            final var category = persistTaskCategory(randomizeTaskCategory());
            IntStream.range(0, size).forEach(i -> {
                final var task = randomizeTask(category);
                if (i % 2 == 0) {
                    task.setName("whatever");
                }
                final var task0 = persistTask(task);
            });

            final List<TaskDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("filter", Task_.NAME + ":whatever")
                                            .queryParam("size", size)
                                            .build()
                                    )
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<TaskDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("tasks");

            assertThat(result).hasSize(size / 2);
        }

    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        void should_return_found() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            final var task = persistTask(randomizeTask(category));

            final TaskDto result = restTestClient
                    .get()
                    .uri("/{id}", task.getUuid())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(TaskDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "name")
                    .contains(task.getUuid(), task.getName());
        }

        @Test
        void should_return_404_when_not_found() {
            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/{id}", UUID.randomUUID())
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

}