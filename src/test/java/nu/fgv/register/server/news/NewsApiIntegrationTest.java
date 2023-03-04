package nu.fgv.register.server.news;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import nu.fgv.register.server.util.AbstractIntegrationTest;
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
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static org.assertj.core.api.Assertions.assertThat;

public class NewsApiIntegrationTest extends AbstractIntegrationTest {

    private static String basePath;
    private final EasyRandom random;
    @LocalServerPort
    private int localPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NewsRepository repository;

    public NewsApiIntegrationTest() {
        final EasyRandomParameters parameters = new EasyRandomParameters();
        random = new EasyRandom(parameters);
    }

    @BeforeAll
    public static void beforeClass() {
        basePath = NewsApi.class.getAnnotation(RequestMapping.class).value()[0];
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
            //@formatter:off
            final List<NewsDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.news", NewsDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        public void should_return_one() {
            persistNews(randomizeNews());

            //@formatter:off
            final List<NewsDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.news", NewsDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        public void should_return_many() {
            int size = 42;
            IntStream.range(0, size).forEach(i -> persistNews(randomizeNews()));

            //@formatter:off
            final List<NewsDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .queryParam("size", size)
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.news", NewsDto.class);
            //@formatter:on

            assertThat(result).hasSize(size);
        }

    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        public void should_create_and_return_201() throws Exception {
            final NewsCreateDto dto = random.nextObject(NewsCreateDto.class);

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

            final NewsDto result = objectMapper.readValue(json, NewsDto.class);
            assertThat(result)
                    .extracting("subject", "text", "publicationDate")
                    .contains(dto.getSubject(), dto.getText(), dto.getPublicationDate());
        }

        @Test
        public void should_fail_when_invalid_input() {
            final NewsCreateDto dto = random.nextObject(NewsCreateDto.class);
            dto.setSubject(null);

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
            var news = persistNews(randomizeNews());

            //@formatter:off
            final NewsDto result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", news.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(NewsDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "subject", "text", "publicationDate")
                    .contains(news.getId(), news.getSubject(), news.getText(), news.getPublicationDate());
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
            var news = persistNews(randomizeNews());

            //@formatter:off
            final NewsDto before =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", news.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(NewsDto.class);
            //@formatter:on

            final NewsUpdateDto dto = NewsUpdateDto.builder()
                    .id(before.getId())
                    .subject(before.getSubject() + "_")
                    .text(before.getText())
                    .publicationDate(before.getPublicationDate())
                    .build();

            //@formatter:off
            final String json =
                    given()
                        .contentType(ContentType.JSON)
                        .body(dto)
                    .when()
                        .put("/{id}", news.getId())
                    .then()
                        .statusCode(HttpStatus.ACCEPTED.value())
                        .extract().body().asString();
            //@formatter:on

            final NewsDto updated = objectMapper.readValue(json, NewsDto.class);

            //@formatter:off
            final NewsDto after =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", news.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(NewsDto.class);
            //@formatter:on

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
        }

        @Test
        public void should_fail_when_invalid_input() {
            final NewsUpdateDto dto = random.nextObject(NewsUpdateDto.class);
            dto.setSubject(null);

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
            final NewsUpdateDto dto = random.nextObject(NewsUpdateDto.class);

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
            var news = persistNews(randomizeNews());

            //@formatter:off
            final NewsDto before =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", news.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                    .extract().body().as(NewsDto.class);
            //@formatter:on

            final NewsUpdateDto dto = NewsUpdateDto.builder()
                    .id(before.getId())
                    .subject(before.getSubject() + "_")
                    .text(before.getText())
                    .publicationDate(before.getPublicationDate())
                    .build();

            //@formatter:off
            final String json =
                    given()
                        .contentType(ContentType.JSON)
                        .body(dto)
                    .when()
                        .patch("/{id}", news.getId())
                    .then()
                        .statusCode(HttpStatus.ACCEPTED.value())
                        .extract().body().asString();
            //@formatter:on

            final NewsDto updated = objectMapper.readValue(json, NewsDto.class);

            //@formatter:off
            final NewsDto after =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", news.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(NewsDto.class);
            //@formatter:on

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
        }

        @Test
        public void should_return_404_when_not_found() {
            final NewsUpdateDto dto = random.nextObject(NewsUpdateDto.class);

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
            var news = persistNews(randomizeNews());

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}", news.getId())
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

    private News randomizeNews() {
        return random.nextObject(News.class);
    }

    private News persistNews(News news) {
        return repository.save(news);
    }

}
