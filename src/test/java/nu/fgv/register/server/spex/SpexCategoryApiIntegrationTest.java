package nu.fgv.register.server.spex;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.randomizer.YearRandomizer;
import org.apache.http.HttpHeaders;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.FieldPredicates;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static org.assertj.core.api.Assertions.assertThat;

public class SpexCategoryApiIntegrationTest extends AbstractIntegrationTest {

    private static String basePath;
    private final EasyRandom random;
    @LocalServerPort
    private int localPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpexCategoryRepository repository;

    public SpexCategoryApiIntegrationTest() {
        final EasyRandomParameters parameters = new EasyRandomParameters();
        parameters
                .randomize(
                        FieldPredicates.named("firstYear"), new YearRandomizer()
                );
        random = new EasyRandom(parameters);
    }

    @BeforeAll
    public static void beforeClass() {
        basePath = SpexCategoryApi.class.getAnnotation(RequestMapping.class).value()[0];
    }

    @BeforeEach
    public void setUp() {
        RestAssured.port = localPort;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        final RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder();
        requestSpecBuilder.setBasePath(basePath);
        RestAssured.requestSpecification = requestSpecBuilder.build();
        RestAssured.config = config().encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false));
        repository.deleteAll();
    }

    @AfterEach
    public void tearDown() {
        RestAssured.reset();
    }

    @Nested
    @DisplayName("Retrieve paged")
    class RetrievePagedTests {

        @Test
        public void should_return_zero() {
            // @formatter:off
            final List<SpexCategoryDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spexCategories", SpexCategoryDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        public void should_return_one() {
            var category = random.nextObject(SpexCategory.class);
            repository.save(category);

            //@formatter:off
            final List<SpexCategoryDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spexCategories", SpexCategoryDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        public void should_return_many() {
            int size = 42;
            final Stream<SpexCategory> categories = random.objects(SpexCategory.class, size);
            categories.forEach(category -> repository.save(category));

            //@formatter:off
            final List<SpexCategoryDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .queryParam("size", size)
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spexCategories", SpexCategoryDto.class);
            //@formatter:on

            assertThat(result).hasSize(size);
        }
    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        public void should_create_and_return_201() throws Exception {
            final SpexCategoryCreateDto dto = random.nextObject(SpexCategoryCreateDto.class);

            //@formatter:off
            final String json =
                    given()
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
        }

        @Test
        public void should_fail_when_invalid_input() {
            final SpexCategoryCreateDto dto = random.nextObject(SpexCategoryCreateDto.class);
            dto.setName(null);

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
                .body(dto)
            .when()
                .post()
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
            //@formatter:on
        }
    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        public void should_return_found() {
            var category = random.nextObject(SpexCategory.class);
            var persisted = repository.save(category);

            //@formatter:off
            final SpexCategoryDto result =
                given()
                    .contentType(ContentType.JSON)
                .when()
                    .get("/{id}", persisted.getId())
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .extract().body().as(SpexCategoryDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "name", "firstYear")
                    .contains(persisted.getId(), persisted.getName(), persisted.getFirstYear());
        }

        @Test
        public void should_return_404_when_not_found() {
            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .get("/{id}", "123")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }
    }

    @Nested
    @DisplayName("Update")
    class UpdateTests {

        @Test
        public void should_update_and_return_202() throws Exception {
            var category = random.nextObject(SpexCategory.class);
            var persisted = repository.save(category);

            //@formatter:off
            final SpexCategoryDto before =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", persisted.getId())
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
                            .contentType(ContentType.JSON)
                            .body(dto)
                    .when()
                            .put("/{id}", persisted.getId())
                    .then()
                            .statusCode(HttpStatus.ACCEPTED.value())
                            .extract().body().asString();
            //@formatter:on

            final SpexCategoryDto updated = objectMapper.readValue(json, SpexCategoryDto.class);

            //@formatter:off
            final SpexCategoryDto after =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", persisted.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(SpexCategoryDto.class);
            //@formatter:on

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
        }

        @Test
        public void should_fail_when_invalid_input() {
            final SpexCategoryUpdateDto dto = random.nextObject(SpexCategoryUpdateDto.class);
            dto.setName(null);

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
                .body(dto)
            .when()
                .put("/{id}", dto.getId())
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
            //@formatter:on
        }

        @Test
        public void should_return_404_when_not_found() {
            final SpexCategoryUpdateDto dto = random.nextObject(SpexCategoryUpdateDto.class);

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
                .body(dto)
            .when()
                .put("/{id}", dto.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }
    }

    @Nested
    @DisplayName("Partial update")
    class PartialUpdateTests {

        @Test
        public void should_update_and_return_202() throws Exception {
            var category = random.nextObject(SpexCategory.class);
            var persisted = repository.save(category);

            //@formatter:off
            final SpexCategoryDto before =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", persisted.getId())
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
                        .contentType(ContentType.JSON)
                        .body(dto)
                    .when()
                        .patch("/{id}", persisted.getId())
                    .then()
                        .statusCode(HttpStatus.ACCEPTED.value())
                        .extract().body().asString();
            //@formatter:on

            final SpexCategoryDto updated = objectMapper.readValue(json, SpexCategoryDto.class);

            //@formatter:off
            final SpexCategoryDto after =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", persisted.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(SpexCategoryDto.class);
            //@formatter:on

            assertThat(after)
                    .usingRecursiveComparison()
                    .isEqualTo(updated);
        }

        @Test
        public void should_return_404_when_not_found() {
            final SpexCategoryUpdateDto dto = random.nextObject(SpexCategoryUpdateDto.class);

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
                .body(dto)
            .when()
                .patch("/{id}", dto.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        public void should_delete() {
            var category = random.nextObject(SpexCategory.class);
            var persisted = repository.save(category);

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}", persisted.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(0);
        }

        @Test
        public void should_return_404_when_not_found() {
            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}", 123)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }
    }

    @Nested
    @DisplayName("Logo")
    class LogoTests {

        @Test
        public void should_update_logo() throws Exception {
            var category = random.nextObject(SpexCategory.class);
            var persisted = repository.save(category);
            var logo = Files.readAllBytes(Paths.get(ResourceUtils.getFile("classpath:test.png").getPath()));

            //@formatter:off
            given()
                .contentType(MediaType.IMAGE_PNG_VALUE)
                .body(logo)
            .when()
                .put("/{id}/logo", persisted.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            //@formatter:off
            final byte[] result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}/logo", persisted.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                        .extract().asByteArray();
            //@formatter:on

            assertThat(result).isEqualTo(logo);
        }

        @Test
        public void should_update_logo_via_multipart() throws Exception {
            var category = random.nextObject(SpexCategory.class);
            var persisted = repository.save(category);
            var logo = ResourceUtils.getFile("classpath:test.png");

            //@formatter:off
            given()
                .multiPart("file", logo, MediaType.IMAGE_PNG_VALUE)
            .when()
                .post("/{id}/logo", persisted.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            //@formatter:off
            final byte[] result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}/logo", persisted.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                        .extract().asByteArray();
            //@formatter:on

            assertThat(result).isEqualTo(Files.readAllBytes(Paths.get(logo.getPath())));
        }

        @Test
        public void should_delete_logo() throws Exception {
            var category = random.nextObject(SpexCategory.class);
            var persisted = repository.save(category);
            var logo = Files.readAllBytes(Paths.get(ResourceUtils.getFile("classpath:test.png").getPath()));

            //@formatter:off
            given()
                .contentType(MediaType.IMAGE_PNG_VALUE)
                .body(logo)
            .when()
                .put("/{id}/logo", persisted.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}/logo", persisted.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .get("/{id}/logo", persisted.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }
    }
}
