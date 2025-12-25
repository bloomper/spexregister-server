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

package nu.fgv.register.server.news;

import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventRepository;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.ApiVersionInserter;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class NewsApiIntegrationTest extends AbstractIntegrationTest {

    private final EasyRandom random;
    private final NewsRepository repository;
    private final EventRepository eventRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public NewsApiIntegrationTest(final JdbcClient jdbcClient,
                                  final AclCache aclCache,
                                  final Keycloak keycloakAdminClient,
                                  final String keycloakClientId,
                                  final PermissionService permissionService,
                                  final NewsRepository repository,
                                  final EventRepository eventRepository,
                                  final ObjectMapper objectMapper) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.repository = repository;
        this.eventRepository = eventRepository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

        random = new EasyRandom(parameters);
    }

    @BeforeEach
    void setUp() {
        restTestClient = RestTestClient
                .bindToServer()
                .baseUrl("http://localhost:%s/api/news".formatted(localPort))
                .apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
                .build();

        JdbcTestUtils.deleteFromTables(jdbcClient, "news", "event");
    }

    @AfterEach
    void tearDown() {
    }

    @Nested
    @DisplayName("Retrieve paged")
    class RetrievePagedTests {

        @Test
        void should_return_zero() {
            final List<NewsDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<NewsDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("news");

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_one() {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleUser(toObjectIdentity(News.class, news.getId()));

            final List<NewsDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<NewsDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("news");

            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            IntStream.range(0, size).forEach(i -> {
                final var news = persistNews(randomizeNews());
                grantReadPermissionToRoleUser(toObjectIdentity(News.class, news.getId()));
            });

            final List<NewsDto> result = Objects.requireNonNull(
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
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<NewsDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("news");

            assertThat(result).hasSize(size);
        }

    }

    @Nested
    @DisplayName("Retrieve paged with filtering")
    class RetrievePagedWithFilteringTests {

        @Test
        void should_return_zero() {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleUser(toObjectIdentity(News.class, news.getId()));

            final List<NewsDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("filter", News_.SUBJECT + ":whatever")
                                            .build()
                                    )
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<NewsDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("news");

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_one() {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleUser(toObjectIdentity(News.class, news.getId()));

            final List<NewsDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("filter", News_.SUBJECT + ":" + news.getSubject())
                                            .build()
                                    )
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<NewsDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("news");

            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            IntStream.range(0, size).forEach(i -> {
                final var news = randomizeNews();
                if (i % 2 == 0) {
                    news.setSubject("whatever");
                }
                grantReadPermissionToRoleUser(toObjectIdentity(News.class, persistNews(news).getId()));
            });

            final List<NewsDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("filter", News_.SUBJECT + ":whatever")
                                            .queryParam("size", size)
                                            .build()
                                    )
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<NewsDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("news");

            assertThat(result).hasSize(size / 2);
        }

    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create_and_return_201() {
            final NewsCreateDto dto = random.nextObject(NewsCreateDto.class);

            final NewsDto result = restTestClient
                    .post()
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(NewsDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result)
                    .extracting("subject", "text", "visibleFrom")
                    .contains(dto.subject(), dto.text(), dto.visibleFrom());
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_400_when_invalid_input() {
            final NewsCreateDto randDto = random.nextObject(NewsCreateDto.class);
            final var dto = randDto.toBuilder()
                    .subject(null)
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
            final NewsCreateDto dto = random.nextObject(NewsCreateDto.class);

            restTestClient
                    .post()
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isForbidden();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        void should_return_found() {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleUser(toObjectIdentity(News.class, news.getId()));

            final NewsDto result = restTestClient
                    .get()
                    .uri("/{id}", news.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(NewsDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "subject", "text", "visibleFrom")
                    .contains(news.getId(), news.getSubject(), news.getText(), news.getVisibleFrom());
        }

        @Test
        void should_return_404_when_not_found() {
            restTestClient
                    .get()
                    .uri("/{id}", 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    @DisplayName("Update")
    class UpdateTests {

        @Test
        void should_update_and_return_200() throws Exception {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleUser(toObjectIdentity(News.class, news.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(News.class, news.getId()));

            final NewsDto before = restTestClient
                    .get()
                    .uri("/{id}", news.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(NewsDto.class)
                    .returnResult()
                    .getResponseBody();

            final NewsUpdateDto dto = NewsUpdateDto.builder()
                    .id(before.getId())
                    .subject(before.getSubject() + "_")
                    .text(before.getText())
                    .build();

            final NewsDto updated = restTestClient
                    .put()
                    .uri("/{id}", news.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(NewsDto.class)
                    .returnResult()
                    .getResponseBody();

            final NewsDto after = restTestClient
                    .get()
                    .uri("/{id}", news.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(NewsDto.class)
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
            final NewsUpdateDto randDto = random.nextObject(NewsUpdateDto.class);
            final var dto = randDto.toBuilder()
                    .subject(null)
                    .build();

            restTestClient
                    .put()
                    .uri("/{id}", dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isBadRequest();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_not_found() {
            final NewsUpdateDto dto = random.nextObject(NewsUpdateDto.class);

            restTestClient
                    .put()
                    .uri("/{id}", dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isNotFound();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_permission() {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleAdmin(toObjectIdentity(News.class, news.getId()));

            final NewsDto before = restTestClient
                    .get()
                    .uri("/{id}", news.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(NewsDto.class)
                    .returnResult()
                    .getResponseBody();

            final NewsUpdateDto dto = NewsUpdateDto.builder()
                    .id(before.getId())
                    .subject(before.getSubject() + "_")
                    .text(before.getText())
                    .build();

            restTestClient
                    .put()
                    .uri("/{id}", dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isForbidden();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_role() {
            final NewsUpdateDto dto = random.nextObject(NewsUpdateDto.class);

            restTestClient
                    .put()
                    .uri("/{id}", dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isForbidden();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Partial update")
    class PartialUpdateTests {

        @Test
        void should_update_and_return_200() {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleUser(toObjectIdentity(News.class, news.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(News.class, news.getId()));

            final NewsDto before = restTestClient
                    .get()
                    .uri("/{id}", news.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(NewsDto.class)
                    .returnResult()
                    .getResponseBody();

            final NewsUpdateDto dto = NewsUpdateDto.builder()
                    .id(before.getId())
                    .subject(before.getSubject() + "_")
                    .text(before.getText())
                    .visibleFrom(LocalDate.now().minusDays(3))
                    .build();

            final NewsDto updated = restTestClient
                    .patch()
                    .uri("/{id}", news.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(NewsDto.class)
                    .returnResult()
                    .getResponseBody();

            final NewsDto after = restTestClient
                    .get()
                    .uri("/{id}", news.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(NewsDto.class)
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
            final NewsUpdateDto dto = random.nextObject(NewsUpdateDto.class);

            restTestClient
                    .patch()
                    .uri("/{id}", dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isNotFound();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_permission() {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleAdmin(toObjectIdentity(News.class, news.getId()));

            final NewsDto before = restTestClient
                    .get()
                    .uri("/{id}", news.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(NewsDto.class)
                    .returnResult()
                    .getResponseBody();

            final NewsUpdateDto dto = NewsUpdateDto.builder()
                    .id(before.getId())
                    .subject(before.getSubject() + "_")
                    .text(before.getText())
                    .visibleFrom(LocalDate.now().minusDays(3))
                    .build();

            restTestClient
                    .patch()
                    .uri("/{id}", dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isForbidden();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_role() {
            final NewsUpdateDto dto = random.nextObject(NewsUpdateDto.class);

            restTestClient
                    .patch()
                    .uri("/{id}", dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isForbidden();

            assertThat(repository.count()).isZero();
        }

    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        void should_delete_and_return_204() {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleAdmin(toObjectIdentity(News.class, news.getId()));
            grantDeletePermissionToRoleAdmin(toObjectIdentity(News.class, news.getId()));

            restTestClient
                    .delete()
                    .uri("/{id}", news.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_not_found() {
            restTestClient
                    .delete()
                    .uri("/{id}", 123)
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_permission() {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleAdmin(toObjectIdentity(News.class, news.getId()));

            restTestClient
                    .delete()
                    .uri("/{id}", news.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_role() {
            restTestClient
                    .delete()
                    .uri("/{id}", 123)
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Permissions")
    class PermissionTests {

        @Test
        void should_create_non_published() throws Exception {
            final NewsCreateDto randDto = random.nextObject(NewsCreateDto.class);
            final var dto = randDto.toBuilder()
                    .visibleFrom(LocalDate.now().plusDays(1))
                    .visibleTo(LocalDate.now().plusDays(2))
                    .build();

            final NewsDto created = restTestClient
                    .post()
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(NewsDto.class)
                    .returnResult()
                    .getResponseBody();

            restTestClient
                    .get()
                    .uri("/{id}", created.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden();

            final List<NewsDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<NewsDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("news");

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isEmpty();
        }

        @Test
        void should_create_published() throws Exception {
            final NewsCreateDto randDto = random.nextObject(NewsCreateDto.class);
            final var dto = randDto.toBuilder()
                    .visibleFrom(LocalDate.now().minusDays(1))
                    .visibleTo(LocalDate.now().plusDays(2))
                    .build();

            final NewsDto created = restTestClient
                    .post()
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(NewsDto.class)
                    .returnResult()
                    .getResponseBody();

            restTestClient
                    .get()
                    .uri("/{id}", created.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk();

            final List<NewsDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<NewsDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("news");

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Events")
    class EventTests {

        @Test
        void should_return_found() {
            final var news = persistNews(randomizeNews());

            final List<EventDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri("/events")
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
            assertThat(result.getFirst().getEvent()).isEqualTo(Event.EventType.CREATE.name());
            assertThat(result.getFirst().getSource()).isEqualTo(Event.SourceType.NEWS.name());
            assertThat(result.getFirst().getCreatedBy()).isEqualTo(news.getCreatedBy());
        }

        @Test
        void should_return_403_when_not_permitted() {
            restTestClient
                    .get()
                    .uri("/events")
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden();
        }
    }

    private News randomizeNews() {
        final var news = random.nextObject(News.class);

        news.setPublished(true);

        return news;
    }

    private News persistNews(final News news) {
        news.setId(null);

        return repository.save(news);
    }

}
