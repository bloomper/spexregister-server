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

package nu.fgv.register.server.settings;

import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.HalEmbeddedResponse;
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
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.ApiVersionInserter;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class SettingsApiIntegrationTest extends AbstractIntegrationTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public SettingsApiIntegrationTest(final JdbcClient jdbcClient,
                                      final AclCache aclCache,
                                      final Keycloak keycloakAdminClient,
                                      final String keycloakClientId,
                                      final PermissionService permissionService,
                                      final ObjectMapper objectMapper) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
    }

    @BeforeEach
    void setUp() {
        restTestClient = RestTestClient
                .bindToServer()
                .baseUrl("http://localhost:%s/api/settings".formatted(localPort))
                .apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
                .build();
    }

    @AfterEach
    void tearDown() {
    }

    @Nested
    @DisplayName("Retrieve language(s)")
    class RetrieveLanguageTests {

        @Test
        void should_return_many() {
            final List<LanguageDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri("/languages")
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<LanguageDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("languages");

            assertThat(result).hasSize(2);
        }

        @Test
        void should_return_found() {
            final LanguageDto result = restTestClient
                    .get()
                    .uri("/languages/{isoCode}", "sv")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(LanguageDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("isoCode", "label")
                    .contains("sv", "Svenska");
        }

        @Test
        void should_return_found_in_sv() {
            final LanguageDto result = restTestClient
                    .get()
                    .uri("/languages/{isoCode}", "sv")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "sv")
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(LanguageDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("isoCode", "label")
                    .contains("sv", "Svenska");
        }

        @Test
        void should_return_found_in_en() {
            final LanguageDto result = restTestClient
                    .get()
                    .uri("/languages/{isoCode}", "sv")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(LanguageDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("isoCode", "label")
                    .contains("sv", "Swedish");
        }

        @Test
        void should_return_404_when_not_found() {
            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/languages/{isoCode}", "123")
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
    @DisplayName("Retrieve country(ies)")
    class RetrieveCountryTests {

        @Test
        void should_return_many() {
            final List<CountryDto> result = Objects.requireNonNull(restTestClient
                            .get()
                            .uri("/countries")
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .apiVersion("1.0")
                            .exchange()
                            .expectStatus().isOk()
                            .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<CountryDto>>() {
                            })
                            .returnResult()
                            .getResponseBody())
                    .getList("countries");

            assertThat(result).hasSize(249);
        }

        @Test
        void should_return_found() {
            final CountryDto result = restTestClient
                    .get()
                    .uri("/countries/{isoCode}", "SE")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(CountryDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("isoCode", "label")
                    .contains("SE", "Sverige");
        }

        @Test
        void should_return_found_in_sv() {
            final CountryDto result = restTestClient
                    .get()
                    .uri("/countries/{isoCode}", "SE")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "sv")
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(CountryDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("isoCode", "label")
                    .contains("SE", "Sverige");
        }

        @Test
        void should_return_found_in_en() {
            final CountryDto result = restTestClient
                    .get()
                    .uri("/countries/{isoCode}", "SE")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(CountryDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("isoCode", "label")
                    .contains("SE", "Sweden");
        }

        @Test
        void should_return_404_when_not_found() {
            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/countries/{isoCode}", "123")
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
    @DisplayName("Retrieve type(s)")
    class RetrieveTypeTests {

        @Test
        void should_return_many() {
            final List<TypeDto> result = Objects.requireNonNull(restTestClient
                            .get()
                            .uri("/types")
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .apiVersion("1.0")
                            .exchange()
                            .expectStatus().isOk()
                            .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<TypeDto>>() {
                            })
                            .returnResult()
                            .getResponseBody())
                    .getList("types");

            assertThat(result).hasSize(17);
        }

        @Test
        void should_return_many_of_type() {
            final List<TypeDto> result = Objects.requireNonNull(restTestClient
                            .get()
                            .uri("/types/{type}", TypeType.ADDRESS)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .apiVersion("1.0")
                            .exchange()
                            .expectStatus().isOk()
                            .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<TypeDto>>() {
                            })

                            .returnResult()
                            .getResponseBody())
                    .getList("types");

            assertThat(result).hasSize(3);
        }

        @Test
        void should_return_400_when_unknown_type() {
            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/types/{type}", "whatever")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void should_return_found() {
            final TypeDto result = restTestClient
                    .get()
                    .uri("/types/{type}/{id}", TypeType.ADDRESS, "HOME")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(TypeDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "label", "type")
                    .contains("HOME", "Hem", TypeType.ADDRESS);
        }

        @Test
        void should_return_found_in_sv() {
            final TypeDto result = restTestClient
                    .get()
                    .uri("/types/{type}/{id}", TypeType.ADDRESS, "HOME")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "sv")
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(TypeDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "label", "type")
                    .contains("HOME", "Hem", TypeType.ADDRESS);
        }

        @Test
        void should_return_found_in_en() {
            final TypeDto result = restTestClient
                    .get()
                    .uri("/types/{type}/{id}", TypeType.ADDRESS, "HOME")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(TypeDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "label", "type")
                    .contains("HOME", "Home", TypeType.ADDRESS);
        }

        @Test
        void should_return_404_when_not_found() {
            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/types/{type}/{id}", TypeType.ADDRESS, "whatever")
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
