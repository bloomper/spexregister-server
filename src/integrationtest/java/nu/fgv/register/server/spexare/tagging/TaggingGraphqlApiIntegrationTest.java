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

package nu.fgv.register.server.spexare.tagging;

import com.fasterxml.jackson.databind.ObjectMapper;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.tag.TagDto;
import nu.fgv.register.server.tag.TagRepository;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import nu.fgv.register.server.util.graphql.CustomErrorType;
import nu.fgv.register.server.util.randomizer.SocialSecurityNumberRandomizer;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
class TaggingGraphqlApiIntegrationTest extends AbstractGraphqlIntegrationTest {

    private final EasyRandom random;
    private final TaggingRepository repository;
    private final TagRepository tagRepository;
    private final SpexareRepository spexareRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public TaggingGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
                                            final AclCache aclCache,
                                            final Keycloak keycloakAdminClient,
                                            final String keycloakClientId,
                                            final PermissionService permissionService,
                                            final ObjectMapper objectMapper,
                                            final TaggingRepository repository,
                                            final TagRepository tagRepository,
                                            final SpexareRepository spexareRepository) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.repository = repository;
        this.tagRepository = tagRepository;
        this.spexareRepository = spexareRepository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

        parameters
                .randomize(
                        named("socialSecurityNumber"), new SocialSecurityNumberRandomizer()
                )
                .excludeField(named("partner").and(ofType(Spexare.class)).and(inClass(Spexare.class)))
                .excludeField(named("user").and(ofType(User.class)).and(inClass(Spexare.class)))
                .excludeField(named("activities").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("tags").and(ofType(Set.class)).and(inClass(Spexare.class)))
                .excludeField(named("addresses").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("memberships").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("consents").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("toggles").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("tags").and(ofType(List.class)).and(inClass(Spexare.class)))
                .randomizationDepth(1);
        random = new EasyRandom(parameters);
    }

    @BeforeEach
    void setUp() {
        httpGraphQlTester = HttpGraphQlTester.builder(
                        WebTestClient.bindToServer()
                                .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath)))
                .build();

        JdbcTestUtils.deleteFromTables(jdbcClient, "tagging", "tag", "spexare", "event");
    }

    @AfterEach
    void tearDown() {
    }

    @Nested
    @DisplayName("Retrieve paged")
    class RetrievePagedTests {

        @Test
        void should_return_NOT_FOUND() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/tagging/taggingPaged")
                    .variable("spexareId", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("taggingPaged")
                    .valueIsNull();
        }

        @Test
        void should_return_zero() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/tagging/taggingPaged")
                    .variable("spexareId", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("taggingPaged.edges")
                    .entityList(TagDto.class)
                    .hasSize(0);
        }

        @Test
        void should_return_one() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var tag = persistTag(randomizeTag());
            spexare.setTags(Set.of(tag));
            spexareRepository.save(spexare);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/tagging/taggingPaged")
                    .variable("spexareId", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("taggingPaged.edges")
                    .entityList(TagDto.class)
                    .hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var taggings = new ArrayList<Tag>();
            IntStream.range(0, size).forEach(i -> {
                final var tag = persistTag(randomizeTag());
                taggings.add(tag);
            });
            spexare.setTags(Set.copyOf(taggings));
            spexareRepository.save(spexare);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/tagging/taggingPaged")
                    .variable("spexareId", spexare.getId())
                    .variable("first", size)
                    .execute()
                    .errors()
                    .verify()
                    .path("taggingPaged.edges")
                    .entityList(TagDto.class)
                    .hasSize(size);
        }

    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var tag = persistTag(randomizeTag());

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/tagging/taggingCreate")
                    .variable("spexareId", spexare.getId())
                    .variable("tagId", tag.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("taggingCreate")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare.taggings")
                    .entityList(TagDto.class)
                    .hasSize(1);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare.taggingsPaged.edges")
                    .entityList(TagDto.class)
                    .hasSize(1);

            assertThat(repository.countTaggings()).isEqualTo(1);
        }

        @Test
        void should_return_CONFLICT_when_creating_already_existing_value() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var tag = persistTag(randomizeTag());

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/tagging/taggingCreate")
                    .variable("spexareId", spexare.getId())
                    .variable("tagId", tag.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("taggingCreate")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/tagging/taggingCreate")
                    .variable("spexareId", spexare.getId())
                    .variable("tagId", tag.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(CustomErrorType.CONFLICT.toString()))
                    )
                    .path("taggingCreate")
                    .valueIsNull();

            assertThat(repository.countTaggings()).isEqualTo(1);
        }

        @Test
        void should_return_NOT_FOUND_when_creating_and_spexare_not_found() {
            final var tag = persistTag(randomizeTag());

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/tagging/taggingCreate")
                    .variable("spexareId", 1L)
                    .variable("tagId", tag.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("taggingCreate")
                    .valueIsNull();

            assertThat(repository.countTaggings()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_permission() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var tag = persistTag(randomizeTag());

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/tagging/taggingCreate")
                    .variable("spexareId", spexare.getId())
                    .variable("tagId", tag.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("taggingCreate")
                    .valueIsNull();

            assertThat(repository.countTaggings()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_role() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var tag = persistTag(randomizeTag());

            httpGraphQlTester
                    .mutate()
                    .build()
                    .documentName("spexare/tagging/taggingCreate")
                    .variable("spexareId", spexare.getId())
                    .variable("tagId", tag.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("taggingCreate")
                    .valueIsNull();

            assertThat(repository.countTaggings()).isZero();
        }
    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        void should_delete() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var tag = persistTag(randomizeTag());

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/tagging/taggingCreate")
                    .variable("spexareId", spexare.getId())
                    .variable("tagId", tag.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("taggingCreate")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare.taggings")
                    .entityList(TagDto.class)
                    .hasSize(1);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare.taggingsPaged.edges")
                    .entityList(TagDto.class)
                    .hasSize(1);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/tagging/taggingDelete")
                    .variable("spexareId", spexare.getId())
                    .variable("tagId", tag.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("taggingDelete")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare.taggings")
                    .entityList(TagDto.class)
                    .hasSize(0);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare.taggingsPaged.edges")
                    .entityList(TagDto.class)
                    .hasSize(0);

            assertThat(repository.countTaggings()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_deleting_non_existing_value() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/tagging/taggingDelete")
                    .variable("spexareId", spexare.getId())
                    .variable("tagId", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("taggingDelete")
                    .valueIsNull();

            assertThat(repository.countTaggings()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_deleting_and_spexare_not_found() {
            final var tag = persistTag(randomizeTag());

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/tagging/taggingDelete")
                    .variable("spexareId", -1L)
                    .variable("tagId", tag.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("taggingDelete")
                    .valueIsNull();

            assertThat(repository.countTaggings()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_permission() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var tag = persistTag(randomizeTag());

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/tagging/taggingCreate")
                    .variable("spexareId", spexare.getId())
                    .variable("tagId", tag.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("taggingCreate")
                    .valueIsNull();

            revokeWritePermissionFromRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/tagging/taggingDelete")
                    .variable("spexareId", spexare.getId())
                    .variable("tagId", tag.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("taggingDelete")
                    .valueIsNull();

            assertThat(repository.countTaggings()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_role() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var tag = persistTag(randomizeTag());

            httpGraphQlTester
                    .mutate()
                    .build()
                    .documentName("spexare/tagging/taggingDelete")
                    .variable("spexareId", spexare.getId())
                    .variable("tagId", tag.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("taggingDelete")
                    .valueIsNull();

            assertThat(repository.countTaggings()).isZero();
        }
    }

    private Tag randomizeTag() {
        return random.nextObject(Tag.class);
    }

    private Tag persistTag(final Tag tag) {
        tag.setId(null);

        return tagRepository.save(tag);
    }

    private Spexare randomizeSpexare() {
        return random.nextObject(Spexare.class);
    }

    private Spexare persistSpexare(final Spexare spexare) {
        spexare.setId(null);

        return spexareRepository.save(spexare);
    }

}
