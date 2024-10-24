package nu.fgv.register.server.spexare;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventRepository;
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
import org.springframework.http.MediaType;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.file.Files;
import java.nio.file.Paths;
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
class SpexareApiIntegrationTest extends AbstractIntegrationTest {

    private static String basePath;
    private final EasyRandom random;
    @LocalServerPort
    private int localPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpexareRepository repository;

    @Autowired
    private EventRepository eventRepository;

    public SpexareApiIntegrationTest() {
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
        basePath = SpexareApi.class.getAnnotation(RequestMapping.class).value()[0];
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
        eventRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        RestAssured.reset();
    }

    @Nested
    @DisplayName("Retrieve paged")
    class RetrievePagedTests {

        @Test
        void should_return_zero() {
            //@formatter:off
            final List<SpexareDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spexare", SpexareDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_one() {
            persistSpexare(randomizeSpexare());

            //@formatter:off
            final List<SpexareDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spexare", SpexareDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            int size = 42;
            IntStream.range(0, size).forEach(i -> persistSpexare(randomizeSpexare()));

            //@formatter:off
            final List<SpexareDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .queryParam("size", size)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spexare", SpexareDto.class);
            //@formatter:on

            assertThat(result).hasSize(size);
        }

    }

    @Nested
    @DisplayName("Retrieve paged with filtering")
    class RetrieveWithFilteringPagedTests {

        @Test
        void should_return_zero() {
            persistSpexare(randomizeSpexare());

            //@formatter:off
            final List<SpexareDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .queryParam("filter", Spexare_.FIRST_NAME + ":whatever")
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spexare", SpexareDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_one() {
            var spexare = persistSpexare(randomizeSpexare());

            //@formatter:off
            final List<SpexareDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .queryParam("filter", Spexare_.FIRST_NAME + ":" + spexare.getFirstName())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spexare", SpexareDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            int size = 42;
            IntStream.range(0, size).forEach(i -> {
                var spexare = randomizeSpexare();
                if (i % 2 == 0) {
                    spexare.setFirstName("whatever");
                }
                persistSpexare(spexare);
            });

            //@formatter:off
            final List<SpexareDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .queryParam("filter", Spexare_.FIRST_NAME + ":whatever")
                        .queryParam("size", size)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spexare", SpexareDto.class);
            //@formatter:on

            assertThat(result).hasSize(size / 2);
        }

    }

    @Nested
    @DisplayName("Search paged")
    class SearchPagedTests {

