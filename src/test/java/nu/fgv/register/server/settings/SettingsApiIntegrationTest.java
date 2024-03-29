package nu.fgv.register.server.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static org.assertj.core.api.Assertions.assertThat;

class SettingsApiIntegrationTest extends AbstractIntegrationTest {

    private static String basePath;
    @LocalServerPort
    private int localPort;

    @Autowired
    private ObjectMapper objectMapper;

    public SettingsApiIntegrationTest() {
        final EasyRandomParameters parameters = new EasyRandomParameters();
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
            given()
                .contentType(ContentType.JSON)
            .when()
                .get("/languages/{isoCode}", "123")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
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
            given()
                .contentType(ContentType.JSON)
            .when()
                .get("/countries/{isoCode}", "123")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
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

            assertThat(result).hasSize(19);
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
            given()
                .contentType(ContentType.JSON)
            .when()
                .get("/types/{type}", "whatever")
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
            //@formatter:on
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
            given()
                .contentType(ContentType.JSON)
            .when()
                .get("/types/{type}/{id}", TypeType.ADDRESS, 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }
    }
}
