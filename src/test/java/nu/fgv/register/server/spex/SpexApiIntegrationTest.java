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
import java.util.stream.IntStream;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.named;

public class SpexApiIntegrationTest extends AbstractIntegrationTest {

    private static String basePath;
    private final EasyRandom random;
    @LocalServerPort
    private int localPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpexRepository repository;

    @Autowired
    private SpexDetailsRepository detailsRepository;

    @Autowired
    private SpexCategoryRepository categoryRepository;

    public SpexApiIntegrationTest() {
        final EasyRandomParameters parameters = new EasyRandomParameters();
        parameters
                .randomize(
                        named("year"), new YearRandomizer()
                )
                .randomize(
                        named("firstYear"), new YearRandomizer()
                )
                .randomizationDepth(1);
        random = new EasyRandom(parameters);
    }

    @BeforeAll
    public static void beforeClass() {
        basePath = SpexApi.class.getAnnotation(RequestMapping.class).value()[0];
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
        detailsRepository.deleteAll();
        categoryRepository.deleteAll();
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
            //@formatter:off
            final List<SpexDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spex", SpexDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        public void should_return_one() {
            var category = persistSpexCategory(randomizeSpexCategory());
            persistSpex(randomizeSpex(category));

            //@formatter:off
            final List<SpexDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spex", SpexDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        public void should_return_many() {
            int size = 42;
            var category = persistSpexCategory(randomizeSpexCategory());
            IntStream.range(0, size).forEach(i -> persistSpex(randomizeSpex(category)));

            //@formatter:off
            final List<SpexDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .queryParam("size", size)
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spex", SpexDto.class);
            //@formatter:on

            assertThat(result).hasSize(size);
        }

    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        public void should_create_and_return_201() throws Exception {
            final SpexCreateDto dto = random.nextObject(SpexCreateDto.class);

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

            final SpexDto result = objectMapper.readValue(json, SpexDto.class);
            assertThat(result)
                    .extracting("title", "year")
                    .contains(dto.getTitle(), dto.getYear());
        }

