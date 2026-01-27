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

package nu.fgv.register.server.tag;

import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventRepository;
import nu.fgv.register.server.util.AbstractAuditable;
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
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.inClass;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class TagGraphqlApiIntegrationTest extends AbstractGraphqlIntegrationTest {
    private final EasyRandom random;
    private final TagRepository repository;
    private final EventRepository eventRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public TagGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
                                        final AclCache aclCache,
                                        final Keycloak keycloakAdminClient,
                                        final String keycloakClientId,
                                        final PermissionService permissionService,
                                        final TagRepository repository,
                                        final EventRepository eventRepository,
                                        final ObjectMapper objectMapper) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.repository = repository;
        this.eventRepository = eventRepository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

        parameters
                .excludeField(named("version").and(ofType(Long.class)).and(inClass(AbstractAuditable.class)));
        random = new EasyRandom(parameters);
    }

    @BeforeEach
    void setUp() {
        httpGraphQlTester = HttpGraphQlTester.create(
                WebTestClient.bindToServer()
                        .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath))
                        .build()
        );

        JdbcTestUtils.deleteFromTables(jdbcClient, "tag", "event", "tag_audit");
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
                    .documentName("tag/tagPaged")
                    .execute()
                    .errors()
                    .verify()
                    .path("tagPaged.edges")
                    .entityList(TagDto.class)
                    .hasSize(0);
        }

        @Test
        void should_return_one() {
            final var tag = persistTag(randomizeTag());
            grantReadPermissionToRoleUser(toObjectIdentity(Tag.class, tag.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("tag/tagPaged")
                    .execute()
                    .errors()
                    .verify()
                    .path("tagPaged.edges")
                    .entityList(TagDto.class)
                    .hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            IntStream.range(0, size).forEach(i -> {
                final var tag = persistTag(randomizeTag());
                grantReadPermissionToRoleUser(toObjectIdentity(Tag.class, tag.getId()));
            });

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("tag/tagPaged")
                    .variable("first", size)
                    .execute()
                    .errors()
                    .verify()
                    .path("tagPaged.edges")
                    .entityList(TagDto.class)
                    .hasSize(size);
        }

    }

    @Nested
    @DisplayName("Retrieve paged with filtering")
    class RetrievePagedWithFilteringTests {

        @Test
        void should_return_zero() {
            final var tag = persistTag(randomizeTag());
            grantReadPermissionToRoleUser(toObjectIdentity(Tag.class, tag.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("tag/tagPaged")
                    .variable("filter", Tag_.NAME + ":whatever")
                    .execute()
                    .errors()
                    .verify()
                    .path("tagPaged.edges")
                    .entityList(TagDto.class)
                    .hasSize(0);
        }

        @Test
        void should_return_one() {
            final var tag = persistTag(randomizeTag());
            grantReadPermissionToRoleUser(toObjectIdentity(Tag.class, tag.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("tag/tagPaged")
                    .variable("filter", Tag_.NAME + ":" + tag.getName())
                    .execute()
                    .errors()
                    .verify()
                    .path("tagPaged.edges")
                    .entityList(TagDto.class)
                    .hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            IntStream.range(0, size).forEach(i -> {
                final var tag = randomizeTag();
                if (i % 2 == 0) {
                    tag.setName("whatever");
                }
                grantReadPermissionToRoleUser(toObjectIdentity(Tag.class, persistTag(tag).getId()));
            });

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("tag/tagPaged")
                    .variable("first", size)
                    .variable("filter", Tag_.NAME + ":whatever")
                    .execute()
                    .errors()
                    .verify()
                    .path("tagPaged.edges")
                    .entityList(TagDto.class)
                    .hasSize(size / 2);
        }

    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create() {
            final TagCreateDto dto = random.nextObject(TagCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("tag/tagCreate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .verify()
                    .path("tagCreate", result -> result
                            .path("name").entity(String.class).isEqualTo(dto.name())
                    );

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_BAD_REQUEST_when_invalid_input() {
            final TagCreateDto randDto = random.nextObject(TagCreateDto.class);
            final var dto = randDto.toBuilder()
                    .name("")
                    .build();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("tag/tagCreate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> Objects.requireNonNull(error.getMessage()).contains("name"))
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.BAD_REQUEST.toString()))
                    )
                    .path("tagCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted() {
            final TagCreateDto dto = random.nextObject(TagCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("tag/tagCreate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("tagCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        void should_return_found() {
            final var tag = persistTag(randomizeTag());
            grantReadPermissionToRoleUser(toObjectIdentity(Tag.class, tag.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("tag/tag")
                    .variable("id", tag.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("tag", result -> result
                            .path("name").entity(String.class).isEqualTo(tag.getName())
                    );
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("tag/tag")
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("tag")
                    .valueIsNull();
        }
    }

    @Nested
    @DisplayName("Update")
    class UpdateTests {

        @Test
        void should_update() {
            final var tag = persistTag(randomizeTag());
            grantReadPermissionToRoleUser(toObjectIdentity(Tag.class, tag.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Tag.class, tag.getId()));

            final TagDto before = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("tag/tag")
                    .variable("id", tag.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("tag")
                    .entity(TagDto.class)
                    .get();

            final TagUpdateDto dto = TagUpdateDto.builder()
                    .id(before.getId())
                    .name(before.getName() + "_")
                    .build();

            final TagDto updated = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("tag/tagUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .verify()
                    .path("tagUpdate", result -> result
                            .path("name").entity(String.class).isEqualTo(dto.name())
                    )
                    .entity(TagDto.class)
                    .get();

            final TagDto after = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("tag/tag")
                    .variable("id", tag.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("tag")
                    .entity(TagDto.class)
                    .get();

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_BAD_REQUEST_when_invalid_input() {
            final TagUpdateDto randDto = random.nextObject(TagUpdateDto.class);
            final var dto = randDto.toBuilder()
                    .name("")
                    .build();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("tag/tagUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> Objects.requireNonNull(error.getMessage()).contains("name"))
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.BAD_REQUEST.toString()))
                    )
                    .path("tagUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            final TagUpdateDto dto = random.nextObject(TagUpdateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("tag/tagUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("tagUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_permission() {
            final var tag = persistTag(randomizeTag());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Tag.class, tag.getId()));

            final TagDto before = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("tag/tag")
                    .variable("id", tag.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("tag")
                    .entity(TagDto.class)
                    .get();

            final TagUpdateDto dto = TagUpdateDto.builder()
                    .id(before.getId())
                    .name(before.getName() + "_")
                    .build();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("tag/tagUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("tagUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_role() {
            final TagUpdateDto dto = random.nextObject(TagUpdateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("tag/tagUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("tagUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        void should_delete() {
            final var tag = persistTag(randomizeTag());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Tag.class, tag.getId()));
            grantDeletePermissionToRoleAdmin(toObjectIdentity(Tag.class, tag.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("tag/tagDelete")
                    .variable("id", tag.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("tagDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("tag/tagDelete")
                    .variable("id", 123L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("tagDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_permission() {
            final var tag = persistTag(randomizeTag());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Tag.class, tag.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("tag/tagDelete")
                    .variable("id", tag.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("tagDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("tag/tagDelete")
                    .variable("id", 123L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("tagDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Events")
    class EventTests {

        @Test
        void should_return_found() {
            final var tag = persistTag(randomizeTag());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Tag.class, tag.getId()));

            final List<EventDto> result = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("tag/tagEvents")
                    .variable("sourceId", tag.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("tagEvents")
                    .entityList(EventDto.class)
                    .hasSize(1)
                    .get();

            assertThat(eventRepository.count()).isEqualTo(1);
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getEventType()).isEqualTo(Event.EventType.CREATE.name());
            assertThat(result.getFirst().getSourceType()).isEqualTo(Event.SourceType.TAG.name());
            assertThat(result.getFirst().getCreatedBy()).isEqualTo(tag.getCreatedBy());
        }
    }

    private Tag randomizeTag() {
        return random.nextObject(Tag.class);
    }

    private Tag persistTag(final Tag tag) {
        tag.setId(null);

        return repository.save(tag);
    }
}
