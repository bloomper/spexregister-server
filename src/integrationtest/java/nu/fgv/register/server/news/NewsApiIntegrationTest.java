/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nu.fgv.register.server.news;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
import nu.fgv.register.server.acl.PermissionService;
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
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class NewsApiIntegrationTest extends AbstractIntegrationTest {

    private final EasyRandom random;
    private final NewsRepository repository;
    private final EventRepository eventRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public NewsApiIntegrationTest(final JdbcClient jdbcClient,
                                  final AclCache aclCache,
                                  final Keycloak keycloakAdminClient,
                                  final String keycloakClientId,
                                  final PermissionService permissionService,
                                  final NewsRepository repository,
                                  final EventRepository eventRepository) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService);
        this.repository = repository;
        this.eventRepository = eventRepository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

        random = new EasyRandom(parameters);
    }

    @BeforeAll
    public static void beforeClass() {
        basePath = NewsApi.class.getAnnotation(RequestMapping.class).value()[0];
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = localPort;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        final RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder();
        requestSpecBuilder.setBasePath(basePath);
        RestAssured.requestSpecification = requestSpecBuilder.build();
        config = config()
                .encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))
                .logConfig(LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails());

        JdbcTestUtils.deleteFromTables(jdbcClient, "news", "event");
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
            final List<NewsDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
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
        void should_return_one() {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleUser(toObjectIdentity(News.class, news.getId()));

            //@formatter:off
            final List<NewsDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
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
        void should_return_many() {
            final int size = 42;
            IntStream.range(0, size).forEach(i -> {
                final var news = persistNews(randomizeNews());
                grantReadPermissionToRoleUser(toObjectIdentity(News.class, news.getId()));
            });

            //@formatter:off
            final List<NewsDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .queryParam("size", size)
                    .when()
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
    @DisplayName("Retrieve paged with filtering")
    class RetrievePagedWithFilteringTests {

        @Test
        void should_return_zero() {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleUser(toObjectIdentity(News.class, news.getId()));

            //@formatter:off
            final List<NewsDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .queryParam("filter", News_.SUBJECT + ":whatever")
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
        void should_return_one() {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleUser(toObjectIdentity(News.class, news.getId()));

            //@formatter:off
            final List<NewsDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .queryParam("filter", News_.SUBJECT + ":" + news.getSubject())
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
        void should_return_many() {
            final int size = 42;
            IntStream.range(0, size).forEach(i -> {
                final var news = randomizeNews();
                if (i % 2 == 0) {
                    news.setSubject("whatever");
                }
                grantReadPermissionToRoleUser(toObjectIdentity(News.class, persistNews(news).getId()));
            });

            //@formatter:off
            final List<NewsDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .queryParam("filter", News_.SUBJECT + ":whatever")
                        .queryParam("size", size)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.news", NewsDto.class);
            //@formatter:on

            assertThat(result).hasSize(size / 2);
        }

    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create_and_return_201() throws Exception {
            final NewsCreateDto dto = random.nextObject(NewsCreateDto.class);

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

            final NewsDto result = objectMapper.readValue(json, NewsDto.class);
            assertThat(result)
                    .extracting("subject", "text", "visibleFrom")
                    .contains(dto.subject(), dto.text(), dto.visibleFrom());
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_400_when_invalid_input() {
            final NewsCreateDto randDto = random.nextObject(NewsCreateDto.class);
            final var dto = randDto.toBuilder()
                    .subject(null)
                    .build();

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .body(dto)
            .when()
                .post()
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("status", equalTo(HttpStatus.BAD_REQUEST.value()))
                .body("errors", notNullValue())
                .body("errors.subject", notNullValue());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_403_when_not_permitted() {
            final NewsCreateDto dto = random.nextObject(NewsCreateDto.class);

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .body(dto)
            .when()
                .post()
            .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }
    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        void should_return_found() {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleUser(toObjectIdentity(News.class, news.getId()));

            //@formatter:off
            final NewsDto result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", news.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(NewsDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "subject", "text", "visibleFrom")
                    .contains(news.getId(), news.getSubject(), news.getText(), news.getVisibleFrom());
        }

        @Test
        void should_return_404_when_not_found() {
            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .get("/{id}", 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }

    @Nested
    @DisplayName("Update")
    class UpdateTests {

        @Test
        void should_update_and_return_200() throws Exception {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleUser(toObjectIdentity(News.class, news.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(News.class, news.getId()));

            //@formatter:off
            final NewsDto before =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
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
                    .build();

            //@formatter:off
            final String json =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                        .contentType(ContentType.JSON)
                        .body(dto)
                    .when()
                        .put("/{id}", news.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().asString();
            //@formatter:on

            final NewsDto updated = objectMapper.readValue(json, NewsDto.class);

            //@formatter:off
            final NewsDto after =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
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
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_400_when_invalid_input() {
            final NewsUpdateDto randDto = random.nextObject(NewsUpdateDto.class);
            final var dto = randDto.toBuilder()
                    .subject(null)
                    .build();

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .body(dto)
            .when()
                .put("/{id}", dto.id())
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("status", equalTo(HttpStatus.BAD_REQUEST.value()))
                .body("errors", notNullValue())
                .body("errors.subject", notNullValue());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_not_found() {
            final NewsUpdateDto dto = random.nextObject(NewsUpdateDto.class);

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .body(dto)
            .when()
                .put("/{id}", dto.id())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_permission() {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleAdmin(toObjectIdentity(News.class, news.getId()));

            //@formatter:off
            final NewsDto before =
                given()
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
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
                    .build();

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .body(dto)
            .when()
                .put("/{id}", dto.id())
            .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_role() {
            final NewsUpdateDto dto = random.nextObject(NewsUpdateDto.class);

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .body(dto)
            .when()
                .put("/{id}", dto.id())
            .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }
    }

    @Nested
    @DisplayName("Partial update")
    class PartialUpdateTests {

        @Test
        void should_update_and_return_200() throws Exception {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleUser(toObjectIdentity(News.class, news.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(News.class, news.getId()));

            //@formatter:off
            final NewsDto before =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
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
                    .visibleFrom(LocalDate.now().minusDays(3))
                    .build();

            //@formatter:off
            final String json =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                        .contentType(ContentType.JSON)
                        .body(dto)
                    .when()
                        .patch("/{id}", news.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().asString();
            //@formatter:on

            final NewsDto updated = objectMapper.readValue(json, NewsDto.class);

            //@formatter:off
            final NewsDto after =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
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
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_404_when_not_found() {
            final NewsUpdateDto dto = random.nextObject(NewsUpdateDto.class);

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .body(dto)
            .when()
                .patch("/{id}", dto.id())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_permission() {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleAdmin(toObjectIdentity(News.class, news.getId()));

            //@formatter:off
            final NewsDto before =
                given()
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
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
                    .visibleFrom(LocalDate.now().minusDays(3))
                    .build();

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
                .body(dto)
            .when()
                .patch("/{id}", dto.id())
            .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_role() {
            final NewsUpdateDto dto = random.nextObject(NewsUpdateDto.class);

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .body(dto)
            .when()
                .patch("/{id}", dto.id())
            .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        void should_delete_and_return_204() {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleAdmin(toObjectIdentity(News.class, news.getId()));
            grantDeletePermissionToRoleAdmin(toObjectIdentity(News.class, news.getId()));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}", news.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_not_found() {
            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}", 123)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_permission() {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleAdmin(toObjectIdentity(News.class, news.getId()));

            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}", news.getId())
            .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_role() {
            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}", 123)
            .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }
    }

    @Nested
    @DisplayName("Permissions")
    class PermissionTests {

        @Test
        void should_create_non_published() throws Exception {
            final NewsCreateDto randDto = random.nextObject(NewsCreateDto.class);
            final var dto = randDto.toBuilder()
                    .visibleFrom(LocalDate.now().plusDays(1))
                    .visibleTo(LocalDate.now().plusDays(2))
                    .build();

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

            final NewsDto created = objectMapper.readValue(json, NewsDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .get("/{id}", created.getId())
            .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
            //@formatter:on

            //@formatter:off
            final List<NewsDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.news", NewsDto.class);
            //@formatter:on

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isEmpty();
        }

        @Test
        void should_create_published() throws Exception {
            final NewsCreateDto randDto = random.nextObject(NewsCreateDto.class);
            final var dto = randDto.toBuilder()
                    .visibleFrom(LocalDate.now().minusDays(1))
                    .visibleTo(LocalDate.now().plusDays(2))
                    .build();

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

            final NewsDto created = objectMapper.readValue(json, NewsDto.class);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .get("/{id}", created.getId())
            .then()
                .statusCode(HttpStatus.OK.value());
            //@formatter:on

            //@formatter:off
            final List<NewsDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
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
    }

    @Nested
    @DisplayName("Events")
    class EventTests {

        @Test
        void should_return_found() {
            final var news = persistNews(randomizeNews());

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
            assertThat(result.getFirst().getSource()).isEqualTo(Event.SourceType.NEWS.name());
            assertThat(result.getFirst().getCreatedBy()).isEqualTo(news.getCreatedBy());
        }

        @Test
        void should_return_403_when_not_permitted() {
            //@formatter:off
            final ProblemDetail result = given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .get("/events")
            .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .extract().body().as(ProblemDetail.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }
    }

    private News randomizeNews() {
        final var news = random.nextObject(News.class);

        news.setPublished(true);

        return news;
    }

    private News persistNews(final News news) {
        news.setId(null);

        return repository.save(news);
    }

}