        @Test
        public void should_fail_when_invalid_input() {
            final SpexCreateDto dto = random.nextObject(SpexCreateDto.class);
            dto.setTitle(null);

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
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));

            //@formatter:off
            final SpexDto result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", spex.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(SpexDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "title", "year")
                    .contains(spex.getId(), spex.getDetails().getTitle(), spex.getYear());
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
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));

            //@formatter:off
            final SpexDto before =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", spex.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(SpexDto.class);
            //@formatter:on

            final SpexUpdateDto dto = SpexUpdateDto.builder()
                    .id(before.getId())
                    .title(before.getTitle() + "_")
                    .year(before.getYear())
                    .build();

            //@formatter:off
            final String json =
                    given()
                        .contentType(ContentType.JSON)
                        .body(dto)
                    .when()
                        .put("/{id}", spex.getId())
                    .then()
                        .statusCode(HttpStatus.ACCEPTED.value())
                        .extract().body().asString();
            //@formatter:on

            final SpexDto updated = objectMapper.readValue(json, SpexDto.class);

            //@formatter:off
            final SpexDto after =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", spex.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(SpexDto.class);
            //@formatter:on

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
        }

        @Test
        public void should_fail_when_invalid_input() {
            final SpexUpdateDto dto = random.nextObject(SpexUpdateDto.class);
            dto.setTitle(null);

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
            final SpexUpdateDto dto = random.nextObject(SpexUpdateDto.class);

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
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));

            //@formatter:off
            final SpexDto before =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", spex.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                    .extract().body().as(SpexDto.class);
            //@formatter:on

            final SpexUpdateDto dto = SpexUpdateDto.builder()
                    .id(before.getId())
                    .title(before.getTitle() + "_")
                    .year(before.getYear())
                    .build();

            //@formatter:off
            final String json =
                    given()
                        .contentType(ContentType.JSON)
                        .body(dto)
                    .when()
                        .patch("/{id}", spex.getId())
                    .then()
                        .statusCode(HttpStatus.ACCEPTED.value())
                        .extract().body().asString();
            //@formatter:on

            final SpexDto updated = objectMapper.readValue(json, SpexDto.class);

            //@formatter:off
            final SpexDto after =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", spex.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(SpexDto.class);
            //@formatter:on

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
        }

        @Test
        public void should_return_404_when_not_found() {
            final SpexUpdateDto dto = random.nextObject(SpexUpdateDto.class);

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
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}", spex.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(0);
            assertThat(categoryRepository.count()).isEqualTo(1);
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
    @DisplayName("Poster")
    class PosterTests {

        @Test
        public void should_update_poster() throws Exception {
            var category = persistSpexCategory(randomizeSpexCategory());
            var persisted = persistSpex(randomizeSpex(category));
            var poster = Files.readAllBytes(Paths.get(ResourceUtils.getFile("classpath:test.png").getPath()));

            //@formatter:off
            given()
                .contentType(MediaType.IMAGE_PNG_VALUE)
                .body(poster)
            .when()
                .put("/{id}/poster", persisted.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            //@formatter:off
            final byte[] result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}/poster", persisted.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                        .extract().asByteArray();
            //@formatter:on

            assertThat(result).isEqualTo(poster);
        }

        @Test
        public void should_update_poster_via_multipart() throws Exception {
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));
            var poster = ResourceUtils.getFile("classpath:test.png");

            //@formatter:off
            given()
                .multiPart("file", poster, MediaType.IMAGE_PNG_VALUE)
            .when()
                .post("/{id}/poster", spex.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            //@formatter:off
            final byte[] result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}/poster", spex.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                            .extract().asByteArray();
            //@formatter:on

            assertThat(result).isEqualTo(Files.readAllBytes(Paths.get(poster.getPath())));
        }

        @Test
        public void should_delete_poster() throws Exception {
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));
            var poster = Files.readAllBytes(Paths.get(ResourceUtils.getFile("classpath:test.png").getPath()));

            //@formatter:off
            given()
                .contentType(MediaType.IMAGE_PNG_VALUE)
                .body(poster)
            .when()
                .put("/{id}/poster", spex.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}/poster", spex.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .get("/{id}/poster", spex.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }
    }

    @Nested
    @DisplayName("All revivals")
    class AllRevivalTests {

        @Test
        public void should_return_zero() {
            //@formatter:off
            final List<SpexDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/revivals")
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spex", SpexDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        public void should_return_one() {
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));
            persistRevival(randomizeRevival(spex));

            //@formatter:off
            final List<SpexDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/revivals")
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spex", SpexDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        public void should_return_many() {
            int size = 42;
            var category = persistSpexCategory(randomizeSpexCategory());
            IntStream.range(0, size).forEach(i -> {
                var spex = persistSpex(randomizeSpex(category));
                persistRevival(randomizeRevival(spex));
            });

            //@formatter:off
            final List<SpexDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .queryParam("size", size)
                        .get("/revivals")
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spex", SpexDto.class);
            //@formatter:on

            assertThat(result).hasSize(size);
        }

    }

    @Nested
    @DisplayName("Revivals")
    class RevivalTests {

        @Test
        public void should_return_zero() {
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));

            //@formatter:off
            final List<SpexDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}/revivals", spex.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spex", SpexDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        public void should_return_404_when_non_existent_spex() {
            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .get("/{id}/revivals", "123")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        public void should_return_one() {
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));

            var revival = randomizeRevival(spex);
            persistRevival(revival);

            //@formatter:off
            final List<SpexDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}/revivals", spex.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spex", SpexDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        public void should_return_many() {
            int size = 42;
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));
            IntStream.range(0, size).forEach(i -> persistRevival(randomizeRevival(spex)));

            //@formatter:off
            final List<SpexDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .queryParam("size", size)
                        .get("/{id}/revivals", spex.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spex", SpexDto.class);
            //@formatter:on

            assertThat(result).hasSize(size);
        }

        @Test
        public void should_add_and_return_201() throws Exception {
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));

            //@formatter:off
            final String json =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .put("/{id}/revivals/{year}", spex.getId(), "2022")
                    .then()
                        .statusCode(HttpStatus.ACCEPTED.value())
                        .extract().body().asString();
            //@formatter:on

            final SpexDto result = objectMapper.readValue(json, SpexDto.class);
            assertThat(result)
                    .extracting("title", "year")
                    .contains(spex.getDetails().getTitle(), "2022");

            //@formatter:off
            final List<SpexDto> result1 =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}/revivals", spex.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spex", SpexDto.class);
            //@formatter:on

            assertThat(result1).hasSize(1);
        }

        @Test
        public void should_return_404_when_adding_and_spex_not_found() {
            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .put("/{id}/revivals/{year}", "123", "2022")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        public void should_return_409_when_adding_and_year_already_exists() {
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .put("/{id}/revivals/{year}", spex.getId(), "2022")
            .then()
                .statusCode(HttpStatus.ACCEPTED.value())
                .extract().body().asString();
            //@formatter:on

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .put("/{id}/revivals/{year}", spex.getId(), "2022")
            .then()
                .statusCode(HttpStatus.CONFLICT.value());
            //@formatter:on
        }

        @Test
        public void should_remove_and_return_204() {
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .put("/{id}/revivals/{year}", spex.getId(), "2022")
            .then()
                .statusCode(HttpStatus.ACCEPTED.value())
                .extract().body().asString();
            //@formatter:on

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}/revivals/{year}", spex.getId(), "2022")
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on
        }

        @Test
        public void should_return_404_when_removing_and_spex_not_found() {
            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}/revivals/{year}", "123", "2022")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        public void should_return_422_when_removing_and_year_not_found() {
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}/revivals/{year}", spex.getId(), "2022")
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
            //@formatter:on

            //@formatter:off
            final List<SpexDto> result1 =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}/revivals", spex.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.spex", SpexDto.class);
            //@formatter:on

            assertThat(result1).isEmpty();
        }
    }

    @Nested
    @DisplayName("Spex category")
    class SpexCategoryTests {

        @Test
        public void should_update_and_return_201() throws Exception {
            var category = persistSpexCategory(randomizeSpexCategory());

            final SpexCreateDto dto = random.nextObject(SpexCreateDto.class);

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

            final SpexDto spex = objectMapper.readValue(json, SpexDto.class);

            //@formatter:off
            final String json1 =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .put("/{id}/category/{categoryId}", spex.getId(), category.getId())
                    .then()
                        .statusCode(HttpStatus.ACCEPTED.value())
                        .extract().body().asString();
            //@formatter:on

            final SpexDto result = objectMapper.readValue(json1, SpexDto.class);

            assertThat(result)
                    .extracting("title", "category.name")
                    .contains(result.getTitle(), result.getCategory().getName());
        }

        @Test
        public void should_return_404_when_updating_and_spex_not_found() {
            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .put("/{id}/category/{categoryId}", "123", "321")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        public void should_return_404_when_updating_and_category_not_found() {
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .put("/{id}/category/{categoryId}", spex.getId(), "321")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        public void should_remove_and_return_201() throws Exception {
            var category = persistSpexCategory(randomizeSpexCategory());
            var spex = persistSpex(randomizeSpex(category));

            //@formatter:off
            final String json =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .delete("/{id}/category", spex.getId())
                    .then()
                        .statusCode(HttpStatus.ACCEPTED.value())
                        .extract().body().asString();
            //@formatter:on

            final SpexDto result = objectMapper.readValue(json, SpexDto.class);

            assertThat(result.getCategory()).isNull();
        }

        @Test
        public void should_return_404_when_removing_and_spex_not_found() {
            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}/category", "123")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

    }

    private Spex randomizeSpex(SpexCategory category) {
        var spex = random.nextObject(Spex.class);
        spex.setParent(null);
        var details = random.nextObject(SpexDetails.class);
        details.setCategory(category);
        spex.setDetails(details);
        return spex;
    }

    private Spex randomizeRevival(Spex parent) {
        var revival = random.nextObject(Spex.class);
        revival.setParent(parent);
        revival.setDetails(parent.getDetails());
        return revival;
    }

    private SpexCategory randomizeSpexCategory() {
        return random.nextObject(SpexCategory.class);
    }

    private Spex persistSpex(Spex spex) {
        var details = detailsRepository.save(spex.getDetails());
        spex.setDetails(details);
        return repository.save(spex);
    }

    private Spex persistRevival(Spex spex) {
        return repository.save(spex);
    }

    private SpexCategory persistSpexCategory(SpexCategory category) {
        return categoryRepository.save(category);
    }
}
