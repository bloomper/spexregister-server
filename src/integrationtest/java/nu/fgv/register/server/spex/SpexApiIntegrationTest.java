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

package nu.fgv.register.server.spex;

import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventRepository;
import nu.fgv.register.server.spex.category.SpexCategory;
import nu.fgv.register.server.spex.category.SpexCategoryDto;
import nu.fgv.register.server.spex.category.SpexCategoryRepository;
import nu.fgv.register.server.util.AbstractAuditable;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.HalEmbeddedResponse;
import nu.fgv.register.server.util.filter.FilterOperation;
import nu.fgv.register.server.util.randomizer.YearRandomizer;
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
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.ApiVersionInserter;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Paths;
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
class SpexApiIntegrationTest extends AbstractIntegrationTest {

    private final EasyRandom random;
    private final SpexRepository repository;
    private final SpexDetailsRepository detailsRepository;
    private final SpexCategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final ResourceLoader resourceLoader;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public SpexApiIntegrationTest(final JdbcClient jdbcClient,
                                  final AclCache aclCache,
                                  final Keycloak keycloakAdminClient,
                                  final String keycloakClientId,
                                  final PermissionService permissionService,
                                  final SpexRepository repository,
                                  final SpexDetailsRepository detailsRepository,
                                  final SpexCategoryRepository categoryRepository,
                                  final EventRepository eventRepository,
                                  final ObjectMapper objectMapper,
                                  final ResourceLoader resourceLoader) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.repository = repository;
        this.detailsRepository = detailsRepository;
        this.categoryRepository = categoryRepository;
        this.eventRepository = eventRepository;
        this.resourceLoader = resourceLoader;

        final EasyRandomParameters parameters = new EasyRandomParameters();

