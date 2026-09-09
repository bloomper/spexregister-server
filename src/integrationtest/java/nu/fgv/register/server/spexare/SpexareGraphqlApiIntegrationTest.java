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

package nu.fgv.register.server.spexare;

import jakarta.persistence.EntityManager;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventRepository;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.util.AbstractAuditable;
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import nu.fgv.register.server.util.randomizer.SocialSecurityNumberRandomizer;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.ApiVersionInserter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
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
class SpexareGraphqlApiIntegrationTest extends AbstractGraphqlIntegrationTest {

    private final EasyRandom random;
    private final SpexareRepository repository;
    private final EventRepository eventRepository;
    private final EntityManager entityManager;
    private final PlatformTransactionManager transactionManager;

    @Value("${spring.jpa.properties.hibernate.search.backend.directory.root")
    private String indexDataLocation;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public SpexareGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
                                            final AclCache aclCache,
                                            final Keycloak keycloakAdminClient,
                                            final String keycloakClientId,
                                            final PermissionService permissionService,
                                            final SpexareRepository repository,
                                            final EventRepository eventRepository,
                                            final ObjectMapper objectMapper,
                                            final EntityManager entityManager,
                                            final PlatformTransactionManager transactionManager) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.entityManager = entityManager;
        this.transactionManager = transactionManager;

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
                .excludeField(named("version").and(ofType(Long.class)).and(inClass(AbstractAuditable.class)))
                .randomizationDepth(1);
        random = new EasyRandom(parameters);
    }

    @BeforeEach
    void setUp() throws IOException {
        restTestClient = RestTestClient
                .bindToServer()
                .baseUrl("http://localhost:%s/api/spexare".formatted(localPort))
                .apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
                .build();

        httpGraphQlTester = HttpGraphQlTester.create(
                WebTestClient.bindToServer()
                        .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath))
                        .build()
        );

        jdbcClient.sql("SELECT id FROM spexare WHERE partner_id IS NOT NULL")
                .query()
                .listOfRows()
                .forEach(row ->
                        jdbcClient
                                .sql("UPDATE spexare SET partner_id = NULL WHERE id = :id")
                                .param("id", row.get("id"))
                                .update()
                );
        JdbcTestUtils.deleteFromTables(jdbcClient, "spexare", "event", "spexare_audit");
        Files.deleteIfExists(Path.of(indexDataLocation, "spexare"));
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
                    .documentName("spexare/spexarePaged")
                    .execute()
                    .errors()
                    .verify()
                    .path("spexarePaged.edges")
                    .entityList(SpexareDto.class)
                    .hasSize(0);
        }

        @Test
        void should_return_one() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexarePaged")
                    .execute()
                    .errors()
                    .verify()
                    .path("spexarePaged.edges")
                    .entityList(SpexareDto.class)
                    .hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            IntStream.range(0, size).forEach(i -> {
                final var spexare = persistSpexare(randomizeSpexare());
                grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            });

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexarePaged")
                    .variable("first", size)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexarePaged.edges")
                    .entityList(SpexareDto.class)
                    .hasSize(size);
        }

    }

    @Nested
    @DisplayName("Retrieve paged backwards")
    class RetrievePagedBackwardsTests {

        @Test
        void should_return_last_page() {
            final var ids = persistPermittedSpexare(25);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexarePagedCursor")
                    .variable("last", 10)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexarePaged.totalCount").entity(Integer.class).isEqualTo(25)
                    .path("spexarePaged.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(false)
                    .path("spexarePaged.pageInfo.hasPreviousPage").entity(Boolean.class).isEqualTo(true)
                    .path("spexarePaged.edges[*].node.id").entityList(String.class).isEqualTo(asStrings(ids.subList(15, 25)));
        }

        @Test
        void should_return_everything_when_fewer_than_requested() {
            final var ids = persistPermittedSpexare(5);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexarePagedCursor")
                    .variable("last", 10)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexarePaged.totalCount").entity(Integer.class).isEqualTo(5)
                    .path("spexarePaged.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(false)
                    .path("spexarePaged.pageInfo.hasPreviousPage").entity(Boolean.class).isEqualTo(false)
                    .path("spexarePaged.edges[*].node.id").entityList(String.class).isEqualTo(asStrings(ids));
        }

        @Test
        void should_return_zero() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexarePagedCursor")
                    .variable("last", 10)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexarePaged.totalCount").entity(Integer.class).isEqualTo(0)
                    .path("spexarePaged.edges").entityList(SpexareDto.class).hasSize(0);
        }

        @Test
        void should_page_backwards_from_last_page() {
            final var ids = persistPermittedSpexare(25);

            final String startCursor = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexarePagedCursor")
                    .variable("last", 10)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexarePaged.pageInfo.startCursor")
                    .entity(String.class)
                    .get();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexarePagedCursor")
                    .variable("last", 10)
                    .variable("before", startCursor)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexarePaged.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(true)
                    .path("spexarePaged.pageInfo.hasPreviousPage").entity(Boolean.class).isEqualTo(true)
                    .path("spexarePaged.edges[*].node.id").entityList(String.class).isEqualTo(asStrings(ids.subList(5, 15)));
        }

        @Test
        void should_round_trip_forwards_to_last_page() {
            final var ids = persistPermittedSpexare(25);

            final String endCursor = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexarePagedCursor")
                    .variable("first", 15)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexarePaged.pageInfo.endCursor")
                    .entity(String.class)
                    .get();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexarePagedCursor")
                    .variable("first", 10)
                    .variable("after", endCursor)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexarePaged.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(false)
                    .path("spexarePaged.edges[*].node.id").entityList(String.class).isEqualTo(asStrings(ids.subList(15, 25)));
        }

        private List<String> asStrings(final List<Long> ids) {
            return ids.stream().map(String::valueOf).toList();
        }

        private List<Long> persistPermittedSpexare(final int size) {
            return IntStream.range(0, size)
                    .mapToObj(_ -> {
                        final var spexare = persistSpexare(randomizeSpexare());
                        grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
                        return spexare.getId();
                    })
                    .toList();
        }
    }

    @Nested
    @DisplayName("Retrieve paged with filtering")
    class RetrieveWithFilteringPagedTests {

        @Test
        void should_return_zero() {
            persistSpexare(randomizeSpexare());

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexarePaged")
                    .variable("filter", Spexare_.FIRST_NAME + ":whatever")
                    .execute()
                    .errors()
                    .verify()
                    .path("spexarePaged.edges")
                    .entityList(SpexareDto.class)
                    .hasSize(0);
        }

        @Test
        void should_return_one() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexarePaged")
                    .variable("filter", Spexare_.FIRST_NAME + ":" + spexare.getFirstName())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexarePaged.edges")
                    .entityList(SpexareDto.class)
                    .hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            IntStream.range(0, size).forEach(i -> {
                final var spexare = randomizeSpexare();
                if (i % 2 == 0) {
                    spexare.setFirstName("whatever");
                }
                final var spexare0 = persistSpexare(spexare);
                grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare0.getId()));
            });

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexarePaged")
                    .variable("filter", Spexare_.FIRST_NAME + ":whatever")
                    .variable("first", size)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexarePaged.edges")
                    .entityList(SpexareDto.class)
                    .hasSize(size / 2);
        }

    }

    @Nested
    @DisplayName("Search paged")
    class SearchPagedTests {

        @Test
        void should_return_zero() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexareSearchPaged")
                    .variable("q", "whatever")
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareSearchPaged.edges")
                    .entityList(SpexareDto.class)
                    .hasSize(0);
        }

        @Test
        void should_return_one() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            syncIndex();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexareSearchPaged")
                    .variable("q", spexare.getNickName())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareSearchPaged.edges")
                    .entityList(SpexareDto.class)
                    .hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            IntStream.range(0, size).forEach(i -> {
                final var spexare = persistSpexare(randomizeSpexare());
                spexare.setFirstName("firstName");
                repository.save(spexare);
                grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            });
            syncIndex();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexareSearchPaged")
                    .variable("q", "firstName")
                    .variable("first", size)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareSearchPaged.edges")
                    .entityList(SpexareDto.class)
                    .hasSize(size);
        }

        @Test
        void should_return_zero_if_not_published_and_not_permitted() {
            final var spexare = persistSpexare(randomizeSpexare(false));
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            syncIndex();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexareSearchPaged")
                    .variable("q", spexare.getNickName())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareSearchPaged.edges")
                    .entityList(SpexareDto.class)
                    .hasSize(0);
        }

        @Test
        void should_return_one_if_not_published_and_permitted() {
            final var spexare = persistSpexare(randomizeSpexare(false));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            syncIndex();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexareSearchPaged")
                    .variable("q", spexare.getNickName())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareSearchPaged.edges")
                    .entityList(SpexareDto.class)
                    .hasSize(1);
        }
    }

    @Nested
    @DisplayName("Search paged backwards")
    class SearchPagedBackwardsTests {

        @Test
        void should_return_last_page() {
            final int size = 25;
            IntStream.range(0, size).forEach(_ -> {
                final var spexare = persistSpexare(randomizeSpexare());
                spexare.setFirstName("firstName");
                repository.save(spexare);
                grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            });
            syncIndex();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexareSearchPagedCursor")
                    .variable("q", "firstName")
                    .variable("last", 10)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareSearchPaged.totalCount").entity(Integer.class).isEqualTo(size)
                    .path("spexareSearchPaged.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(false)
                    .path("spexareSearchPaged.pageInfo.hasPreviousPage").entity(Boolean.class).isEqualTo(true)
                    .path("spexareSearchPaged.edges").entityList(SpexareDto.class).hasSize(10);
        }

        @Test
        void should_return_everything_when_fewer_than_requested() {
            final int size = 5;
            IntStream.range(0, size).forEach(_ -> {
                final var spexare = persistSpexare(randomizeSpexare());
                spexare.setFirstName("firstName");
                repository.save(spexare);
                grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            });
            syncIndex();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexareSearchPagedCursor")
                    .variable("q", "firstName")
                    .variable("last", 10)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareSearchPaged.totalCount").entity(Integer.class).isEqualTo(size)
                    .path("spexareSearchPaged.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(false)
                    .path("spexareSearchPaged.pageInfo.hasPreviousPage").entity(Boolean.class).isEqualTo(false)
                    .path("spexareSearchPaged.edges").entityList(SpexareDto.class).hasSize(size);
        }

        @Test
        void should_page_backwards_from_last_page() {
            final int size = 25;
            IntStream.range(0, size).forEach(_ -> {
                final var spexare = persistSpexare(randomizeSpexare());
                spexare.setFirstName("firstName");
                repository.save(spexare);
                grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            });
            syncIndex();

            final String startCursor = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexareSearchPagedCursor")
                    .variable("q", "firstName")
                    .variable("last", 10)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareSearchPaged.pageInfo.startCursor")
                    .entity(String.class)
                    .get();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexareSearchPagedCursor")
                    .variable("q", "firstName")
                    .variable("last", 10)
                    .variable("before", startCursor)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareSearchPaged.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(true)
                    .path("spexareSearchPaged.pageInfo.hasPreviousPage").entity(Boolean.class).isEqualTo(true)
                    .path("spexareSearchPaged.edges").entityList(SpexareDto.class).hasSize(10);
        }

    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create() {
            final SpexareCreateDto dto = random.nextObject(SpexareCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexareCreate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareCreate", result -> result
                            .path("firstName").entity(String.class).isEqualTo(dto.firstName())
                            .path("lastName").entity(String.class).isEqualTo(dto.lastName())
                            .path("nickName").entity(String.class).isEqualTo(dto.nickName())
                    );

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_BAD_REQUEST_when_invalid_input() {
            final SpexareCreateDto randDto = random.nextObject(SpexareCreateDto.class);
            final var dto = randDto.toBuilder()
                    .lastName("")
                    .build();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexareCreate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> Objects.requireNonNull(error.getMessage()).contains("lastName"))
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.BAD_REQUEST.toString()))
                    )
                    .path("spexareCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted() {
            final SpexareCreateDto dto = random.nextObject(SpexareCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexareCreate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexareCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        void should_return_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare", result -> result
                            .path("id").entity(Long.class).isEqualTo(spexare.getId())
                            .path("firstName").entity(String.class).isEqualTo(spexare.getFirstName())
                            .path("lastName").entity(String.class).isEqualTo(spexare.getLastName())
                            .path("nickName").entity(String.class).isEqualTo(spexare.getNickName())
                    );
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spexare")
                    .valueIsNull();
        }
    }

    @Nested
    @DisplayName("Update")
    class UpdateTests {

        @Test
        void should_update() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantAdministrationPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));

            final SpexareDto before = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare")
                    .entity(SpexareDto.class)
                    .get();

            final SpexareUpdateDto dto = SpexareUpdateDto.builder()
                    .id(before.getId())
                    .firstName(before.getFirstName() + "_")
                    .lastName(before.getLastName())
                    .nickName(before.getNickName())
                    .deceased(Boolean.FALSE)
                    .published(Boolean.TRUE)
                    .build();

            final SpexareDto updated = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexareUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareUpdate", result -> result
                            .path("firstName").entity(String.class).isEqualTo(dto.firstName())
                            .path("lastName").entity(String.class).isEqualTo(dto.lastName())
                            .path("nickName").entity(String.class).isEqualTo(dto.nickName())
                    )
                    .entity(SpexareDto.class)
                    .get();

            final SpexareDto after = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare")
                    .entity(SpexareDto.class)
                    .get();

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_BAD_REQUEST_when_invalid_input() {
            final SpexareUpdateDto randDto = random.nextObject(SpexareUpdateDto.class);
            final var dto = randDto.toBuilder()
                    .firstName("")
                    .build();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexareUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> Objects.requireNonNull(error.getMessage()).contains("firstName"))
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.BAD_REQUEST.toString()))
                    )
                    .path("spexareUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            final SpexareUpdateDto dto = random.nextObject(SpexareUpdateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexareUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spexareUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_permission() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));

            final SpexareDto before = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare")
                    .entity(SpexareDto.class)
                    .get();

            final SpexareUpdateDto dto = SpexareUpdateDto.builder()
                    .id(before.getId())
                    .firstName(before.getFirstName() + "_")
                    .lastName(before.getLastName())
                    .nickName(before.getNickName())
                    .deceased(Boolean.FALSE)
                    .published(Boolean.TRUE)
                    .build();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexareUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexareUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_role() {
            final SpexareUpdateDto dto = random.nextObject(SpexareUpdateDto.class);

            httpGraphQlTester
                    .mutate()
                    .build()
                    .documentName("spexare/spexareUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexareUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        void should_delete() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantDeletePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexareDelete")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexareDelete")
                    .variable("id", 123)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spexareDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_permission() {
            final var spexare = persistSpexare(randomizeSpexare());

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexareDelete")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexareDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORIDDEN_when_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .build()
                    .documentName("spexare/spexareDelete")
                    .variable("id", 123)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexareDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Image")
    class ImageTests {

        @Test
        void should_delete_image() throws Exception {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var image = Files.readAllBytes(Paths.get(ResourceUtils.getFile("classpath:test.png").getPath()));

            restTestClient
                    .put()
                    .uri("/{spexareId}/image", spexare.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                    .apiVersion("1.0")
                    .body(image)
                    .exchange()
                    .expectStatus().isNoContent();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare", result -> result
                            .path("imageUrl").hasValue()
                    );

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexareImageDelete")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareImageDelete")
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
                    .path("spexare", result -> result
                            .path("imageUrl").valueIsNull()
                    );

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_deleting_not_permitted_due_to_insufficient_permission() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexareImageDelete")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexareImageDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_deleting_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .build()
                    .documentName("spexare/spexareImageDelete")
                    .variable("id", 123L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("spexareImageDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Partner")
    class PartnerTests {

        @Test
        void should_return() {
            final var partner = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, partner.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, partner.getId()));
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            spexare.setPartner(partner);
            partner.setPartner(spexare);
            repository.save(spexare);
            repository.save(partner);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare.partner", result -> result
                            .path("id").entity(Long.class).isEqualTo(partner.getId())
                            .path("firstName").entity(String.class).isEqualTo(partner.getFirstName())
                            .path("lastName").entity(String.class).isEqualTo(partner.getLastName())
                            .path("nickName").entity(String.class).isEqualTo(partner.getNickName())
                    );
        }

        @Test
        void should_update() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var partner = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, partner.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, partner.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare.partner")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexarePartnerAdd")
                    .variable("spexareId", spexare.getId())
                    .variable("id", partner.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexarePartnerAdd")
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
                    .path("spexare.partner", result -> result
                            .path("id").entity(Long.class).isEqualTo(partner.getId())
                            .path("firstName").entity(String.class).isEqualTo(partner.getFirstName())
                            .path("lastName").entity(String.class).isEqualTo(partner.getLastName())
                            .path("nickName").entity(String.class).isEqualTo(partner.getNickName())
                    );
        }

        @Test
        void should_return_NOT_FOUND_when_updating_and_spexare_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("spexare/spexarePartnerAdd")
                    .variable("spexareId", 1L)
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spexarePartnerAdd")
                    .valueIsNull();
        }

        @Test
        void should_return_NOT_FOUND_when_updating_and_partner_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexarePartnerAdd")
                    .variable("spexareId", spexare.getId())
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spexarePartnerAdd")
                    .valueIsNull();
        }

        @Test
        void should_delete() {
            final var partner = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, partner.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, partner.getId()));
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            spexare.setPartner(partner);
            partner.setPartner(spexare);
            repository.save(spexare);
            repository.save(partner);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare.partner", result -> result
                            .path("id").entity(Long.class).isEqualTo(partner.getId())
                            .path("firstName").entity(String.class).isEqualTo(partner.getFirstName())
                            .path("lastName").entity(String.class).isEqualTo(partner.getLastName())
                            .path("nickName").entity(String.class).isEqualTo(partner.getNickName())
                    );

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexarePartnerRemove")
                    .variable("spexareId", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexarePartnerRemove")
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
                    .path("spexare.partner")
                    .valueIsNull();
        }

        @Test
        void should_return_when_removing_and_no_partner() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare.partner")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexarePartnerRemove")
                    .variable("spexareId", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexarePartnerRemove")
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
                    .path("spexare.partner")
                    .valueIsNull();
        }

        @Test
        void should_return_NOT_FOUND_when_removing_and_spexare_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexarePartnerRemove")
                    .variable("spexareId", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("spexarePartnerRemove")
                    .valueIsNull();
        }

    }

    @Nested
    @DisplayName("Events")
    class EventTests {

        @Test
        void should_return_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));

            final List<EventDto> result = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexareEvents")
                    .variable("sourceId", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareEvents")
                    .entityList(EventDto.class)
                    .hasSize(1)
                    .get();

            assertThat(eventRepository.count()).isEqualTo(1);
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getEventType()).isEqualTo(Event.EventType.CREATE.name());
            assertThat(result.getFirst().getSourceType()).isEqualTo(Event.SourceType.SPEXARE.name());
            assertThat(result.getFirst().getCreatedBy()).isEqualTo(spexare.getCreatedBy());
        }
    }

    private Spexare randomizeSpexare() {
        return randomizeSpexare(true);
    }

    private Spexare randomizeSpexare(final boolean published) {
        final var spexare = random.nextObject(Spexare.class);

        spexare.setPublished(published);

        return spexare;
    }

    private Spexare persistSpexare(final Spexare spexare) {
        spexare.setId(null);

        return repository.save(spexare);
    }

    private void syncIndex() {
        final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.execute(status -> {
            final SearchSession searchSession = Search.session(entityManager);
            try {
                searchSession.massIndexer(Spexare.class).startAndWait();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            return null;
        });
    }
}
