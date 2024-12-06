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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.util.AbstractIntegrationTest;
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
import org.springframework.security.acls.model.AclCache;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
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

    @BeforeAll
    public static void beforeClass() {
        basePath = SettingsApi.class.getAnnotation(RequestMapping.class).value()[0];
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
    }

    @AfterEach
    void tearDown() {
        RestAssured.reset();
    }

    @Nested
    @DisplayName("Retrieve language(s)")
    class RetrieveLanguageTests {

        @Test
        void should_return_many() {
            //@formatter:off
            final List<LanguageDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/languages")
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.languages", LanguageDto.class);
            //@formatter:on

            assertThat(result).hasSize(2);
        }

        @Test
        void should_return_found() {
            //@formatter:off
            final LanguageDto result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/languages/{isoCode}", "sv")
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(LanguageDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("isoCode", "label")
                    .contains("sv", "Svenska");
        }

        @Test
        void should_return_found_in_sv() {
            //@formatter:off
            final LanguageDto result =
                    given()
                        .contentType(ContentType.JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "sv")
                    .when()
                        .get("/languages/{isoCode}", "sv")
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(LanguageDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("isoCode", "label")
                    .contains("sv", "Svenska");
        }

        @Test
        void should_return_found_in_en() {
            //@formatter:off
            final LanguageDto result =
                    given()
                            .contentType(ContentType.JSON)
                            .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                            .when()
                            .get("/languages/{isoCode}", "sv")
                            .then()
                            .statusCode(HttpStatus.OK.value())
                            .extract().body().as(LanguageDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("isoCode", "label")
                    .contains("sv", "Swedish");
        }

        @Test
        void should_return_404_when_not_found() {
            //@formatter:off
            final ProblemDetail result = given()
                .contentType(ContentType.JSON)
            .when()
                .get("/languages/{isoCode}", "123")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }

    @Nested
    @DisplayName("Retrieve country(ies)")
    class RetrieveCountryTests {

        @Test
        void should_return_many() {
            //@formatter:off
            final List<CountryDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/countries")
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.countries", CountryDto.class);
            //@formatter:on

            assertThat(result).hasSize(249);
        }

        @Test
        void should_return_found() {
            //@formatter:off
            final CountryDto result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/countries/{isoCode}", "SE")
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(CountryDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("isoCode", "label")
                    .contains("SE", "Sverige");
        }

        @Test
        void should_return_found_in_sv() {
            //@formatter:off
            final CountryDto result =
                    given()
                        .contentType(ContentType.JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "sv")
                    .when()
                        .get("/countries/{isoCode}", "SE")
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(CountryDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("isoCode", "label")
                    .contains("SE", "Sverige");
        }

        @Test
        void should_return_found_in_en() {
            //@formatter:off
            final CountryDto result =
                    given()
                        .contentType(ContentType.JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                    .when()
                        .get("/countries/{isoCode}", "SE")
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(CountryDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("isoCode", "label")
                    .contains("SE", "Sweden");
        }

        @Test
        void should_return_404_when_not_found() {
            //@formatter:off
            final ProblemDetail result = given()
                .contentType(ContentType.JSON)
            .when()
                .get("/countries/{isoCode}", "123")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }

    @Nested
    @DisplayName("Retrieve type(s)")
    class RetrieveTypeTests {

        @Test
        void should_return_many() {
            //@formatter:off
            final List<TypeDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/types")
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.types", TypeDto.class);
            //@formatter:on

            assertThat(result).hasSize(17);
        }

        @Test
        void should_return_many_of_type() {
            //@formatter:off
            final List<TypeDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/types/{type}", TypeType.ADDRESS)
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.types", TypeDto.class);
            //@formatter:on

            assertThat(result).hasSize(3);
        }

        @Test
        void should_return_400_when_unknown_type() {
            //@formatter:off
            final ProblemDetail result = given()
                .contentType(ContentType.JSON)
            .when()
                .get("/types/{type}", "whatever")
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void should_return_found() {
            //@formatter:off
            final TypeDto result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/types/{type}/{id}", TypeType.ADDRESS, "HOME")
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(TypeDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "label", "type")
                    .contains("HOME", "Hem", TypeType.ADDRESS);
        }

        @Test
        void should_return_found_in_sv() {
            //@formatter:off
            final TypeDto result =
                    given()
                        .contentType(ContentType.JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "sv")
                    .when()
                        .get("/types/{type}/{id}", TypeType.ADDRESS, "HOME")
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(TypeDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "label", "type")
                    .contains("HOME", "Hem", TypeType.ADDRESS);
        }

        @Test
        void should_return_found_in_en() {
            //@formatter:off
            final TypeDto result =
                    given()
                        .contentType(ContentType.JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                    .when()
                        .get("/types/{type}/{id}", TypeType.ADDRESS, "HOME")
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(TypeDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "label", "type")
                    .contains("HOME", "Home", TypeType.ADDRESS);
        }

        @Test
        void should_return_404_when_not_found() {
            //@formatter:off
            final ProblemDetail result = given()
                .contentType(ContentType.JSON)
            .when()
                .get("/types/{type}/{id}", TypeType.ADDRESS, "whatever")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }
}
