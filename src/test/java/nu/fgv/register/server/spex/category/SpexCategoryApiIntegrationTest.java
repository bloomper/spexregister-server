package nu.fgv.register.server.spex.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventRepository;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.randomizer.YearRandomizer;
import org.jeasy.random.EasyRandom;
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
import org.springframework.http.MediaType;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.named;

class SpexCategoryApiIntegrationTest extends AbstractIntegrationTest {

    private static String basePath;
    private final EasyRandom random;
    @LocalServerPort
    private int localPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpexCategoryRepository repository;

    @Autowired
    private EventRepository eventRepository;

    public SpexCategoryApiIntegrationTest() {
        final EasyRandomParameters parameters = new EasyRandomParameters();
        parameters
                .randomize(
                        named("firstYear"), new YearRandomizer()
                );
        random = new EasyRandom(parameters);
    }

    @BeforeAll
    public static void beforeClass() {
        basePath = SpexCategoryApi.class.getAnnotation(RequestMapping.class).value()[0];
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
            // @formatter:off
            final List<SpexCategoryDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spex-categories", SpexCategoryDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_one() {
            var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));

            //@formatter:off
            final List<SpexCategoryDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spex-categories", SpexCategoryDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            int size = 42;
            IntStream.range(0, size).forEach(i -> {
                var category = persistSpexCategory(randomizeSpexCategory());
                grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            });

            //@formatter:off
            final List<SpexCategoryDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .queryParam("size", size)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spex-categories", SpexCategoryDto.class);
            //@formatter:on

            assertThat(result).hasSize(size);
        }
    }

    @Nested
    @DisplayName("Retrieve paged with filtering")
    class RetrievePagedWithFilteringTests {

        @Test
        void should_return_zero() {
            var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));

            // @formatter:off
            final List<SpexCategoryDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .queryParam("filter", SpexCategory_.NAME + ":whatever")
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spex-categories", SpexCategoryDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_one() {
            var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));

            //@formatter:off
            final List<SpexCategoryDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .queryParam("filter", SpexCategory_.NAME + ":" + category.getName())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spex-categories", SpexCategoryDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            int size = 42;
            IntStream.range(0, size).forEach(i -> {
                var category = randomizeSpexCategory();
                if (i % 2 == 0) {
                    category.setName("whatever");
                }
                var category0 = persistSpexCategory(category);
                grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category0.getId()));
            });

            //@formatter:off
            final List<SpexCategoryDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .queryParam("filter", SpexCategory_.NAME + ":whatever")
                        .queryParam("size", size)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spex-categories", SpexCategoryDto.class);
            //@formatter:on

            assertThat(result).hasSize(size / 2);
        }
    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create_and_return_201() throws Exception {
            final SpexCategoryCreateDto dto = random.nextObject(SpexCategoryCreateDto.class);

            //@formatter:off
            final String json =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                        .contentType(ContentType.JSON)
                        .body(dto)
                    .when()
                        .post()
                    .then()
                        .statusCode(HttpStatus.CREATED.value())
                        .extract().body().asString();
            //@formatter:on

            final SpexCategoryDto result = objectMapper.readValue(json, SpexCategoryDto.class);
            assertThat(result)
                    .extracting("name", "firstYear")
                    .contains(dto.getName(), dto.getFirstYear());
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_400_when_invalid_input() {
            final SpexCategoryCreateDto dto = random.nextObject(SpexCategoryCreateDto.class);
            dto.setName(null);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .body(dto)
            .when()
                .post()
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_403_when_not_permitted() {
            final SpexCategoryCreateDto dto = random.nextObject(SpexCategoryCreateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .body(dto)
            .when()
                .post()
            .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        void should_return_found() {
            var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));

            //@formatter:off
            final SpexCategoryDto result =
                given()
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .contentType(ContentType.JSON)
                .when()
                    .get("/{id}", category.getId())
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .extract().body().as(SpexCategoryDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "name", "firstYear")
                    .contains(category.getId(), category.getName(), category.getFirstYear());
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
            var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));

            //@formatter:off
            final SpexCategoryDto before =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", category.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(SpexCategoryDto.class);
            //@formatter:on

            final SpexCategoryUpdateDto dto = SpexCategoryUpdateDto.builder()
                    .id(before.getId())
                    .name(before.getName() + "_")
                    .firstYear(before.getFirstYear())
                    .build();

            //@formatter:off
            final String json =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                            .contentType(ContentType.JSON)
                            .body(dto)
                    .when()
                            .put("/{id}", category.getId())
                    .then()
                            .statusCode(HttpStatus.ACCEPTED.value())
                            .extract().body().asString();
            //@formatter:on

            final SpexCategoryDto updated = objectMapper.readValue(json, SpexCategoryDto.class);

            //@formatter:off
            final SpexCategoryDto after =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", category.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(SpexCategoryDto.class);
            //@formatter:on

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_400_when_invalid_input() {
            final SpexCategoryUpdateDto dto = random.nextObject(SpexCategoryUpdateDto.class);
            dto.setName(null);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
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
            final SpexCategoryUpdateDto dto = random.nextObject(SpexCategoryUpdateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .body(dto)
            .when()
                .put("/{id}", dto.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_403_when_not_permitted() {
            final SpexCategoryUpdateDto dto = random.nextObject(SpexCategoryUpdateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .body(dto)
            .when()
                .put("/{id}", dto.getId())
            .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Partial update")
    class PartialUpdateTests {

        @Test
        void should_update_and_return_202() throws Exception {
            var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));

            //@formatter:off
            final SpexCategoryDto before =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", category.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(SpexCategoryDto.class);
            //@formatter:on

            final SpexCategoryUpdateDto dto = SpexCategoryUpdateDto.builder()
                    .id(before.getId())
                    .name(before.getName() + "_")
                    .firstYear(before.getFirstYear())
                    .build();

            //@formatter:off
            final String json =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                        .contentType(ContentType.JSON)
                        .body(dto)
                    .when()
                        .patch("/{id}", category.getId())
                    .then()
                        .statusCode(HttpStatus.ACCEPTED.value())
                        .extract().body().asString();
            //@formatter:on

            final SpexCategoryDto updated = objectMapper.readValue(json, SpexCategoryDto.class);

            //@formatter:off
            final SpexCategoryDto after =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", category.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(SpexCategoryDto.class);
            //@formatter:on

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_not_found() {
            final SpexCategoryUpdateDto dto = random.nextObject(SpexCategoryUpdateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .body(dto)
            .when()
                .patch("/{id}", dto.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_403_when_not_permitted() {
            final SpexCategoryUpdateDto dto = random.nextObject(SpexCategoryUpdateDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .body(dto)
            .when()
                .patch("/{id}", dto.getId())
            .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        void should_delete_and_return_204() {
            var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            grantDeletePermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}", category.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_not_found() {
            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}", 123)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_403_when_not_permitted() {
            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}", 123)
            .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Logo")
    class LogoTests {

        @Test
        void should_update_logo_and_return_204() throws Exception {
            var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            var logo = Files.readAllBytes(Paths.get(ResourceUtils.getFile("classpath:test.png").getPath()));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(MediaType.IMAGE_PNG_VALUE)
                .body(logo)
            .when()
                .put("/{id}/logo", category.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            //@formatter:off
            final byte[] result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}/logo", category.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                        .extract().asByteArray();
            //@formatter:on

            assertThat(result).isEqualTo(logo);
        }

        @Test
        void should_update_logo_via_multipart_and_return_204() throws Exception {
            var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            var logo = ResourceUtils.getFile("classpath:test.png");

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .multiPart("file", logo, MediaType.IMAGE_PNG_VALUE)
            .when()
                .post("/{id}/logo", category.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            //@formatter:off
            final byte[] result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}/logo", category.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                        .extract().asByteArray();
            //@formatter:on

            assertThat(result).isEqualTo(Files.readAllBytes(Paths.get(logo.getPath())));
        }

        @Test
        void should_delete_logo_and_return_204() throws Exception {
            var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            var logo = Files.readAllBytes(Paths.get(ResourceUtils.getFile("classpath:test.png").getPath()));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(MediaType.IMAGE_PNG_VALUE)
                .body(logo)
            .when()
                .put("/{id}/logo", category.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}/logo", category.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .get("/{id}/logo", category.getId())
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
            var category = persistSpexCategory(randomizeSpexCategory());

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
            assertThat(result.getFirst().getSource()).isEqualTo(Event.SourceType.SPEX_CATEGORY.name());
            assertThat(result.getFirst().getCreatedBy()).isEqualTo(category.getCreatedBy());
        }

    }

    private SpexCategory randomizeSpexCategory() {
        return random.nextObject(SpexCategory.class);
    }

    private SpexCategory persistSpexCategory(SpexCategory category) {
        return repository.save(category);
    }
}
