package nu.fgv.register.server.task;

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
import java.util.stream.Stream;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static org.assertj.core.api.Assertions.assertThat;

public class TaskCategoryApiIntegrationTest extends AbstractIntegrationTest {

    private static String basePath;
    private final EasyRandom random;
    @LocalServerPort
    private int localPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskCategoryRepository repository;

    public TaskCategoryApiIntegrationTest() {
        final EasyRandomParameters parameters = new EasyRandomParameters();
        random = new EasyRandom(parameters);
    }

    @BeforeAll
    public static void beforeClass() {
        basePath = TaskCategoryApi.class.getAnnotation(RequestMapping.class).value()[0];
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
            final List<TaskCategoryDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.taskCategories", TaskCategoryDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        public void should_return_one() {
            var category = random.nextObject(TaskCategory.class);
            repository.save(category);

            //@formatter:off
            final List<TaskCategoryDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.taskCategories", TaskCategoryDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        public void should_return_many() {
            int size = 42;
            final Stream<TaskCategory> categories = random.objects(TaskCategory.class, size);
            categories.forEach(category -> repository.save(category));

            //@formatter:off
            final List<TaskCategoryDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .queryParam("size", size)
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.taskCategories", TaskCategoryDto.class);
            //@formatter:on

            assertThat(result).hasSize(size);
        }
    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        public void should_create_and_return_201() throws Exception {
            final TaskCategoryCreateDto dto = random.nextObject(TaskCategoryCreateDto.class);

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

            final TaskCategoryDto result = objectMapper.readValue(json, TaskCategoryDto.class);
            assertThat(result)
                    .extracting("name", "hasActor")
                    .contains(dto.getName(), dto.isHasActor());
        }

        @Test
        public void should_fail_when_invalid_input() {
            final TaskCategoryCreateDto dto = random.nextObject(TaskCategoryCreateDto.class);
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
            var category = random.nextObject(TaskCategory.class);
            var persisted = repository.save(category);

            //@formatter:off
            final TaskCategoryDto result =
                given()
                    .contentType(ContentType.JSON)
                .when()
                    .get("/{id}", persisted.getId())
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .extract().body().as(TaskCategoryDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "name", "hasActor")
                    .contains(persisted.getId(), persisted.getName(), persisted.getHasActor());
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
            var category = random.nextObject(TaskCategory.class);
            var persisted = repository.save(category);

            //@formatter:off
            final TaskCategoryDto before =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", persisted.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(TaskCategoryDto.class);
            //@formatter:on

            final TaskCategoryUpdateDto dto = TaskCategoryUpdateDto.builder()
                    .id(before.getId())
                    .name(before.getName() + "_")
                    .hasActor(before.isHasActor())
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

            final TaskCategoryDto updated = objectMapper.readValue(json, TaskCategoryDto.class);

            //@formatter:off
            final TaskCategoryDto after =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", persisted.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(TaskCategoryDto.class);
            //@formatter:on

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
        }

        @Test
        public void should_fail_when_invalid_input() {
            final TaskCategoryUpdateDto dto = random.nextObject(TaskCategoryUpdateDto.class);
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
            final TaskCategoryUpdateDto dto = random.nextObject(TaskCategoryUpdateDto.class);

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
            var category = random.nextObject(TaskCategory.class);
            var persisted = repository.save(category);

            //@formatter:off
            final TaskCategoryDto before =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", persisted.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(TaskCategoryDto.class);
            //@formatter:on

            final TaskCategoryUpdateDto dto = TaskCategoryUpdateDto.builder()
                    .id(before.getId())
                    .name(before.getName() + "_")
                    .hasActor(before.isHasActor())
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

            final TaskCategoryDto updated = objectMapper.readValue(json, TaskCategoryDto.class);

            //@formatter:off
            final TaskCategoryDto after =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", persisted.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(TaskCategoryDto.class);
            //@formatter:on

            assertThat(after)
                    .usingRecursiveComparison()
                    .isEqualTo(updated);
        }

        @Test
        public void should_return_404_when_not_found() {
            final TaskCategoryUpdateDto dto = random.nextObject(TaskCategoryUpdateDto.class);

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
            var category = random.nextObject(TaskCategory.class);
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

}
