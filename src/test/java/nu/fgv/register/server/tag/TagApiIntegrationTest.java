package nu.fgv.register.server.tag;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.assertj.core.api.Assertions.assertThat;

class TagApiIntegrationTest extends AbstractIntegrationTest {

    private static String basePath;
    private final EasyRandom random;
    @LocalServerPort
    private int localPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TagRepository repository;

    @Autowired
    private EventRepository eventRepository;

    public TagApiIntegrationTest() {
        final EasyRandomParameters parameters = new EasyRandomParameters();
        random = new EasyRandom(parameters);
    }

    @BeforeAll
    public static void beforeClass() {
        basePath = TagApi.class.getAnnotation(RequestMapping.class).value()[0];
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
            final List<TagDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.tags", TagDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_one() {
            var tag = persistTag(randomizeTag());
            grantReadPermissionToRoleUser(toObjectIdentity(Tag.class, tag.getId()));

            //@formatter:off
            final List<TagDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.tags", TagDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            int size = 42;
            IntStream.range(0, size).forEach(i -> {
                var tag = persistTag(randomizeTag());
                grantReadPermissionToRoleUser(toObjectIdentity(Tag.class, tag.getId()));
            });

            //@formatter:off
            final List<TagDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .queryParam("size", size)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.tags", TagDto.class);
            //@formatter:on

            assertThat(result).hasSize(size);
        }
    }

    @Nested
    @DisplayName("Retrieve paged with filtering")
    class RetrievePagedWithFilteringTests {

        @Test
        void should_return_zero() {
            var tag = persistTag(randomizeTag());
            grantReadPermissionToRoleUser(toObjectIdentity(Tag.class, tag.getId()));

            // @formatter:off
            final List<TagDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .queryParam("filter", Tag_.NAME + ":whatever")
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.tags", TagDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_one() {
            var tag = persistTag(randomizeTag());
            grantReadPermissionToRoleUser(toObjectIdentity(Tag.class, tag.getId()));

            //@formatter:off
            final List<TagDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .queryParam("filter", Tag_.NAME + ":" + tag.getName())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.tags", TagDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            int size = 42;
            IntStream.range(0, size).forEach(i ->  {
                var tag = randomizeTag();
                if (i % 2 == 0) {
                    tag.setName("whatever");
                }
                var tag0 = persistTag(tag);
                grantReadPermissionToRoleUser(toObjectIdentity(Tag.class, tag0.getId()));
            });

            //@formatter:off
            final List<TagDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .queryParam("filter", Tag_.NAME + ":whatever")
                        .queryParam("size", size)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.tags", TagDto.class);
            //@formatter:on

            assertThat(result).hasSize(size / 2);
        }
    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create_and_return_201() throws Exception {
            final TagCreateDto dto = random.nextObject(TagCreateDto.class);

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

            final TagDto result = objectMapper.readValue(json, TagDto.class);
            assertThat(result)
                    .extracting("name")
                    .isEqualTo(dto.getName());
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_400_when_invalid_input() {
            final TagCreateDto dto = random.nextObject(TagCreateDto.class);
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
            final TagCreateDto dto = random.nextObject(TagCreateDto.class);

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
            var tag = persistTag(randomizeTag());
            grantReadPermissionToRoleUser(toObjectIdentity(Tag.class, tag.getId()));

            //@formatter:off
            final TagDto result =
                given()
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .contentType(ContentType.JSON)
                .when()
                    .get("/{id}", tag.getId())
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .extract().body().as(TagDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "name")
                    .contains(tag.getId(), tag.getName());
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
            var tag = persistTag(randomizeTag());
            grantReadPermissionToRoleUser(toObjectIdentity(Tag.class, tag.getId()));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Tag.class, tag.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Tag.class, tag.getId()));

            //@formatter:off
            final TagDto before =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", tag.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(TagDto.class);
            //@formatter:on

            final TagUpdateDto dto = TagUpdateDto.builder()
                    .id(before.getId())
                    .name(before.getName() + "_")
                    .build();

            //@formatter:off
            final String json =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                        .contentType(ContentType.JSON)
                        .body(dto)
                    .when()
                        .put("/{id}", tag.getId())
                    .then()
                        .statusCode(HttpStatus.ACCEPTED.value())
                        .extract().body().asString();
            //@formatter:on

            final TagDto updated = objectMapper.readValue(json, TagDto.class);

            //@formatter:off
            final TagDto after =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", tag.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(TagDto.class);
            //@formatter:on

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_400_when_invalid_input() {
            final TagUpdateDto dto = random.nextObject(TagUpdateDto.class);
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
            final TagUpdateDto dto = random.nextObject(TagUpdateDto.class);

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
            final TagUpdateDto dto = random.nextObject(TagUpdateDto.class);

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
            var tag = persistTag(randomizeTag());
            grantReadPermissionToRoleUser(toObjectIdentity(Tag.class, tag.getId()));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Tag.class, tag.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Tag.class, tag.getId()));

            //@formatter:off
            final TagDto before =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", tag.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(TagDto.class);
            //@formatter:on

            final TagUpdateDto dto = TagUpdateDto.builder()
                    .id(before.getId())
                    .name(before.getName() + "_")
                    .build();

            //@formatter:off
            final String json =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                        .contentType(ContentType.JSON)
                        .body(dto)
                    .when()
                        .patch("/{id}", tag.getId())
                    .then()
                        .statusCode(HttpStatus.ACCEPTED.value())
                        .extract().body().asString();
            //@formatter:on

            final TagDto updated = objectMapper.readValue(json, TagDto.class);

            //@formatter:off
            final TagDto after =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", tag.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(TagDto.class);
            //@formatter:on

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_not_found() {
            final TagUpdateDto dto = random.nextObject(TagUpdateDto.class);

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
            final TagUpdateDto dto = random.nextObject(TagUpdateDto.class);

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
            var tag = persistTag(randomizeTag());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Tag.class, tag.getId()));
            grantDeletePermissionToRoleAdmin(toObjectIdentity(Tag.class, tag.getId()));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}", tag.getId())
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
    @DisplayName("Events")
    class EventTests {

        @Test
        void should_return_found() {
            var tag = persistTag(randomizeTag());

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
            assertThat(result.getFirst().getSource()).isEqualTo(Event.SourceType.TAG.name());
            assertThat(result.getFirst().getCreatedBy()).isEqualTo(tag.getCreatedBy());
        }

    }

    private Tag randomizeTag() {
        return random.nextObject(Tag.class);
    }

    private Tag persistTag(Tag tag) {
        return repository.save(tag);
    }
}
