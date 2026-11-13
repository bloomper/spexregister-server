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

import nu.fgv.register.server.spex.category.SpexCategory;
import nu.fgv.register.server.spex.category.SpexCategoryDto;
import nu.fgv.register.server.spex.category.SpexCategoryRepository;
import nu.fgv.register.server.util.AbstractAuditable;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.HalEmbeddedResponse;
import nu.fgv.register.server.util.filter.FilterOperation;
import nu.fgv.register.server.util.randomizer.RandomizerSupport;
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
class SpexApiIntegrationTest extends AbstractIntegrationTest {

    private final EasyRandom random;
    private final SpexRepository repository;
    private final SpexDetailsRepository detailsRepository;
    private final SpexCategoryRepository categoryRepository;
    private final ResourceLoader resourceLoader;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public SpexApiIntegrationTest(final JdbcClient jdbcClient,
                                  final Keycloak keycloakAdminClient,
                                  final String keycloakClientId,
                                  final SpexRepository repository,
                                  final SpexDetailsRepository detailsRepository,
                                  final SpexCategoryRepository categoryRepository,
                                  final ObjectMapper objectMapper,
                                  final ResourceLoader resourceLoader) {
        super(jdbcClient, keycloakAdminClient, keycloakClientId, objectMapper);
        this.repository = repository;
        this.detailsRepository = detailsRepository;
        this.categoryRepository = categoryRepository;
        this.resourceLoader = resourceLoader;

        final EasyRandomParameters parameters = RandomizerSupport.parameters();

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
        JdbcTestUtils.deleteFromTables(jdbcClient, "spex", "spex_details", "spex_category", "spex_audit", "spex_details_audit", "spex_category_audit");
    }

    @AfterEach
    void tearDown() {
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
            final var spex = persistSpex(randomizeSpex(category));

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
            IntStream.range(0, size).forEach(i -> {
                final var spex = persistSpex(randomizeSpex(category));
                if (i % 2 == 0) {
                    final var revival = randomizeRevival(spex);
                    persistRevival(revival);
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
            final var spex = persistSpex(randomizeSpex(category));

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
            final var spex = persistSpex(randomizeSpex(category));

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
            IntStream.range(0, size).forEach(i -> {
                final var spex = randomizeSpex(category);
                if (i % 2 == 0) {
                    spex.setYear("1996");
                } else {
                    spex.setYear("1997");
                }
                final var spex0 = persistSpex(spex);
                if (i % 4 == 0) {
                    final var revival = randomizeRevival(spex0);
                    persistRevival(revival);
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
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        void should_return_found() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            final var spex = persistSpex(randomizeSpex(category));

            final SpexDto result = restTestClient
                    .get()
                    .uri("/{id}", spex.getUuid())
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
                    .contains(spex.getUuid(), spex.getDetails().getTitle(), spex.getYear());
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

    // ... existing code ...
}