        @Test
        void should_return_zero() {
            //@formatter:off
            final List<SpexareDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .queryParam("q", "whatever")
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spexare", SpexareDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_one() {
            var spexare = persistSpexare(randomizeSpexare());

            //@formatter:off
            final List<SpexareDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()

                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spexare", SpexareDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            int size = 42;
            IntStream.range(0, size).forEach(i -> {
                var spexare = persistSpexare(randomizeSpexare());
                spexare.setFirstName("firstName");
                persistSpexare(spexare);
            });

            //@formatter:off
            final List<SpexareDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .queryParam("size", size)
                        .queryParam("q", "firstName")
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spexare", SpexareDto.class);
            //@formatter:on

            assertThat(result).hasSize(size);
        }

    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create_and_return_201() throws Exception {
            final SpexareCreateDto dto = random.nextObject(SpexareCreateDto.class);

            //@formatter:off
            final String json =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .body(dto)
                    .when()
                        .post()
                    .then()
                        .statusCode(HttpStatus.CREATED.value())
                        .extract().body().asString();
            //@formatter:on

            final SpexareDto result = objectMapper.readValue(json, SpexareDto.class);
            assertThat(result)
                    .extracting("firstName", "lastName", "nickName")
                    .contains(dto.getFirstName(), dto.getLastName(), dto.getNickName());
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_400_when_invalid_input() {
            final SpexareCreateDto dto = random.nextObject(SpexareCreateDto.class);
            dto.setLastName(null);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .body(dto)
            .when()
                .post()
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        void should_return_found() {
            var spexare = persistSpexare(randomizeSpexare());

            //@formatter:off
            final SpexareDto result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", spexare.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(SpexareDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "firstName", "lastName", "nickName")
                    .contains(spexare.getId(), spexare.getFirstName(), spexare.getLastName(), spexare.getNickName());
        }

        @Test
        void should_return_404_when_not_found() {
            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .get("/{id}", 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }
    }

    @Nested
    @DisplayName("Update")
    class UpdateTests {

        @Test
        void should_update_and_return_202() throws Exception {
            var spexare = persistSpexare(randomizeSpexare());

            //@formatter:off
            final SpexareDto before =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", spexare.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(SpexareDto.class);
            //@formatter:on

            final SpexareUpdateDto dto = SpexareUpdateDto.builder()
                    .id(before.getId())
                    .firstName(before.getFirstName() + "_")
                    .lastName(before.getLastName())
                    .nickName(before.getNickName())
                    .build();

            //@formatter:off
            final String json =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .body(dto)
                    .when()
                        .put("/{id}", spexare.getId())
                    .then()
                        .statusCode(HttpStatus.ACCEPTED.value())
                        .extract().body().asString();
            //@formatter:on

            final SpexareDto updated = objectMapper.readValue(json, SpexareDto.class);

            //@formatter:off
            final SpexareDto after =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", spexare.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(SpexareDto.class);
            //@formatter:on

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_400_when_invalid_input() {
            final SpexareUpdateDto dto = random.nextObject(SpexareUpdateDto.class);
            dto.setFirstName(null);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .body(dto)
            .when()
                .put("/{id}", dto.getId())
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_400_when_invalid_social_security_number() {
            final SpexareUpdateDto dto = random.nextObject(SpexareUpdateDto.class);
            dto.setSocialSecurityNumber("20120606-4658");

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .body(dto)
            .when()
                .put("/{id}", dto.getId())
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_not_found() {
            final SpexareUpdateDto dto = random.nextObject(SpexareUpdateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .body(dto)
            .when()
                .put("/{id}", dto.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Partial update")
    class PartialUpdateTests {

        @Test
        void should_update_and_return_202() throws Exception {
            var spexare = persistSpexare(randomizeSpexare());

            //@formatter:off
            final SpexareDto before =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", spexare.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                    .extract().body().as(SpexareDto.class);
            //@formatter:on

            final SpexareUpdateDto dto = SpexareUpdateDto.builder()
                    .id(before.getId())
                    .firstName(before.getFirstName() + "_")
                    .lastName(before.getLastName())
                    .nickName(before.getNickName())
                    .build();

            //@formatter:off
            final String json =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .body(dto)
                    .when()
                        .patch("/{id}", spexare.getId())
                    .then()
                        .statusCode(HttpStatus.ACCEPTED.value())
                        .extract().body().asString();
            //@formatter:on

            final SpexareDto updated = objectMapper.readValue(json, SpexareDto.class);

            //@formatter:off
            final SpexareDto after =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", spexare.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(SpexareDto.class);
            //@formatter:on

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_not_found() {
            final SpexareUpdateDto dto = random.nextObject(SpexareUpdateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .body(dto)
            .when()
                .patch("/{id}", dto.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        void should_delete_and_return_204() {
            var spexare = persistSpexare(randomizeSpexare());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}", spexare.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_not_found() {
            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}", 123)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Image")
    class ImageTests {

        @Test
        void should_update_image_and_return_204() throws Exception {
            var spexare = persistSpexare(randomizeSpexare());
            var image = Files.readAllBytes(Paths.get(ResourceUtils.getFile("classpath:test.png").getPath()));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(MediaType.IMAGE_PNG_VALUE)
                .body(image)
            .when()
                .put("/{id}/image", spexare.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            //@formatter:off
            final byte[] result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}/image", spexare.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                        .extract().asByteArray();
            //@formatter:on

            assertThat(result).isEqualTo(image);
        }

        @Test
        void should_update_image_via_multipart_and_return_204() throws Exception {
            var spexare = persistSpexare(randomizeSpexare());
            var image = ResourceUtils.getFile("classpath:test.png");

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .multiPart("file", image, MediaType.IMAGE_PNG_VALUE)
            .when()
                .post("/{id}/image", spexare.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            //@formatter:off
            final byte[] result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}/image", spexare.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                            .extract().asByteArray();
            //@formatter:on

            assertThat(result).isEqualTo(Files.readAllBytes(Paths.get(image.getPath())));
        }

        @Test
        void should_delete_image_and_return_204() throws Exception {
            var spexare = persistSpexare(randomizeSpexare());
            var image = Files.readAllBytes(Paths.get(ResourceUtils.getFile("classpath:test.png").getPath()));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(MediaType.IMAGE_PNG_VALUE)
                .body(image)
            .when()
                .put("/{id}/image", spexare.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}/image", spexare.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .get("/{id}/image", spexare.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }
    }

    @Nested
    @DisplayName("Partner")
    class PartnerTests {

        @Test
        void should_return_200() {
            var partner = persistSpexare(randomizeSpexare());
            var spexare = persistSpexare(randomizeSpexare());
            spexare.setPartner(partner);
            partner.setPartner(spexare);
            repository.save(spexare);
            repository.save(partner);

            //@formatter:off
            final SpexareDto result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{spexareId}/partner", spexare.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(SpexareDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "firstName", "lastName", "nickName")
                    .contains(partner.getId(), partner.getFirstName(), partner.getLastName(), partner.getNickName());
        }

        @Test
        void should_return_404_when_retrieving_and_spexare_not_found() {
            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .get("/{spexareId}/partner", 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return_404_when_retrieving_and_partner_not_found() {
            var spexare = persistSpexare(randomizeSpexare());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .get("/{spexareId}/partner", spexare.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_update_and_return_202() {
            var spexare = persistSpexare(randomizeSpexare());
            var partner = persistSpexare(randomizeSpexare());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .put("/{spexareId}/partner/{id}", spexare.getId(), partner.getId())
            .then()
                .statusCode(HttpStatus.ACCEPTED.value());
            //@formatter:on
        }

        @Test
        void should_return_404_when_updating_and_spexare_not_found() {
            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .put("/{spexareId}/partner/{id}", 1L, 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return_404_when_updating_and_partner_not_found() {
            var spexare = persistSpexare(randomizeSpexare());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .put("/{spexareId}/partner/{id}", spexare.getId(), 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_delete_and_return_204() {
            var partner = persistSpexare(randomizeSpexare());
            var spexare = persistSpexare(randomizeSpexare());
            spexare.setPartner(partner);
            partner.setPartner(spexare);
            repository.save(spexare);
            repository.save(partner);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .delete("/{spexareId}/partner", spexare.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on
        }

        @Test
        void should_return_422_when_removing_and_no_partner() {
            var spexare = persistSpexare(randomizeSpexare());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .delete("/{spexareId}/partner", spexare.getId())
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
            //@formatter:on
        }

        @Test
        void should_return_404_when_removing_and_spexare_not_found() {
            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .delete("/{spexareId}/partner", 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

    }

    @Nested
    @DisplayName("Events")
    class EventTests {

        @Test
        void should_return_found() {
            var spexare = persistSpexare(randomizeSpexare());

            //@formatter:off
            final List<EventDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/events")
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.events", EventDto.class);
            //@formatter:on

            assertThat(eventRepository.count()).isEqualTo(1);
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getEvent()).isEqualTo(Event.EventType.CREATE.name());
            assertThat(result.getFirst().getSource()).isEqualTo(Event.SourceType.SPEXARE.name());
            assertThat(result.getFirst().getCreatedBy()).isEqualTo(spexare.getCreatedBy());
        }

        @Test
        void should_return_403_when_not_permitted() {
            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .get("/events")
            .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
            //@formatter:on
        }
    }

    private Spexare randomizeSpexare() {
        return random.nextObject(Spexare.class);
    }

    private Spexare persistSpexare(Spexare spexare) {
        return repository.save(spexare);
    }

}
