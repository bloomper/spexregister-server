package nu.fgv.register.server.task.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventRepository;
import nu.fgv.register.server.task.category.TaskCategory;
import nu.fgv.register.server.task.category.TaskCategoryApi;
import nu.fgv.register.server.task.category.TaskCategoryCreateDto;
import nu.fgv.register.server.task.category.TaskCategoryDto;
import nu.fgv.register.server.task.category.TaskCategoryRepository;
import nu.fgv.register.server.task.category.TaskCategoryUpdateDto;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.IntStream;

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

    @Autowired
    private EventRepository eventRepository;

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
        RestAssured.config = config()
                .encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))
                .logConfig(LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails());

        repository.deleteAll();
        eventRepository.deleteAll();
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
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.task-categories", TaskCategoryDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        public void should_return_one() {
            persistTaskCategory(randomizeTaskCategory());

            //@formatter:off
            final List<TaskCategoryDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.task-categories", TaskCategoryDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        public void should_return_many() {
            int size = 42;
            IntStream.range(0, size).forEach(i -> persistTaskCategory(randomizeTaskCategory()));

            //@formatter:off
            final List<TaskCategoryDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .queryParam("size", size)
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.task-categories", TaskCategoryDto.class);
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
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
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
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_400_when_invalid_input() {
            final TaskCategoryCreateDto dto = random.nextObject(TaskCategoryCreateDto.class);
            dto.setName(null);

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

            assertThat(repository.count()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        public void should_return_found() {
            var category = persistTaskCategory(randomizeTaskCategory());

            //@formatter:off
            final TaskCategoryDto result =
                given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .contentType(ContentType.JSON)
                .when()
                    .get("/{id}", category.getId())
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .extract().body().as(TaskCategoryDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "name", "hasActor")
                    .contains(category.getId(), category.getName(), category.getHasActor());
        }

        @Test
        public void should_return_404_when_not_found() {
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
        public void should_update_and_return_202() throws Exception {
            var category = persistTaskCategory(randomizeTaskCategory());

            //@formatter:off
            final TaskCategoryDto before =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", category.getId())
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
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .body(dto)
                    .when()
                        .put("/{id}", category.getId())
                    .then()
                        .statusCode(HttpStatus.ACCEPTED.value())
                        .extract().body().asString();
            //@formatter:on

            final TaskCategoryDto updated = objectMapper.readValue(json, TaskCategoryDto.class);

            //@formatter:off
            final TaskCategoryDto after =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", category.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(TaskCategoryDto.class);
            //@formatter:on

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_400_when_invalid_input() {
            final TaskCategoryUpdateDto dto = random.nextObject(TaskCategoryUpdateDto.class);
            dto.setName(null);

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

            assertThat(repository.count()).isEqualTo(0);
        }

        @Test
        public void should_return_404_when_not_found() {
            final TaskCategoryUpdateDto dto = random.nextObject(TaskCategoryUpdateDto.class);

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

            assertThat(repository.count()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Partial update")
    class PartialUpdateTests {

        @Test
        public void should_update_and_return_202() throws Exception {
            var category = persistTaskCategory(randomizeTaskCategory());

            //@formatter:off
            final TaskCategoryDto before =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", category.getId())
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
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .body(dto)
                    .when()
                        .patch("/{id}", category.getId())
                    .then()
                        .statusCode(HttpStatus.ACCEPTED.value())
                        .extract().body().asString();
            //@formatter:on

            final TaskCategoryDto updated = objectMapper.readValue(json, TaskCategoryDto.class);

            //@formatter:off
            final TaskCategoryDto after =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", category.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(TaskCategoryDto.class);
            //@formatter:on

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_404_when_not_found() {
            final TaskCategoryUpdateDto dto = random.nextObject(TaskCategoryUpdateDto.class);

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

            assertThat(repository.count()).isEqualTo(0);
        }

    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        public void should_delete_and_return_204() {
            var category = persistTaskCategory(randomizeTaskCategory());

            //@formatter:off
            given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}", category.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(0);
        }

        @Test
        public void should_return_404_when_not_found() {
            //@formatter:off
            given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}", 123)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Events")
    class EventTests {

        @Test
        public void should_return_found() {
            var category = persistTaskCategory(randomizeTaskCategory());

            //@formatter:off
            final List<EventDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
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
            assertThat(result.get(0).getEvent()).isEqualTo(Event.EventType.CREATE.name());
            assertThat(result.get(0).getSource()).isEqualTo(Event.SourceType.TASK_CATEGORY.name());
            assertThat(result.get(0).getCreatedBy()).isEqualTo(category.getCreatedBy());
        }

    }

    private TaskCategory randomizeTaskCategory() {
        return random.nextObject(TaskCategory.class);
    }

    private TaskCategory persistTaskCategory(TaskCategory category) {
        return repository.save(category);
    }
}