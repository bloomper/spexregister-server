package nu.fgv.register.server.spexare.toggle;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.settings.TypeRepository;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.randomizer.SocialSecurityNumberRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.inClass;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;

@Disabled
class ToggleApiIntegrationTest extends AbstractIntegrationTest {

    private static String basePath;
    private final EasyRandom random;
    @LocalServerPort
    private int localPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ToggleRepository repository;

    @Autowired
    private TypeRepository typeRepository;

    @Autowired
    private SpexareRepository spexareRepository;

    public ToggleApiIntegrationTest() {
        final EasyRandomParameters parameters = new EasyRandomParameters();
        parameters
                .randomize(
                        named("socialSecurityNumber"), new SocialSecurityNumberRandomizer()
                )
                .excludeField(named("partner").and(ofType(Spexare.class)).and(inClass(Spexare.class)))
                .excludeField(named("user").and(ofType(User.class)).and(inClass(Spexare.class)))
                .excludeField(named("activities").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("tags").and(ofType(Set.class)).and(inClass(Spexare.class)))
                .excludeField(named("addresses").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("memberships").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("consents").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("toggles").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("tags").and(ofType(List.class)).and(inClass(Spexare.class)))
                .randomizationDepth(1);
        random = new EasyRandom(parameters);
    }

    @BeforeAll
    public static void beforeClass() {
        basePath = ToggleApi.class.getAnnotation(RequestMapping.class).value()[0];
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

        repository.deleteAll();
        spexareRepository.deleteAll();
        typeRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        RestAssured.reset();
    }

    @Nested
    @DisplayName("Retrieve paged")
    class RetrievePagedTests {

        @Test
        void should_return_404() {
            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId",1L)
            .when()
                .get()
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return_zero() {
            var spexare = persistSpexare(randomizeSpexare());

            //@formatter:off
            final List<ToggleDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.toggles", ToggleDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_one() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            persistToggle(randomizeToggle(type, spexare));

            //@formatter:off
            final List<ToggleDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.toggles", ToggleDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            int size = 42;
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            IntStream.range(0, size).forEach(i -> persistToggle(randomizeToggle(type, spexare)));

            //@formatter:off
            final List<ToggleDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                        .queryParam("size", size)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.toggles", ToggleDto.class);
            //@formatter:on

            assertThat(result).hasSize(size);
        }

    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        void should_return_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            var toggle = persistToggle(randomizeToggle(type, spexare));

            //@formatter:off
            final ToggleDto result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .get("/{id}", toggle.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(ToggleDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "value")
                    .contains(toggle.getId(), toggle.getValue());
        }

        @Test
        void should_return_404_when_not_found() {
            var spexare = persistSpexare(randomizeSpexare());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .get("/{id}", 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return_404_when_spexare_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            var toggle = persistToggle(randomizeToggle(type, spexare));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
            .when()
                .get("/{id}", toggle.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }
    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create_and_return_201() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .post("/{typeId}/{value}", type.getId(), Boolean.TRUE)
            .then()
                .statusCode(HttpStatus.CREATED.value());
            //@formatter:on

            //@formatter:off
            final List<ToggleDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.toggles", ToggleDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_409_when_creating_already_existing_value() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .post("/{typeId}/{value}", type.getId(), Boolean.TRUE)
            .then()
                .statusCode(HttpStatus.CREATED.value());
            //@formatter:on

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .post("/{typeId}/{value}", type.getId(), Boolean.FALSE)
            .then()
                .statusCode(HttpStatus.CONFLICT.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_creating_and_spexare_not_found() {
            var type = persistType(randomizeType());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
            .when()
                .post("/{typeId}/{value}", type.getId(), Boolean.TRUE)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return_404_when_creating_and_type_not_found() {
            var spexare = persistSpexare(randomizeSpexare());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .post("/{typeId}/{value}", "dummy", Boolean.TRUE)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

    }

    @Nested
    @DisplayName("Update")
    class UpdateTests {

        @Test
        void should_update_and_return_202() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            var toggle = persistToggle(randomizeToggle(type, spexare));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .put("/{typeId}/{id}/{value}", type.getId(), toggle.getId(), Boolean.FALSE)
            .then()
                .statusCode(HttpStatus.ACCEPTED.value());
            //@formatter:on

            //@formatter:off
            final List<ToggleDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.toggles", ToggleDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
            assertThat(result.getFirst())
                    .extracting("id", "value")
                    .contains(toggle.getId(), Boolean.FALSE);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_updating_non_existing_value() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .put("/{typeId}/{id}/{value}", type.getId(), 1L, Boolean.TRUE)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_updating_and_spexare_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            var toggle = persistToggle(randomizeToggle(type, spexare));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
            .when()
                .put("/{typeId}/{id}/{value}", type.getId(), toggle.getId(), Boolean.TRUE)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_updating_and_type_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            var toggle = persistToggle(randomizeToggle(type, spexare));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .put("/{typeId}/{id}/{value}", "dummy", toggle.getId(), Boolean.TRUE)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_422_when_updating_and_incorrect_spexare() {
            var spexare1 = persistSpexare(randomizeSpexare());
            var spexare2 = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            var toggle = persistToggle(randomizeToggle(type, spexare2));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
            .when()
                .put("/{typeId}/{id}/{value}", type.getId(), toggle.getId(), Boolean.TRUE)
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        void should_delete_and_return_204() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            var toggle = persistToggle(randomizeToggle(type, spexare));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .delete("/{typeId}/{id}", type.getId(), toggle.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            //@formatter:off
            final List<ToggleDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.toggles", ToggleDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_deleting_non_existing_value() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .delete("/{typeId}/{id}", type.getId(), 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_deleting_and_spexare_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            var toggle = persistToggle(randomizeToggle(type, spexare));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", -1L)
            .when()
                .delete("/{typeId}/{id}", type.getId(), toggle.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_deleting_and_type_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            var toggle = persistToggle(randomizeToggle(type, spexare));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .delete("/{typeId}/{id}", "dummy", toggle.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_422_when_deleting_and_incorrect_spexare() {
            var spexare1 = persistSpexare(randomizeSpexare());
            var spexare2 = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            var toggle = persistToggle(randomizeToggle(type, spexare2));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
            .when()
                .delete("/{typeId}/{id}", type.getId(), toggle.getId())
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

    }

    private Toggle randomizeToggle(Type type, Spexare spexare) {
        var toggle = random.nextObject(Toggle.class);
        toggle.setSpexare(spexare);
        toggle.setType(type);
        return toggle;
    }

    private Toggle persistToggle(Toggle toggle) {
        return repository.save(toggle);
    }

    private Type randomizeType() {
        var type = random.nextObject(Type.class);
        type.setType(TypeType.TOGGLE);
        return type;
    }

    private Type persistType(Type type) {
        return typeRepository.save(type);
    }

    private Spexare randomizeSpexare() {
        return random.nextObject(Spexare.class);
    }

    private Spexare persistSpexare(Spexare spexare) {
        return spexareRepository.save(spexare);
    }

}