        parameters
                .randomize(
                        named("year"), new YearRandomizer()
                )
                .randomize(
                        named("firstYear"), new YearRandomizer()
                )
                .excludeField(named("version").and(ofType(Long.class)).and(inClass(AbstractAuditable.class)))
                .randomizationDepth(1);
        random = new EasyRandom(parameters);
    }

    @BeforeEach
    void setUp() {
        restTestClient = RestTestClient
                .bindToServer()
                .baseUrl("http://localhost:%s/api/spex".formatted(localPort))
                .apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
                .build();

        jdbcClient.sql("SELECT id FROM spex WHERE parent_id IS NOT NULL")
                .query()
                .listOfRows()
                .forEach(row ->
                        jdbcClient
                                .sql("UPDATE spex SET parent_id = NULL WHERE id = :id")
                                .param("id", row.get("id"))
                                .update()
                );
        JdbcTestUtils.deleteFromTables(jdbcClient, "spex", "spex_details", "spex_category", "event", "spex_audit", "spex_details_audit", "spex_category_audit");
    }

    @AfterEach
    void tearDown() {
    }

    @Nested
    @DisplayName("Retrieve paged")
    class RetrievePagedTests {

        @Test
        void should_return_zero() {
            final List<SpexDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<SpexDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("spex");

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_one() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));

            final List<SpexDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<SpexDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("spex");

            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            IntStream.range(0, size).forEach(i -> {
                final var spex = persistSpex(randomizeSpex(category));
                grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
                if (i % 2 == 0) {
                    final var revival = randomizeRevival(spex);
                    persistRevival(revival);
                    grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, revival.getId()));
                }
            });

            final List<SpexDto> result = Objects.requireNonNull(
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
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<SpexDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("spex");

            assertThat(result).hasSize(size);
        }

    }

    @Nested
    @DisplayName("Retrieve paged with filtering")
    class RetrievePagedWithFilteringTests {

        @Test
        void should_return_zero() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));

            final List<SpexDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("filter", Spex_.YEAR + ":whatever")
                                            .build()
                                    )
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<SpexDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("spex");

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_one() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));

            final List<SpexDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("filter", Spex_.DETAILS + "." + SpexDetails_.TITLE + ":" + spex.getDetails().getTitle())
                                            .build()
                                    )
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<SpexDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("spex");

            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            IntStream.range(0, size).forEach(i -> {
                final var spex = randomizeSpex(category);
                if (i % 2 == 0) {
                    spex.setYear("1996");
                }
                final var spex0 = persistSpex(spex);
                grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex0.getId()));
                if (i % 4 == 0) {
                    final var revival = randomizeRevival(spex0);
                    persistRevival(revival);
                    grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, revival.getId()));
                }
            });

            final List<SpexDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("filter", "( " + Spex_.YEAR + ":1996 AND " + Spex_.PARENT + ":" + FilterOperation.NULL + " )")
                                            .queryParam("size", size)
                                            .build()
                                    )
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<SpexDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("spex");

            assertThat(result).hasSize(size / 2);
        }

    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create_and_return_201() throws Exception {
            final SpexCreateDto dto = random.nextObject(SpexCreateDto.class);

            final SpexDto result = restTestClient
                    .post()
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(SpexDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result)
                    .extracting("title", "year")
                    .contains(dto.title(), dto.year());

            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_return_400_when_invalid_input() {
            final SpexCreateDto randDto = random.nextObject(SpexCreateDto.class);
            final var dto = randDto.toBuilder()
                    .title(null)
                    .build();

            restTestClient
                    .post()
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("status").isEqualTo(HttpStatus.BAD_REQUEST.value())
                    .jsonPath("errors").isNotEmpty()
                    .jsonPath("errors.title").isNotEmpty();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
        }

        @Test
        void should_return_403_when_not_permitted() {
            final SpexCreateDto dto = random.nextObject(SpexCreateDto.class);

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
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));

            final SpexDto result = restTestClient
                    .get()
                    .uri("/{id}", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(SpexDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "title", "year")
                    .contains(spex.getId(), spex.getDetails().getTitle(), spex.getYear());
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
        void should_update_and_return_200() throws Exception {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            final SpexDto before = restTestClient
                    .get()
                    .uri("/{id}", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(SpexDto.class)
                    .returnResult()
                    .getResponseBody();

            final SpexUpdateDto dto = SpexUpdateDto.builder()
                    .id(before.getId())
                    .title(before.getTitle() + "_")
                    .year(before.getYear())
                    .build();

            final SpexDto updated = restTestClient
                    .put()
                    .uri("/{id}", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(SpexDto.class)
                    .returnResult()
                    .getResponseBody();

            final SpexDto after = restTestClient
                    .get()
                    .uri("/{id}", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(SpexDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_return_400_when_invalid_input() {
            final SpexUpdateDto randDto = random.nextObject(SpexUpdateDto.class);
            final var dto = randDto.toBuilder()
                    .title(null)
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
                    .jsonPath("errors.title").isNotEmpty();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
        }

        @Test
        void should_return_404_when_not_found() {
            final SpexUpdateDto dto = random.nextObject(SpexUpdateDto.class);

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
            assertThat(detailsRepository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_permission() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            final SpexDto before = restTestClient
                    .get()
                    .uri("/{id}", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(SpexDto.class)
                    .returnResult()
                    .getResponseBody();

            final SpexUpdateDto dto = SpexUpdateDto.builder()
                    .id(before.getId())
                    .title(before.getTitle() + "_")
                    .year(before.getYear())
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
            final SpexUpdateDto dto = random.nextObject(SpexUpdateDto.class);

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
        void should_update_and_return_200() throws Exception {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            final SpexDto before = restTestClient
                    .get()
                    .uri("/{id}", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(SpexDto.class)
                    .returnResult()
                    .getResponseBody();

            final SpexUpdateDto dto = SpexUpdateDto.builder()
                    .id(before.getId())
                    .title(before.getTitle() + "_")
                    .year(before.getYear())
                    .build();

            final SpexDto updated = restTestClient
                    .patch()
                    .uri("/{id}", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(SpexDto.class)
                    .returnResult()
                    .getResponseBody();

            final SpexDto after = restTestClient
                    .get()
                    .uri("/{id}", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(SpexDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_not_found() {
            final SpexUpdateDto dto = random.nextObject(SpexUpdateDto.class);

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
            assertThat(detailsRepository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_permission() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            final SpexDto before = restTestClient
                    .get()
                    .uri("/{id}", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(SpexDto.class)
                    .returnResult()
                    .getResponseBody();

            final SpexUpdateDto dto = SpexUpdateDto.builder()
                    .id(before.getId())
                    .title(before.getTitle() + "_")
                    .year(before.getYear())
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
            final SpexUpdateDto dto = random.nextObject(SpexUpdateDto.class);

            final ProblemDetail result =
                    restTestClient
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
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            grantDeletePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            restTestClient
                    .delete()
                    .uri("/{id}", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
        }

        @Test
        void should_return_404_when_not_found() {
            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{id}", 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_permission() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{id}", spex.getId())
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
        void should_return_403_when_not_permitted_due_to_insufficient_role() {
            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{id}", 123L)
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
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

    // ... existing code ...
    @Nested
    @DisplayName("Poster")
    class PosterTests {

        @Test
        void should_update_poster_and_return_204() throws Exception {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var poster = Files.readAllBytes(Paths.get(ResourceUtils.getFile("classpath:test.png").getPath()));

            restTestClient
                    .put()
                    .uri("/{id}/poster", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                    .apiVersion("1.0")
                    .body(poster)
                    .exchange()
                    .expectStatus().isNoContent();

            final byte[] result = restTestClient
                    .get()
                    .uri("/{spexId}/poster", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isEqualTo(poster);
        }

        @Test
        void should_update_poster_via_multipart_and_return_204() throws Exception {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var poster = resourceLoader.getResource("classpath:test.png");

            final MultiValueMap<@NonNull String, Object> parts = new LinkedMultiValueMap<>();

            parts.add("file", poster);

            restTestClient
                    .post()
                    .uri("/{spexId}/poster", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
                    .apiVersion("1.0")
                    .body(parts)
                    .exchange()
                    .expectStatus().isNoContent();

            final byte[] result = restTestClient
                    .get()
                    .uri("/{spexId}/poster", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isEqualTo(Files.readAllBytes(Paths.get(poster.getFile().getPath())));
        }

        @Test
        void should_delete_poster_and_return_204() throws Exception {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var poster = Files.readAllBytes(Paths.get(ResourceUtils.getFile("classpath:test.png").getPath()));

            restTestClient
                    .put()
                    .uri("/{spexId}/poster", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                    .apiVersion("1.0")
                    .body(poster)
                    .exchange()
                    .expectStatus().isNoContent();

            restTestClient
                    .delete()
                    .uri("/{spexId}/poster", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNoContent();

            restTestClient
                    .get()
                    .uri("/{spexId}/logo", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_return_403_when_updating_not_permitted_due_to_insufficient_permission() throws Exception {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var poster = Files.readAllBytes(Paths.get(ResourceUtils.getFile("classpath:test.png").getPath()));

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{spexId}/poster", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                    .apiVersion("1.0")
                    .body(poster)
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
        void should_return_403_when_updating_not_permitted_due_to_insufficient_role() throws Exception {
            final var poster = Files.readAllBytes(Paths.get(ResourceUtils.getFile("classpath:test.png").getPath()));

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{spexId}/poster", 123L)
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                    .apiVersion("1.0")
                    .body(poster)
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
        void should_return_403_when_deleting_not_permitted_due_to_insufficient_permission() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{spexId}/poster", spex.getId())
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
        void should_return_403_when_deleting_not_permitted_due_to_insufficient_role() {
            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{spexId}/poster", 123L)
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
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
    @DisplayName("Revivals")
    class RevivalTests {

        @Test
        void should_return_parent_when_found() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var revival = persistRevival(randomizeRevival(spex));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, revival.getId()));

            final SpexDto result = restTestClient
                    .get()
                    .uri("/{id}/parent", revival.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(SpexDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "title", "year")
                    .contains(spex.getId(), spex.getDetails().getTitle(), spex.getYear());
        }

        @Test
        void should_return_404_when_parent_not_found() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var revival = persistRevival(randomizeRevival(spex));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, revival.getId()));

            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/{id}/parent", spex.getId())
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
        void should_return_found() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var revival = persistRevival(randomizeRevival(spex));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, revival.getId()));

            final SpexDto result = restTestClient
                    .get()
                    .uri("/{spexId}/revivals/{id}", spex.getId(), revival.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(SpexDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "title", "year")
                    .contains(revival.getId(), revival.getDetails().getTitle(), revival.getYear());
        }

        @Test
        void should_return_404_when_spex_not_found() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var revival = persistRevival(randomizeRevival(spex));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, revival.getId()));

            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/{spexId}/revivals/{id}", -1L, revival.getId())
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
        void should_return_404_when_incorrect_spex() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex1 = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex1.getId()));
            final var spex2 = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex2.getId()));
            final var revival = persistRevival(randomizeRevival(spex2));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, revival.getId()));

            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/{spexId}/revivals/{id}", spex1.getId(), revival.getId())
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
        void should_return_zero() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));

            final List<SpexDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri("/{spexId}/revivals", spex.getId())
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<SpexDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("spex");

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_404_when_non_existent_spex() {
            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/{spexId}/revivals", 1L)
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
        void should_return_one() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var revival = persistRevival(randomizeRevival(spex));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, revival.getId()));

            final List<SpexDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri("/{spexId}/revivals", spex.getId())
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<SpexDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("spex");

            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            IntStream.range(0, size).forEach(i -> {
                final var revival = persistRevival(randomizeRevival(spex));
                grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, revival.getId()));
            });

            final List<SpexDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .path("/{spexId}/revivals")
                                            .queryParam("size", size)
                                            .build(spex.getId())
                                    )
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<SpexDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("spex");

            assertThat(result).hasSize(size);
        }

        @Test
        void should_create_and_return_201() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            final SpexDto result = restTestClient
                    .post()
                    .uri("/{spexId}/revivals/{year}", spex.getId(), "2022")
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(SpexDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result)
                    .extracting("title", "year")
                    .contains(spex.getDetails().getTitle(), "2022");

            final List<SpexDto> after = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri("/{spexId}/revivals", spex.getId())
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<SpexDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("spex");

            assertThat(after).hasSize(1);
            assertThat(repository.count()).isEqualTo(2);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_adding_and_spex_not_found() {
            final ProblemDetail result = restTestClient
                    .post()
                    .uri("/{spexId}/revivals/{year}", 1L, "2022")
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_409_when_adding_and_year_already_exists() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var revival = persistRevival(randomizeRevival(spex));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spex.class, revival.getId()));

            final ProblemDetail result = restTestClient
                    .post()
                    .uri("/{spexId}/revivals/{year}", spex.getId(), revival.getYear())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(2);
            assertThat(detailsRepository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        }

        @Test
        void should_delete_and_return_204() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var revival = persistRevival(randomizeRevival(spex));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, revival.getId()));
            grantDeletePermissionToRoleAdmin(toObjectIdentity(Spex.class, revival.getId()));

            restTestClient
                    .delete()
                    .uri("/{spexId}/revivals/{id}", spex.getId(), revival.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_removing_and_spex_not_found() {
            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{spexId}/revivals/{id}", 1L, 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_removing_and_revival_not_found() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            grantDeletePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{spexId}/revivals/{id}", spex.getId(), -1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());

            final List<SpexDto> result2 = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri("/{spexId}/revivals", spex.getId())
                                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<SpexDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("spex");

            assertThat(result2).isEmpty();
            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_return_403_when_adding_not_permitted_due_to_insufficient_permission() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));

            final ProblemDetail result = restTestClient
                    .post()
                    .uri("/{spexId}/revivals/{year}", spex.getId(), "2022")
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_adding_not_permitted_due_to_insufficient_role() {
            final ProblemDetail result = restTestClient
                    .post()
                    .uri("/{spexId}/revivals/{year}", 1L, "2022")
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_deleting_not_permitted_due_to_insufficient_permission() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var revival = persistRevival(randomizeRevival(spex));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, revival.getId()));

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{spexId}/revivals/{id}", spex.getId(), revival.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(2);
            assertThat(detailsRepository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_deleting_not_permitted_due_to_insufficient_role() {
            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{spexId}/revivals/{id}", 1L, 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }
    }

    @Nested
    @DisplayName("Category")
    class CategoryTests {

        @Test
        void should_return_found() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));

            final SpexCategoryDto result = restTestClient
                    .get()
                    .uri("/{spexId}/category", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(SpexCategoryDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "name", "firstYear")
                    .contains(category.getId(), category.getName(), category.getFirstYear());
            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_not_found() {
            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/{spexId}/category", 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_add_and_return_204() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            restTestClient
                    .put()
                    .uri("/{spexId}/category/{id}", spex.getId(), category.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_adding_and_spex_not_found() {
            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{spexId}/category/{id}", 1L, 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_adding_and_category_not_found() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{spexId}/category/{id}", spex.getId(), -1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_remove_and_return_204() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            restTestClient
                    .delete()
                    .uri("/{spexId}/category", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_return_204_when_removing_and_no_category() {
            final var spex = persistSpex(randomizeSpex(null));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            restTestClient
                    .delete()
                    .uri("/{spexId}/category", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_removing_and_spex_not_found() {
            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{spexId}/category", 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_403_when_adding_not_permitted_due_to_insufficient_permission() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{spexId}/category/{id}", spex.getId(), category.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_adding_not_permitted_due_to_insufficient_role() {
            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{spexId}/category/{id}", 1L, 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_removing_not_permitted_due_to_insufficient_permission() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{spexId}/category", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(detailsRepository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_removing_not_permitted_due_to_insufficient_role() {
            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{spexId}/category", 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(detailsRepository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

    }


    @Nested
    @DisplayName("Events")
    class EventTests {

        @Test
        void should_return_found() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            final List<EventDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri("/events/{sourceId}", spex.getId())
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

            assertThat(eventRepository.count()).isEqualTo(2);
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getEventType()).isEqualTo(Event.EventType.CREATE.name());
            assertThat(result.getFirst().getSourceType()).isEqualTo(Event.SourceType.SPEX.name());
            assertThat(result.getFirst().getCreatedBy()).isEqualTo(spex.getCreatedBy());
        }
    }

    private Spex randomizeSpex(@Nullable final SpexCategory category) {
        final var spex = random.nextObject(Spex.class);
        spex.setParent(null);
        final var details = random.nextObject(SpexDetails.class);
        details.setCategory(category);
        spex.setDetails(details);
        return spex;
    }

    private Spex randomizeRevival(final Spex parent) {
        final var revival = random.nextObject(Spex.class);
        revival.setParent(parent);
        revival.setDetails(parent.getDetails());
        return revival;
    }

    private SpexCategory randomizeSpexCategory() {
        return random.nextObject(SpexCategory.class);
    }

    private Spex persistSpex(final Spex spex) {
        spex.setId(null);
        spex.getDetails().setId(null);
        final var details = detailsRepository.save(spex.getDetails());
        spex.setDetails(details);
        return repository.save(spex);
    }

    private Spex persistRevival(final Spex spex) {
        spex.setId(null);

        return repository.save(spex);
    }

    private SpexCategory persistSpexCategory(final SpexCategory category) {
        category.setId(null);

        return categoryRepository.save(category);
    }
}
