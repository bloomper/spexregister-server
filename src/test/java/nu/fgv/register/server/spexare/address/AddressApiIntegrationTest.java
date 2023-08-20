package nu.fgv.register.server.spexare.address;

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
import org.jeasy.random.randomizers.EmailRandomizer;
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
import java.util.Set;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.inClass;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;

public class AddressApiIntegrationTest extends AbstractIntegrationTest {

    private static String basePath;
    private final EasyRandom random;
    @LocalServerPort
    private int localPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AddressRepository repository;

    @Autowired
    private TypeRepository typeRepository;

    @Autowired
    private SpexareRepository spexareRepository;

    public AddressApiIntegrationTest() {
        final EasyRandomParameters parameters = new EasyRandomParameters();
        parameters
                .randomize(
                        named("emailAddress"), new EmailRandomizer()
                )
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
        basePath = AddressApi.class.getAnnotation(RequestMapping.class).value()[0];
    }

    @BeforeEach
    public void setUp() {
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
    public void tearDown() {
        RestAssured.reset();
    }

    @Nested
    @DisplayName("Retrieve paged")
    class RetrievePagedTests {

        @Test
        public void should_return_404() {
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
        public void should_return_zero() {
            var spexare = persistSpexare(randomizeSpexare());

            //@formatter:off
            final List<AddressDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.addresses", AddressDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        public void should_return_one() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            persistAddress(randomizeAddress(type, spexare));

            //@formatter:off
            final List<AddressDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.addresses", AddressDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        public void should_return_many() {
            int size = 42;
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            IntStream.range(0, size).forEach(i -> persistAddress(randomizeAddress(type, spexare)));

            //@formatter:off
            final List<AddressDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .queryParam("size", size)
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.addresses", AddressDto.class);
            //@formatter:on

            assertThat(result).hasSize(size);
        }

    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        public void should_return_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            var address = persistAddress(randomizeAddress(type, spexare));

            //@formatter:off
            final AddressDto result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .get("/{id}", address.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(AddressDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "streetAddress")
                    .contains(address.getId(), address.getStreetAddress());
        }

        @Test
        public void should_return_404_when_not_found() {
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
        public void should_return_404_when_spexare_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            var address = persistAddress(randomizeAddress(type, spexare));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
            .when()
                .get("/{id}", address.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }
    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        public void should_create_and_return_201() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            var dto = random.nextObject(AddressCreateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .body(dto)
            .when()
                .post("/{typeId}", type.getId())
            .then()
                .statusCode(HttpStatus.CREATED.value());
            //@formatter:on

            //@formatter:off
            final List<AddressDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.addresses", AddressDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_409_when_creating_already_existing_value() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            var dto = random.nextObject(AddressCreateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .body(dto)
            .when()
                .post("/{typeId}", type.getId())
            .then()
                .statusCode(HttpStatus.CREATED.value());
            //@formatter:on

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .body(dto)
            .when()
                .post("/{typeId}", type.getId())
            .then()
                .statusCode(HttpStatus.CONFLICT.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_404_when_creating_and_spexare_not_found() {
            var type = persistType(randomizeType());
            var dto = random.nextObject(AddressCreateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
                .body(dto)
            .when()
                .post("/{typeId}", type.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(0);
        }

        @Test
        public void should_return_404_when_creating_and_type_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var dto = random.nextObject(AddressCreateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .body(dto)
            .when()
                .post("/{typeId}", 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(0);
        }

    }

    @Nested
    @DisplayName("Update")
    class UpdateTests {

        @Test
        public void should_update_and_return_202() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            var dto = random.nextObject(AddressCreateDto.class);

            //@formatter:off
            final AddressDto before = given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .body(dto)
            .when()
                .post("/{typeId}", type.getId())
            .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract().body().as(AddressDto.class);
            //@formatter:on

            var updateDto = AddressUpdateDto.builder().id(before.getId()).streetAddress(before.getStreetAddress() + "_")
                    .postalCode(before.getPostalCode()).city(before.getCity()).country(before.getCountry())
                    .phone(before.getPhone()).phoneMobile(dto.getPhoneMobile()).emailAddress(before.getEmailAddress()).build();

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .body(updateDto)
            .when()
                .put("/{typeId}/{id}", type.getId(), before.getId())
            .then()
                .statusCode(HttpStatus.ACCEPTED.value());
            //@formatter:on

            //@formatter:off
            final List<AddressDto> after =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.addresses", AddressDto.class);
            //@formatter:on

            assertThat(after).hasSize(1);
            assertThat(after.get(0))
                    .extracting("id", "streetAddress")
                    .contains(before.getId(), updateDto.getStreetAddress());
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_404_when_updating_non_existing_value() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            var dto = random.nextObject(AddressUpdateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .body(dto)
            .when()
                .put("/{typeId}/{id}", type.getId(), dto.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(0);
        }

        @Test
        public void should_return_404_when_updating_and_spexare_not_found() {
            var type = persistType(randomizeType());
            var dto = random.nextObject(AddressUpdateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
                .body(dto)
            .when()
                .put("/{typeId}/{id}", type.getId(), dto.getId())
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(0);
        }

        @Test
        public void should_return_404_when_updating_and_type_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var dto = random.nextObject(AddressUpdateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .body(dto)
            .when()
                .put("/{typeId}/{id}", "dummy", dto.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(0);
        }

        @Test
        public void should_return_422_when_updating_and_incorrect_spexare() {
            var spexare1 = persistSpexare(randomizeSpexare());
            var spexare2 = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            var address = persistAddress(randomizeAddress(type, spexare2));
            var dto = random.nextObject(AddressUpdateDto.class);
            dto.setId(address.getId());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
                .body(dto)
            .when()
                .put("/{typeId}/{id}", type.getId(), dto.getId())
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Partial update")
    class PartialUpdateTests {

        @Test
        public void should_update_and_return_202() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            var dto = random.nextObject(AddressCreateDto.class);

            //@formatter:off
            final AddressDto result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                        .body(dto)
                    .when()
                        .post("/{typeId}", type.getId())
                    .then()
                        .statusCode(HttpStatus.CREATED.value())
                        .extract().body().as(AddressDto.class);
            //@formatter:on

            var updateDto = AddressUpdateDto.builder().id(result.getId()).streetAddress(result.getStreetAddress() + "_").build();

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .body(updateDto)
            .when()
                .patch("/{typeId}/{id}", type.getId(), result.getId())
            .then()
                .statusCode(HttpStatus.ACCEPTED.value());
            //@formatter:on

            //@formatter:off
            final List<AddressDto> after =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.addresses", AddressDto.class);
            //@formatter:on

            assertThat(after).hasSize(1);
            assertThat(after.get(0))
                    .extracting("id", "streetAddress", "city")
                    .contains(result.getId(), updateDto.getStreetAddress(), dto.getCity());
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_404_when_updating_non_existing_value() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            var dto = random.nextObject(AddressUpdateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .body(dto)
            .when()
                .patch("/{typeId}/{id}", type.getId(), dto.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(0);
        }

        @Test
        public void should_return_404_when_updating_and_spexare_not_found() {
            var type = persistType(randomizeType());
            var dto = random.nextObject(AddressUpdateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1L)
                .body(dto)
            .when()
                .patch("/{typeId}/{id}", type.getId(), dto.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(0);
        }

        @Test
        public void should_return_404_when_updating_and_type_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var dto = random.nextObject(AddressUpdateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
                .body(dto)
            .when()
                .patch("/{typeId}/{id}", "dummy", dto.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(0);
        }

        @Test
        public void should_return_422_when_updating_and_incorrect_spexare() {
            var spexare1 = persistSpexare(randomizeSpexare());
            var spexare2 = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            var address = persistAddress(randomizeAddress(type, spexare2));
            var dto = random.nextObject(AddressUpdateDto.class);
            dto.setId(address.getId());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
                .body(dto)
            .when()
                .patch("/{typeId}/{id}", type.getId(), dto.getId())
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
        public void should_delete_and_return_204() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            var address = persistAddress(randomizeAddress(type, spexare));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .delete("/{typeId}/{id}", type.getId(), address.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            //@formatter:off
            final List<AddressDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.addresses", AddressDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
            assertThat(repository.count()).isEqualTo(0);
        }

        @Test
        public void should_return_404_when_deleting_non_existing_value() {
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

            assertThat(repository.count()).isEqualTo(0);
        }

        @Test
        public void should_return_404_when_deleting_and_spexare_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            var address = persistAddress(randomizeAddress(type, spexare));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", -1L)
            .when()
                .delete("/{typeId}/{id}", type.getId(), address.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_404_when_deleting_and_type_not_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            var address = persistAddress(randomizeAddress(type, spexare));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .delete("/{typeId}/{id}", "dummy", address.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_422_when_deleting_and_incorrect_spexare() {
            var spexare1 = persistSpexare(randomizeSpexare());
            var spexare2 = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            var address = persistAddress(randomizeAddress(type, spexare2));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare1.getId())
            .when()
                .delete("/{typeId}/{id}", type.getId(), address.getId())
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }
    }

    private Address randomizeAddress(Type type, Spexare spexare) {
        var address = random.nextObject(Address.class);
        address.setSpexare(spexare);
        address.setType(type);
        return address;
    }

    private Address persistAddress(Address address) {
        return repository.save(address);
    }

    private Type randomizeType() {
        var type = random.nextObject(Type.class);
        type.setType(TypeType.ADDRESS);
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
