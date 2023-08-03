package nu.fgv.register.server.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventRepository;
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

public class TaskApiIntegrationTest extends AbstractIntegrationTest {

    private static String basePath;
    private final EasyRandom random;
    @LocalServerPort
    private int localPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository repository;

    @Autowired
    private TaskCategoryRepository categoryRepository;

    @Autowired
    private EventRepository eventRepository;

    public TaskApiIntegrationTest() {
        final EasyRandomParameters parameters = new EasyRandomParameters();
        random = new EasyRandom(parameters);
    }

    @BeforeAll
    public static void beforeClass() {
        basePath = TaskApi.class.getAnnotation(RequestMapping.class).value()[0];
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
        categoryRepository.deleteAll();
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
            //@formatter:off
            final List<TaskDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.tasks", TaskDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        public void should_return_one() {
            var category = persistTaskCategory(randomizeTaskCategory());
            persistTask(randomizeTask(category));

            //@formatter:off
            final List<TaskDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.tasks", TaskDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        public void should_return_many() {
            int size = 42;
            IntStream.range(0, size).forEach(i -> {
                var category = persistTaskCategory(randomizeTaskCategory());
                persistTask(randomizeTask(category));
            });

            //@formatter:off
            final List<TaskDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .queryParam("size", size)
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.tasks", TaskDto.class);
            //@formatter:on

            assertThat(result).hasSize(size);
        }

    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        public void should_create_and_return_201() throws Exception {
            final TaskCreateDto dto = random.nextObject(TaskCreateDto.class);

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

            final TaskDto result = objectMapper.readValue(json, TaskDto.class);
            assertThat(result)
                    .extracting("name")
                    .isEqualTo(dto.getName());
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_400_when_invalid_input() {
            final TaskCreateDto dto = random.nextObject(TaskCreateDto.class);
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

            assertThat(repository.count()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        public void should_return_found() {
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));

            //@formatter:off
            final TaskDto result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", task.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(TaskDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "name")
                    .contains(task.getId(), task.getName());
        }

        @Test
        public void should_return_404_when_not_found() {
            //@formatter:off
            given()
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
            var task = persistTask(randomizeTask(category));

            //@formatter:off
            final TaskDto before =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", task.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(TaskDto.class);
            //@formatter:on

            final TaskUpdateDto dto = TaskUpdateDto.builder()
                    .id(before.getId())
                    .name(before.getName() + "_")
                    .build();

            //@formatter:off
            final String json =
                    given()
                        .contentType(ContentType.JSON)
                        .body(dto)
                    .when()
                        .put("/{id}", task.getId())
                    .then()
                        .statusCode(HttpStatus.ACCEPTED.value())
                        .extract().body().asString();
            //@formatter:on

            final TaskDto updated = objectMapper.readValue(json, TaskDto.class);

            //@formatter:off
            final TaskDto after =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", task.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(TaskDto.class);
            //@formatter:on

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_400_when_invalid_input() {
            final TaskUpdateDto dto = random.nextObject(TaskUpdateDto.class);
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

            assertThat(repository.count()).isEqualTo(0);
        }

        @Test
        public void should_return_404_when_not_found() {
            final TaskUpdateDto dto = random.nextObject(TaskUpdateDto.class);

            //@formatter:off
            given()
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
            var task = persistTask(randomizeTask(category));

            //@formatter:off
            final TaskDto before =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", task.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                    .extract().body().as(TaskDto.class);
            //@formatter:on

            final TaskUpdateDto dto = TaskUpdateDto.builder()
                    .id(before.getId())
                    .name(before.getName() + "_")
                    .build();

            //@formatter:off
            final String json =
                    given()
                        .contentType(ContentType.JSON)
                        .body(dto)
                    .when()
                        .patch("/{id}", task.getId())
                    .then()
                        .statusCode(HttpStatus.ACCEPTED.value())
                        .extract().body().asString();
            //@formatter:on

            final TaskDto updated = objectMapper.readValue(json, TaskDto.class);

            //@formatter:off
            final TaskDto after =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", task.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(TaskDto.class);
            //@formatter:on

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_404_when_not_found() {
            final TaskUpdateDto dto = random.nextObject(TaskUpdateDto.class);

            //@formatter:off
            given()
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
            var task = persistTask(randomizeTask(category));

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}", task.getId())
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

            assertThat(repository.count()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Category")
    class CategoryTests {

        @Test
        public void should_return_found() {
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));

            //@formatter:off
            final TaskCategoryDto result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{taskId}/category", task.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(TaskCategoryDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "name")
                    .contains(category.getId(), category.getName());
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_404_when_not_found() {
            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .get("/{taskId}/category", 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(0);
        }

        @Test
        public void should_update_and_return_202() {
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .put("/{taskId}/category/{id}", task.getId(), category.getId())
            .then()
                .statusCode(HttpStatus.ACCEPTED.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_404_when_updating_and_task_not_found() {
            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .put("/{id}/category/{categoryId}", 1L, 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(0);
        }

        @Test
        public void should_return_404_when_updating_and_category_not_found() {
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .put("/{taskId}/category/{id}", task.getId(), 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_delete_and_return_204() {
            var category = persistTaskCategory(randomizeTaskCategory());
            var task = persistTask(randomizeTask(category));

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .delete("/{taskId}/category", task.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_422_when_removing_and_no_category() {
            var task = persistTask(randomizeTask(null));

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .delete("/{taskId}/category", task.getId())
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_404_when_removing_and_task_not_found() {
            //@formatter:off
            given()
                .contentType(ContentType.JSON)
            .when()
                .delete("/{taskId}/category", 1L)
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
            var task = persistTask(randomizeTask(category));

            //@formatter:off
            final List<EventDto> result =
                    given()
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/events")
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.events", EventDto.class);
            //@formatter:on

            assertThat(eventRepository.count()).isEqualTo(2);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEvent()).isEqualTo(Event.EventType.CREATE.name());
            assertThat(result.get(0).getSource()).isEqualTo(Event.SourceType.TASK.name());
            assertThat(result.get(0).getCreatedBy()).isEqualTo(task.getCreatedBy());
        }

    }

    private Task randomizeTask(TaskCategory category) {
        var task = random.nextObject(Task.class);
        task.setCategory(category);
        return task;
    }

    private TaskCategory randomizeTaskCategory() {
        return random.nextObject(TaskCategory.class);
    }

    private Task persistTask(Task task) {
        return repository.save(task);
    }

    private TaskCategory persistTaskCategory(TaskCategory category) {
        return categoryRepository.save(category);
    }
}
