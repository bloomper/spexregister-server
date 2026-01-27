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

package nu.fgv.register.server.task.category;

import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventRepository;
import nu.fgv.register.server.util.AbstractAuditable;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.HalEmbeddedResponse;
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
class TaskCategoryApiIntegrationTest extends AbstractIntegrationTest {

    private final EasyRandom random;
    private final TaskCategoryRepository repository;
    private final EventRepository eventRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public TaskCategoryApiIntegrationTest(final JdbcClient jdbcClient,
                                          final AclCache aclCache,
                                          final Keycloak keycloakAdminClient,
                                          final String keycloakClientId,
                                          final PermissionService permissionService,
                                          final TaskCategoryRepository repository,
                                          final EventRepository eventRepository,
                                          final ObjectMapper objectMapper) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.repository = repository;
        this.eventRepository = eventRepository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

        parameters
                .excludeField(named("version").and(ofType(Long.class)).and(inClass(AbstractAuditable.class)));
        random = new EasyRandom(parameters);
    }

    @BeforeEach
    void setUp() {
        restTestClient = RestTestClient
                .bindToServer()
                .baseUrl("http://localhost:%s/api/tasks/categories".formatted(localPort))
                .apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
                .build();

        JdbcTestUtils.deleteFromTables(jdbcClient, "task_category", "event");
    }

    @AfterEach
    void tearDown() {
    }

    @Nested
    @DisplayName("Retrieve paged")
    class RetrievePagedTests {

        @Test
        void should_return_zero() {
            final List<TaskCategoryDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<TaskCategoryDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("task-categories");

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_one() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));

            final List<TaskCategoryDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<TaskCategoryDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("task-categories");

            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            IntStream.range(0, size).forEach(i -> {
                final var category = persistTaskCategory(randomizeTaskCategory());
                grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));
            });

            final List<TaskCategoryDto> result = Objects.requireNonNull(
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
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<TaskCategoryDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("task-categories");

            assertThat(result).hasSize(size);
        }
    }

    @Nested
    @DisplayName("Retrieve paged with filtering")
    class RetrievePagedWithFilteringTests {

        @Test
        void should_return_zero() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));

            final List<TaskCategoryDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("filter", TaskCategory_.NAME + ":whatever")
                                            .build()
                                    )
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<TaskCategoryDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("task-categories");

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_one() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));

            final List<TaskCategoryDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("filter", TaskCategory_.NAME + ":" + category.getName())
                                            .build()
                                    )
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<TaskCategoryDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("task-categories");

            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            IntStream.range(0, size).forEach(i -> {
                final var category = randomizeTaskCategory();
                if (i % 2 == 0) {
                    category.setName("whatever");
                }
                final var category0 = persistTaskCategory(category);
                grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category0.getId()));
            });

            final List<TaskCategoryDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("filter", TaskCategory_.NAME + ":whatever")
                                            .queryParam("size", size)
                                            .build()
                                    )
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<TaskCategoryDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("task-categories");

            assertThat(result).hasSize(size / 2);
        }
    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create_and_return_201() {
            final TaskCategoryCreateDto dto = random.nextObject(TaskCategoryCreateDto.class);

            final TaskCategoryDto result = restTestClient
                    .post()
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(TaskCategoryDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result)
                    .extracting("name")
                    .isEqualTo(dto.name());
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_400_when_invalid_input() {
            final TaskCategoryCreateDto randDto = random.nextObject(TaskCategoryCreateDto.class);
            final var dto = randDto.toBuilder()
                    .name(null)
                    .build();

            restTestClient
                    .post()
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isBadRequest();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_403_when_not_permitted() {
            final TaskCategoryCreateDto dto = random.nextObject(TaskCategoryCreateDto.class);

            final ProblemDetail result = restTestClient
                    .post()
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
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
    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        void should_return_found() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));

            final TaskCategoryDto result = restTestClient
                    .get()
                    .uri("/{id}", category.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(TaskCategoryDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "name", "actorPresent")
                    .contains(category.getId(), category.getName(), category.getActorPresent());
        }

        @Test
        void should_return_404_when_not_found() {
            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/{id}", 1L)
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
    @DisplayName("Update")
    class UpdateTests {

        @Test
        void should_update_and_return_200() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));

            final TaskCategoryDto before = restTestClient
                    .get()
                    .uri("/{id}", category.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(TaskCategoryDto.class)
                    .returnResult()
                    .getResponseBody();

            final TaskCategoryUpdateDto dto = TaskCategoryUpdateDto.builder()
                    .id(before.getId())
                    .name(before.getName() + "_")
                    .actorPresent(before.getActorPresent())
                    .build();

            final TaskCategoryDto updated = restTestClient
                    .put()
                    .uri("/{id}", category.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(TaskCategoryDto.class)
                    .returnResult()
                    .getResponseBody();

            final TaskCategoryDto after = restTestClient
                    .get()
                    .uri("/{id}", category.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(TaskCategoryDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_400_when_invalid_input() {
            final TaskCategoryUpdateDto randDto = random.nextObject(TaskCategoryUpdateDto.class);
            final var dto = randDto.toBuilder()
                    .name(null)
                    .build();

            restTestClient
                    .put()
                    .uri("/{id}", dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("status").isEqualTo(HttpStatus.BAD_REQUEST.value())
                    .jsonPath("errors").isNotEmpty()
                    .jsonPath("errors.name").isNotEmpty();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_not_found() {
            final TaskCategoryUpdateDto dto = random.nextObject(TaskCategoryUpdateDto.class);

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{id}", dto.id())
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
        void should_return_403_when_not_permitted_due_to_insufficient_permission() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));

            final TaskCategoryDto before = restTestClient
                    .get()
                    .uri("/{id}", category.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(TaskCategoryDto.class)
                    .returnResult()
                    .getResponseBody();

            final TaskCategoryUpdateDto dto = TaskCategoryUpdateDto.builder()
                    .id(before.getId())
                    .name(before.getName() + "_")
                    .actorPresent(before.getActorPresent())
                    .build();

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{id}", dto.id())
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
        void should_return_403_when_not_permitted_due_to_insufficient_role() {
            final TaskCategoryUpdateDto dto = random.nextObject(TaskCategoryUpdateDto.class);

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{id}", dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
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
    }

    @Nested
    @DisplayName("Partial update")
    class PartialUpdateTests {

        @Test
        void should_update_and_return_200() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, category.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));

            final TaskCategoryDto before = restTestClient
                    .get()
                    .uri("/{id}", category.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(TaskCategoryDto.class)
                    .returnResult()
                    .getResponseBody();

            final TaskCategoryUpdateDto dto = TaskCategoryUpdateDto.builder()
                    .id(before.getId())
                    .name(before.getName() + "_")
                    .actorPresent(before.getActorPresent())
                    .build();

            final TaskCategoryDto updated = restTestClient
                    .patch()
                    .uri("/{id}", category.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(TaskCategoryDto.class)
                    .returnResult()
                    .getResponseBody();

            final TaskCategoryDto after = restTestClient
                    .get()
                    .uri("/{id}", category.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(TaskCategoryDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_not_found() {
            final TaskCategoryUpdateDto dto = random.nextObject(TaskCategoryUpdateDto.class);

            final ProblemDetail result = restTestClient
                    .patch()
                    .uri("/{id}", dto.id())
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
        void should_return_403_when_not_permitted_due_to_insufficient_permission() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));

            final TaskCategoryDto before = restTestClient
                    .get()
                    .uri("/{id}", category.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(TaskCategoryDto.class)
                    .returnResult()
                    .getResponseBody();

            final TaskCategoryUpdateDto dto = TaskCategoryUpdateDto.builder()
                    .id(before.getId())
                    .name(before.getName() + "_")
                    .build();

            final ProblemDetail result = restTestClient
                    .patch()
                    .uri("/{id}", dto.id())
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
        void should_return_403_when_not_permitted_due_to_insufficient_role() {
            final TaskCategoryUpdateDto dto = random.nextObject(TaskCategoryUpdateDto.class);

            final ProblemDetail result = restTestClient
                    .patch()
                    .uri("/{id}", dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
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
    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        void should_delete_and_return_204() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));
            grantDeletePermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));

            restTestClient
                    .delete()
                    .uri("/{id}", category.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_not_found() {
            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{id}", 123)
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
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
        void should_return_403_when_not_permitted_due_to_insufficient_permission() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{id}", category.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
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
        void should_return_403_when_not_permitted_due_to_insufficient_role() {
            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{id}", 123)
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }
    }

    @Nested
    @DisplayName("Events")
    class EventTests {

        @Test
        void should_return_found() {
            final var category = persistTaskCategory(randomizeTaskCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(TaskCategory.class, category.getId()));

            final List<EventDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri("/events/{sourceId}", category.getId())
                                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<EventDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("events");

            assertThat(eventRepository.count()).isEqualTo(1);
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getEventType()).isEqualTo(Event.EventType.CREATE.name());
            assertThat(result.getFirst().getSourceType()).isEqualTo(Event.SourceType.TASK_CATEGORY.name());
            assertThat(result.getFirst().getCreatedBy()).isEqualTo(category.getCreatedBy());
        }
    }

    private TaskCategory randomizeTaskCategory() {
        return random.nextObject(TaskCategory.class);
    }

    private TaskCategory persistTaskCategory(final TaskCategory category) {
        category.setId(null);

        return repository.save(category);
    }
}