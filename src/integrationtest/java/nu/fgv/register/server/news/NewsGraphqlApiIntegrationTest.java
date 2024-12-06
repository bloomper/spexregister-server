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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventRepository;
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class NewsGraphqlApiIntegrationTest extends AbstractGraphqlIntegrationTest {
    private final EasyRandom random;
    private final NewsRepository repository;
    private final EventRepository eventRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public NewsGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
                                         final AclCache aclCache,
                                         final Keycloak keycloakAdminClient,
                                         final String keycloakClientId,
                                         final PermissionService permissionService,
                                         final ObjectMapper objectMapper,
                                         final NewsRepository repository,
                                         final EventRepository eventRepository) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.repository = repository;
        this.eventRepository = eventRepository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

        random = new EasyRandom(parameters);
    }

    @BeforeEach
    void setUp() {
        httpGraphQlTester = HttpGraphQlTester.builder(
                        WebTestClient.bindToServer()
                                .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath)))
                .build();

        JdbcTestUtils.deleteFromTables(jdbcClient, "news", "event");
    }

    @AfterEach
    void tearDown() {
    }

    @Nested
    @DisplayName("Retrieve paged")
    class RetrievePagedTests {

        @Test
        void should_return_zero() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("news/newsPaged")
                    .execute()
                    .errors()
                    .verify()
                    .path("newsPaged.edges")
                    .entityList(NewsDto.class)
                    .hasSize(0);
        }

        @Test
        void should_return_one() {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleUser(toObjectIdentity(News.class, news.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("news/newsPaged")
                    .execute()
                    .errors()
                    .verify()
                    .path("newsPaged.edges")
                    .entityList(NewsDto.class)
                    .hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            IntStream.range(0, size).forEach(i -> {
                final var news = persistNews(randomizeNews());
                grantReadPermissionToRoleUser(toObjectIdentity(News.class, news.getId()));
            });

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("news/newsPaged")
                    .variable("first", size)
                    .execute()
                    .errors()
                    .verify()
                    .path("newsPaged.edges")
                    .entityList(NewsDto.class)
                    .hasSize(size);
        }

    }

    @Nested
    @DisplayName("Retrieve paged with filtering")
    class RetrievePagedWithFilteringTests {

        @Test
        void should_return_zero() {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleUser(toObjectIdentity(News.class, news.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("news/newsPaged")
                    .variable("filter", News_.SUBJECT + ":whatever")
                    .execute()
                    .errors()
                    .verify()
                    .path("newsPaged.edges")
                    .entityList(NewsDto.class)
                    .hasSize(0);
        }

        @Test
        void should_return_one() {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleUser(toObjectIdentity(News.class, news.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("news/newsPaged")
                    .variable("filter", News_.SUBJECT + ":" + news.getSubject())
                    .execute()
                    .errors()
                    .verify()
                    .path("newsPaged.edges")
                    .entityList(NewsDto.class)
                    .hasSize(1);
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

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("news/newsPaged")
                    .variable("first", size)
                    .variable("filter", News_.SUBJECT + ":whatever")
                    .execute()
                    .errors()
                    .verify()
                    .path("newsPaged.edges")
                    .entityList(NewsDto.class)
                    .hasSize(size / 2);
        }

    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create() {
            final NewsCreateDto dto = random.nextObject(NewsCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("news/newsCreate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .verify()
                    .path("newsCreate", result -> result
                            .path("subject").entity(String.class).isEqualTo(dto.getSubject())
                            .path("text").entity(String.class).isEqualTo(dto.getText())
                            .path("visibleFrom").entity(LocalDate.class).isEqualTo(dto.getVisibleFrom())
                    );

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_BAD_REQUEST_when_invalid_input() {
            final NewsCreateDto dto = random.nextObject(NewsCreateDto.class);
            dto.setSubject("");

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("news/newsCreate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> Objects.requireNonNull(error.getMessage()).contains("subject"))
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.BAD_REQUEST.toString()))
                    )
                    .path("newsCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted() {
            final NewsCreateDto dto = random.nextObject(NewsCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("news/newsCreate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("newsCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        void should_return_found() {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleUser(toObjectIdentity(News.class, news.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("news/news")
                    .variable("id", news.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("news", result -> result
                            .path("subject").entity(String.class).isEqualTo(news.getSubject())
                            .path("text").entity(String.class).isEqualTo(news.getText())
                            .path("visibleFrom").entity(LocalDate.class).isEqualTo(news.getVisibleFrom())
                    );
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("news/news")
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("news")
                    .valueIsNull();
        }
    }

    @Nested
    @DisplayName("Update")
    class UpdateTests {

        @Test
        void should_update() {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleUser(toObjectIdentity(News.class, news.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(News.class, news.getId()));

            final NewsDto before = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("news/news")
                    .variable("id", news.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("news")
                    .entity(NewsDto.class)
                    .get();

            final NewsUpdateDto dto = NewsUpdateDto.builder()
                    .id(before.getId())
                    .subject(before.getSubject() + "_")
                    .text(before.getText())
                    .visibleFrom(before.getVisibleFrom())
                    .build();

            final NewsDto updated = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("news/newsUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .verify()
                    .path("newsUpdate", result -> result
                            .path("subject").entity(String.class).isEqualTo(dto.getSubject())
                            .path("text").entity(String.class).isEqualTo(dto.getText())
                            .path("visibleFrom").entity(LocalDate.class).isEqualTo(dto.getVisibleFrom())
                    )
                    .entity(NewsDto.class)
                    .get();

            final NewsDto after = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("news/news")
                    .variable("id", news.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("news")
                    .entity(NewsDto.class)
                    .get();

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_BAD_REQUEST_when_invalid_input() {
            final NewsUpdateDto dto = random.nextObject(NewsUpdateDto.class);
            dto.setSubject("");

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("news/newsUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> Objects.requireNonNull(error.getMessage()).contains("subject"))
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.BAD_REQUEST.toString()))
                    )
                    .path("newsUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            final NewsUpdateDto dto = random.nextObject(NewsUpdateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("news/newsUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("newsUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_permission() {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleAdmin(toObjectIdentity(News.class, news.getId()));

            final NewsDto before = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("news/news")
                    .variable("id", news.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("news")
                    .entity(NewsDto.class)
                    .get();

            final NewsUpdateDto dto = NewsUpdateDto.builder()
                    .id(before.getId())
                    .subject(before.getSubject() + "_")
                    .text(before.getText())
                    .visibleFrom(before.getVisibleFrom())
                    .build();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("news/newsUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("newsUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_role() {
            final NewsUpdateDto dto = random.nextObject(NewsUpdateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("news/newsUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("newsUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        void should_delete() {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleAdmin(toObjectIdentity(News.class, news.getId()));
            grantDeletePermissionToRoleAdmin(toObjectIdentity(News.class, news.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("news/newsDelete")
                    .variable("id", news.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("newsDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("news/newsDelete")
                    .variable("id", 123L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("newsDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_permission() {
            final var news = persistNews(randomizeNews());
            grantReadPermissionToRoleAdmin(toObjectIdentity(News.class, news.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("news/newsDelete")
                    .variable("id", news.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("newsDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("news/newsDelete")
                    .variable("id", 123L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("newsDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Permissions")
    class PermissionTests {

        @Test
        void should_create_non_published() {
            final NewsCreateDto dto = random.nextObject(NewsCreateDto.class);
            dto.setVisibleFrom(LocalDate.now().plusDays(1));
            dto.setVisibleTo(LocalDate.now().plusDays(2));

            final NewsDto created = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("news/newsCreate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .verify()
                    .path("newsCreate")
                    .entity(NewsDto.class)
                    .get();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("news/news")
                    .variable("id", created.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("news")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("news/newsPaged")
                    .execute()
                    .errors()
                    .verify()
                    .path("newsPaged.edges")
                    .entityList(NewsDto.class)
                    .hasSize(0);

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_create_published() {
            final NewsCreateDto dto = random.nextObject(NewsCreateDto.class);
            dto.setVisibleFrom(LocalDate.now().minusDays(1));
            dto.setVisibleTo(LocalDate.now().plusDays(2));

            final NewsDto created = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("news/newsCreate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .verify()
                    .path("newsCreate")
                    .entity(NewsDto.class)
                    .get();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("news/news")
                    .variable("id", created.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("news")
                    .hasValue();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("news/newsPaged")
                    .execute()
                    .errors()
                    .verify()
                    .path("newsPaged.edges")
                    .entityList(NewsDto.class)
                    .hasSize(1);

            assertThat(repository.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Events")
    class EventTests {

        @Test
        void should_return_found() {
            final var news = persistNews(randomizeNews());

            final List<EventDto> result = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("news/newsEvents")
                    .execute()
                    .errors()
                    .verify()
                    .path("newsEvents")
                    .entityList(EventDto.class)
                    .hasSize(1)
                    .get();

            assertThat(eventRepository.count()).isEqualTo(1);
            assertThat(result.getFirst().getEvent()).isEqualTo(Event.EventType.CREATE.name());
            assertThat(result.getFirst().getSource()).isEqualTo(Event.SourceType.NEWS.name());
            assertThat(result.getFirst().getCreatedBy()).isEqualTo(news.getCreatedBy());
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("news/newsEvents")
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("newsEvents")
                    .valueIsNull();
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
