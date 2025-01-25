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

package nu.fgv.register.server.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventRepository;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.user.authority.Authority;
import nu.fgv.register.server.user.authority.AuthorityDto;
import nu.fgv.register.server.user.authority.AuthorityRepository;
import nu.fgv.register.server.user.state.State;
import nu.fgv.register.server.user.state.StateRepository;
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import nu.fgv.register.server.util.randomizer.LabelsRandomizer;
import nu.fgv.register.server.util.randomizer.SocialSecurityNumberRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.EmailRandomizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.inClass;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;
import static org.passay.AllowedCharacterRule.ERROR_CODE;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class UserGraphqlApiIntegrationTest extends AbstractGraphqlIntegrationTest {

    private static final int PRE_CREATED_USERS_IN_KEYCLOAK = 3;

    private final EasyRandom random;
    private final UserRepository repository;
    private final AuthorityRepository authorityRepository;
    private final StateRepository stateRepository;
    private final SpexareRepository spexareRepository;
    private final EventRepository eventRepository;

    private final EmailRandomizer emailRandomizer = new EmailRandomizer();
    private final Random rnd = new SecureRandom();

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public UserGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
                                         final AclCache aclCache,
                                         final Keycloak keycloakAdminClient,
                                         final String keycloakClientId,
                                         final PermissionService permissionService,
                                         final ObjectMapper objectMapper,
                                         final UserRepository repository,
                                         final AuthorityRepository authorityRepository,
                                         final StateRepository stateRepository,
                                         final SpexareRepository spexareRepository,
                                         final EventRepository eventRepository) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.repository = repository;
        this.authorityRepository = authorityRepository;
        this.stateRepository = stateRepository;
        this.spexareRepository = spexareRepository;
        this.eventRepository = eventRepository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

        parameters
                .randomize(
                        named("labels"), new LabelsRandomizer()
                )
                .randomize(
                        named("email"), new EmailRandomizer()
                )
                .randomize(
                        named("socialSecurityNumber"), new SocialSecurityNumberRandomizer()
                )
                .excludeField(named("spexare").and(ofType(Spexare.class)).and(inClass(User.class)))
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

        JdbcTestUtils.deleteFromTables(jdbcClient, "user", "state", "spexare", "event");
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
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userPaged")
                    .execute()
                    .errors()
                    .verify()
                    .path("userPaged.edges")
                    .entityList(UserDto.class)
                    .hasSize(0);
        }

        @Test
        void should_return_one() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userPaged")
                    .execute()
                    .errors()
                    .verify()
                    .path("userPaged.edges")
                    .entityList(UserDto.class)
                    .hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            final var state = persistState(randomizeState());
            IntStream.range(0, size).forEach(i -> {
                final var user = persistUser(randomizeUser(state));
                grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));
            });

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userPaged")
                    .variable("first", size)
                    .execute()
                    .errors()
                    .verify()
                    .path("userPaged.edges")
                    .entityList(UserDto.class)
                    .hasSize(size);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("user/userPaged")
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("userPaged")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Retrieve paged with filtering")
    class RetrievePagedWithFilteringTests {

        @Test
        void should_return_zero() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userPaged")
                    .variable("filter", User_.EXTERNAL_ID + ":whatever")
                    .execute()
                    .errors()
                    .verify()
                    .path("userPaged.edges")
                    .entityList(UserDto.class)
                    .hasSize(0);
        }

        @Test
        void should_return_one() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userPaged")
                    .variable("filter", User_.ID + ":" + user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("userPaged.edges")
                    .entityList(UserDto.class)
                    .hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            final var state = persistState(randomizeState());
            final List<String> externalIds = new ArrayList<>();
            IntStream.range(0, size).forEach(i -> {
                final var user = persistUser(randomizeUser(state));
                externalIds.add(user.getExternalId());
                grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));
            });

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userPaged")
                    .variable("filter", "( " + externalIds.stream().map(e -> "%s:%s".formatted(User_.EXTERNAL_ID, e)).collect(Collectors.joining(" OR ")) + " )")
                    .variable("first", size)
                    .execute()
                    .errors()
                    .verify()
                    .path("userPaged.edges")
                    .entityList(UserDto.class)
                    .hasSize(size);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("user/userPaged")
                    .variable("filter", User_.EXTERNAL_ID + ":whatever")
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("userPaged")
                    .valueIsNull();
        }
    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create() {
            final UserCreateDto dto = random.nextObject(UserCreateDto.class);
            final var state = persistState(randomizeState());
            state.setInitial(true);
            stateRepository.save(state);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userCreate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .verify()
                    .path("userCreate", result -> result
                            .path("email").entity(String.class).isEqualTo(dto.getEmail())
                    );

            assertThat(repository.count()).isEqualTo(1);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted() {
            final UserCreateDto dto = random.nextObject(UserCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("user/userCreate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("userCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        void should_return_found() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("user", result -> result
                            .path("id").entity(Long.class).isEqualTo(user.getId())
                    );
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("user")
                    .valueIsNull();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("user")
                    .valueIsNull();
        }
    }

    @Nested
    @DisplayName("Update")
    class UpdateTests {

        @Test
        void should_update() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final UserDto before = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("user")
                    .entity(UserDto.class)
                    .get();

            final UserUpdateDto dto = UserUpdateDto.builder()
                    .id(before.getId())
                    .email("a" + before.getEmail())
                    .build();

            final UserDto updated = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .verify()
                    .path("userUpdate", result -> result
                            .path("email").entity(String.class).isEqualTo(dto.getEmail())
                    )
                    .entity(UserDto.class)
                    .get();

            final UserDto after = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("user")
                    .entity(UserDto.class)
                    .get();

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            final UserUpdateDto dto = random.nextObject(UserUpdateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("userUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
            assertThat(getUsersCountInKeycloak()).isEqualTo(PRE_CREATED_USERS_IN_KEYCLOAK);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_permission() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final UserDto before = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("user")
                    .entity(UserDto.class)
                    .get();

            final UserUpdateDto dto = UserUpdateDto.builder()
                    .id(before.getId())
                    .email("a" + before.getEmail())
                    .build();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("userUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_role() {
            final UserUpdateDto dto = random.nextObject(UserUpdateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("user/userUpdate")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("userUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        void should_delete() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));
            grantDeletePermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userDelete")
                    .variable("id", user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("userDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
            assertThat(getUsersCountInKeycloak()).isEqualTo(PRE_CREATED_USERS_IN_KEYCLOAK);
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userDelete")
                    .variable("id", 123L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("userDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
            assertThat(getUsersCountInKeycloak()).isEqualTo(PRE_CREATED_USERS_IN_KEYCLOAK);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_permission() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userDelete")
                    .variable("id", user.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("userDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("user/userDelete")
                    .variable("id", 123L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("userDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Authorities")
    class AuthorityTests {

        @Test
        void should_return_zero() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("user.authorities")
                    .entityList(AuthorityDto.class)
                    .hasSize(0);

            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
            assertThat(getRoleRepresentationsForUserInKeycloak(user)).isEmpty();
        }

        @Test
        void should_return_one() {
            final var state = persistState(randomizeState());
            final var authority = getRandomAuthority();
            final var user = persistUser(randomizeUser(state), authority);
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("user.authorities")
                    .entityList(AuthorityDto.class)
                    .hasSize(1);

            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
            final List<RoleRepresentation> assignedRoles = getRoleRepresentationsForUserInKeycloak(user);
            assertThat(assignedRoles).hasSize(1);
            assertThat(assignedRoles.getFirst().getName()).isEqualTo(authority);
        }

        @Test
        void should_return_many() {
            final var state = persistState(randomizeState());
            final var authorities = getRandomAuthorities(2);
            final var user = persistUser(randomizeUser(state), authorities.getFirst(), authorities.get(1));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("user.authorities")
                    .entityList(AuthorityDto.class)
                    .hasSize(2);

            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
            final List<RoleRepresentation> assignedRoles = getRoleRepresentationsForUserInKeycloak(user);
            assertThat(assignedRoles).hasSize(2);
            assertThat(assignedRoles.stream().anyMatch(r -> authorities.getFirst().equals(r.getName()))).isTrue();
            assertThat(assignedRoles.stream().anyMatch(r -> authorities.get(1).equals(r.getName()))).isTrue();
        }

        @Test
        void should_add() {
            final var state = persistState(randomizeState());
            final var authorities = getRandomAuthorities(1);
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("user.authorities")
                    .entityList(AuthorityDto.class)
                    .hasSize(0);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userAuthorityAdd")
                    .variable("userId", user.getId())
                    .variable("id", authorities.getFirst())
                    .execute()
                    .errors()
                    .verify()
                    .path("userAuthorityAdd")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("user.authorities")
                    .entityList(AuthorityDto.class)
                    .hasSize(1);

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
            final List<RoleRepresentation> assignedRoles = getRoleRepresentationsForUserInKeycloak(user);
            assertThat(assignedRoles).hasSize(1);
            assertThat(assignedRoles.stream().anyMatch(r -> authorities.getFirst().equals(r.getName()))).isTrue();
        }

        @Test
        void should_return_NOT_FOUND_when_adding_and_user_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userAuthorityAdd")
                    .variable("userId", 1L)
                    .variable("id", getRandomAuthority())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("userAuthorityAdd")
                    .valueIsNull();

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(PRE_CREATED_USERS_IN_KEYCLOAK);
        }

        @Test
        void should_return_NOT_FOUND_when_adding_and_authority_not_found() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userAuthorityAdd")
                    .variable("userId", user.getId())
                    .variable("id", "whatever")
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("userAuthorityAdd")
                    .valueIsNull();

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
            assertThat(getRoleRepresentationsForUserInKeycloak(user)).isEmpty();
        }

        @Test
        void should_add_multiple() {
            final var state = persistState(randomizeState());
            final var authorities = getRandomAuthorities(2);
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("user.authorities")
                    .entityList(AuthorityDto.class)
                    .hasSize(0);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userAuthoritiesAdd")
                    .variable("userId", user.getId())
                    .variable("ids", authorities)
                    .execute()
                    .errors()
                    .verify()
                    .path("userAuthoritiesAdd")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("user.authorities")
                    .entityList(AuthorityDto.class)
                    .hasSize(2);

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
            final List<RoleRepresentation> assignedRoles = getRoleRepresentationsForUserInKeycloak(user);
            assertThat(assignedRoles).hasSize(2);
            assertThat(assignedRoles.stream().anyMatch(r -> authorities.getFirst().equals(r.getName()))).isTrue();
            assertThat(assignedRoles.stream().anyMatch(r -> authorities.get(1).equals(r.getName()))).isTrue();
        }

        @Test
        void should_return_NOT_FOUND_when_adding_multiple_and_user_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userAuthoritiesAdd")
                    .variable("userId", 1L)
                    .variable("ids", List.of(getRandomAuthority(), getRandomAuthority()))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("userAuthoritiesAdd")
                    .valueIsNull();

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(PRE_CREATED_USERS_IN_KEYCLOAK);
        }

        @Test
        void should_return_NOT_FOUND_when_adding_multiple_and_authorities_not_found() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userAuthoritiesAdd")
                    .variable("userId", 1L)
                    .variable("ids", List.of("whatever1", "whatever2"))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("userAuthoritiesAdd")
                    .valueIsNull();

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
            assertThat(getRoleRepresentationsForUserInKeycloak(user)).isEmpty();
        }

        @Test
        void should_return_NOT_FOUND_when_adding_multiple_and_authority_not_found() {
            final var state = persistState(randomizeState());
            final var authority = getRandomAuthority();
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userAuthoritiesAdd")
                    .variable("userId", 1L)
                    .variable("ids", List.of(authority, "whatever2"))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("userAuthoritiesAdd")
                    .valueIsNull();

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
            assertThat(getRoleRepresentationsForUserInKeycloak(user)).isEmpty();
        }

        @Test
        void should_remove() {
            final var state = persistState(randomizeState());
            final var authority = getRandomAuthority();
            final var user = persistUser(randomizeUser(state), authority);
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("user.authorities")
                    .entityList(AuthorityDto.class)
                    .hasSize(1);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userAuthorityRemove")
                    .variable("userId", user.getId())
                    .variable("id", authority)
                    .execute()
                    .errors()
                    .verify()
                    .path("userAuthorityRemove")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("user.authorities")
                    .entityList(AuthorityDto.class)
                    .hasSize(0);

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
            assertThat(getRoleRepresentationsForUserInKeycloak(user)).isEmpty();
        }

        @Test
        void should_return_NOT_FOUND_when_removing_and_user_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userAuthorityRemove")
                    .variable("userId", 1L)
                    .variable("id", getRandomAuthority())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("userAuthorityRemove")
                    .valueIsNull();

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(PRE_CREATED_USERS_IN_KEYCLOAK);
        }

        @Test
        void should_return_NOT_FOUND_when_removing_and_authority_not_found() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userAuthorityRemove")
                    .variable("userId", user.getId())
                    .variable("id", "whatever")
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("userAuthorityRemove")
                    .valueIsNull();

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
            assertThat(getRoleRepresentationsForUserInKeycloak(user)).isEmpty();
        }

        @Test
        void should_remove_multiple() {
            final var state = persistState(randomizeState());
            final var authorities = getRandomAuthorities(2);
            final var user = persistUser(randomizeUser(state), authorities.getFirst(), authorities.get(1));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("user.authorities")
                    .entityList(AuthorityDto.class)
                    .hasSize(2);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userAuthoritiesRemove")
                    .variable("userId", user.getId())
                    .variable("ids", List.of(authorities.getFirst(), authorities.get(1)))
                    .execute()
                    .errors()
                    .verify()
                    .path("userAuthoritiesRemove")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("user.authorities")
                    .entityList(AuthorityDto.class)
                    .hasSize(0);

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
            assertThat(getRoleRepresentationsForUserInKeycloak(user)).isEmpty();
        }

        @Test
        void should_return_NOT_FOUND_when_removing_multiple_and_user_not_found() {
            final var authorities = getRandomAuthorities(2);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userAuthoritiesRemove")
                    .variable("userId", 1L)
                    .variable("ids", List.of(authorities.getFirst(), authorities.get(1)))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("userAuthoritiesRemove")
                    .valueIsNull();

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(PRE_CREATED_USERS_IN_KEYCLOAK);
        }

        @Test
        void should_return_NOT_FOUND_when_removing_multiple_and_authorities_not_found() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userAuthoritiesRemove")
                    .variable("userId", user.getId())
                    .variable("ids", List.of("whatever1", "whatever2"))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("userAuthoritiesRemove")
                    .valueIsNull();

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
            assertThat(getRoleRepresentationsForUserInKeycloak(user)).isEmpty();
        }

        @Test
        void should_return_NOT_FOUND_when_removing_multiple_and_authority_not_found() {
            final var state = persistState(randomizeState());
            final var authority = getRandomAuthority();
            final var user = persistUser(randomizeUser(state), authority);
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userAuthoritiesRemove")
                    .variable("userId", user.getId())
                    .variable("ids", List.of(authority, "whatever2"))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("userAuthoritiesRemove")
                    .valueIsNull();

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
            final List<RoleRepresentation> assignedRoles = getRoleRepresentationsForUserInKeycloak(user);
            assertThat(assignedRoles).hasSize(1);
            assertThat(assignedRoles.stream().anyMatch(r -> authority.equals(r.getName()))).isTrue();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("user")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_adding_not_permitted_due_to_insufficient_permission() {
            final var state = persistState(randomizeState());
            final var authorities = getRandomAuthorities(2);
            final var user = persistUser(randomizeUser(state), authorities.getFirst());
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userAuthorityAdd")
                    .variable("userId", user.getId())
                    .variable("id", getRandomAuthority())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("userAuthorityAdd")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_adding_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("user/userAuthorityAdd")
                    .variable("userId", 1L)
                    .variable("id", getRandomAuthority())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("userAuthorityAdd")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_adding_multiple_not_permitted_due_to_insufficient_permission() {
            final var state = persistState(randomizeState());
            final var authorities = getRandomAuthorities(2);
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userAuthoritiesAdd")
                    .variable("userId", user.getId())
                    .variable("ids", authorities)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("userAuthoritiesAdd")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_adding_multiple_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("user/userAuthoritiesAdd")
                    .variable("userId", 1L)
                    .variable("ids", List.of(getRandomAuthority(), getRandomAuthority()))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("userAuthoritiesAdd")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_deleting_not_permitted_due_to_insufficient_permission() {
            final var state = persistState(randomizeState());
            final var authority = getRandomAuthority();
            final var user = persistUser(randomizeUser(state), authority);
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userAuthorityRemove")
                    .variable("userId", user.getId())
                    .variable("id", getRandomAuthority())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("userAuthorityRemove")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_deleting_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("user/userAuthorityRemove")
                    .variable("userId", 1L)
                    .variable("id", getRandomAuthority())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("userAuthorityRemove")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_deleting_multiple_not_permitted_due_to_insufficient_permission() {
            final var state = persistState(randomizeState());
            final var authorities = getRandomAuthorities(2);
            final var user = persistUser(randomizeUser(state), authorities.getFirst(), authorities.get(1));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userAuthoritiesRemove")
                    .variable("userId", user.getId())
                    .variable("ids", authorities)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("userAuthoritiesRemove")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_deleting_multiple_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("user/userAuthoritiesRemove")
                    .variable("userId", 1L)
                    .variable("ids", List.of(getRandomAuthority(), getRandomAuthority()))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("userAuthoritiesRemove")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("State")
    class StateTests {

        @Test
        void should_return() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("user.state", result -> result
                            .path("id").entity(String.class).isEqualTo(user.getState().getId())
                    );
        }

        @Test
        void should_set() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            final var newState = persistState(randomizeState());
            newState.setEnabled(!state.getEnabled());
            stateRepository.save(newState);
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("user.state", result -> result
                            .path("id").entity(String.class).isEqualTo(state.getId())
                    );

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userStateSet")
                    .variable("userId", user.getId())
                    .variable("id", newState.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("userStateSet")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("user.state", result -> result
                            .path("id").entity(String.class).isEqualTo(newState.getId())
                    );

            assertThat(repository.findById(user.getId()).map(User::getState).orElseThrow(() -> new RuntimeException("User not found"))).isEqualTo(newState);
            assertThat(getUserRepresentationForUserInKeycloak(user).isEnabled()).isEqualTo(newState.getEnabled());
        }

        @Test
        void should_return_NOT_FOUND_when_setting_and_user_not_found() {
            final var state = persistState(randomizeState());

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userStateSet")
                    .variable("userId", 1L)
                    .variable("id", state.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("userStateSet")
                    .valueIsNull();
        }

        @Test
        void should_return_NOT_FOUND_when_setting_and_state_not_found() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userStateSet")
                    .variable("userId", 1L)
                    .variable("id", "whatever")
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("userStateSet")
                    .valueIsNull();
        }

        @Test
        void should_return_FORBIDDEN_when_setting_not_permitted_due_to_insufficient_permission() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            final var newState = persistState(randomizeState());
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userStateSet")
                    .variable("userId", user.getId())
                    .variable("id", newState.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("userStateSet")
                    .valueIsNull();
        }

        @Test
        void should_return_FORBIDDEN_when_setting_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("user/userStateSet")
                    .variable("userId", 1L)
                    .variable("id", "whatever")
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("userStateSet")
                    .valueIsNull();
        }
    }

    @Nested
    @DisplayName("Spexare")
    class SpexareTests {

        @Test
        void should_return() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            final var spexare = persistSpexare(randomizeSpexare());
            user.setSpexare(spexare);
            repository.save(user);
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("user.spexare", result -> result
                            .path("id").entity(Long.class).isEqualTo(spexare.getId())
                    );
        }

        @Test
        void should_add() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            final var spexare = persistSpexare(randomizeSpexare());
            grantAdministrationPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantAdministrationPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("user.spexare")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userSpexareAdd")
                    .variable("userId", user.getId())
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("userSpexareAdd")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("user.spexare", result -> result
                            .path("id").entity(Long.class).isEqualTo(spexare.getId())
                    );

            assertThat(repository.findById(user.getId()).map(User::getSpexare).orElseThrow(() -> new RuntimeException("User not found"))).isEqualTo(spexare);
        }

        @Test
        void should_return_NOT_FOUND_when_adding_and_user_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userSpexareAdd")
                    .variable("userId", 1L)
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("userSpexareAdd")
                    .valueIsNull();
        }

        @Test
        void should_return_NOT_FOUND_when_adding_and_spexare_not_found() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userSpexareAdd")
                    .variable("userId", user.getId())
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("userSpexareAdd")
                    .valueIsNull();
        }

        @Test
        void should_remove() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            final var spexare = persistSpexare(randomizeSpexare());
            user.setSpexare(spexare);
            repository.save(user);
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("user.spexare", result -> result
                            .path("id").entity(Long.class).isEqualTo(spexare.getId())
                    );

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userSpexareRemove")
                    .variable("userId", user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("userSpexareRemove")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/user")
                    .variable("id", user.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("user.spexare")
                    .valueIsNull();

            assertThat(repository.findById(user.getId()).map(User::getSpexare)).isEmpty();
        }

        @Test
        void should_return_NOT_FOUND_when_removing_and_user_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userSpexareRemove")
                    .variable("userId", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("userSpexareRemove")
                    .valueIsNull();
        }

        @Test
        void should_return_FORBIDDEN_when_adding_not_permitted_due_to_insufficient_permission() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userSpexareAdd")
                    .variable("userId", user.getId())
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("userSpexareAdd")
                    .valueIsNull();
        }

        @Test
        void should_return_FORBIDDEN_when_adding_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("user/userSpexareAdd")
                    .variable("userId", 1L)
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("userSpexareAdd")
                    .valueIsNull();
        }

        @Test
        void should_return_FORBIDDEN_when_removing_not_permitted_due_to_insufficient_permission() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userSpexareRemove")
                    .variable("userId", user.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("userSpexareRemove")
                    .valueIsNull();
        }

        @Test
        void should_return_FORBIDDEN_when_removing_not_permitted_due_to_insufficient_role() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("user/userSpexareRemove")
                    .variable("userId", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("userSpexareRemove")
                    .valueIsNull();
        }
    }

    @Nested
    @DisplayName("Events")
    class EventTests {

        @Test
        void should_return_found() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));

            final List<EventDto> result = httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("user/userEvents")
                    .execute()
                    .errors()
                    .verify()
                    .path("userEvents")
                    .entityList(EventDto.class)
                    .hasSize(1)
                    .get();

            assertThat(eventRepository.count()).isEqualTo(2L);
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getEvent()).isEqualTo(Event.EventType.CREATE.name());
            assertThat(result.getFirst().getSource()).isEqualTo(Event.SourceType.USER.name());
            assertThat(result.getFirst().getCreatedBy()).isEqualTo(user.getCreatedBy());
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("user/userEvents")
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("userEvents")
                    .valueIsNull();
        }
    }

    private User randomizeUser(final State state) {
        final var user = random.nextObject(User.class);

        user.setState(state);

        return user;
    }

    private User persistUser(final User user, final String... roles) {
        user.setId(null);
        final var representation = persistUserInKeycloak(roles);
        user.setExternalId(representation.getId());
        return repository.save(user);
    }

    private UserRepresentation persistUserInKeycloak(final String... roles) {
        final UserRepresentation userRepresentation = new UserRepresentation();

        userRepresentation.setEmail(emailRandomizer.getRandomValue());
        userRepresentation.setEnabled(false);

        final CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setValue(generateTemporaryPassword());
        credentialRepresentation.setTemporary(true);

        try (final Response response = keycloakAdminClient
                .realm(keycloakRealm)
                .users()
                .create(userRepresentation)
        ) {
            if (response.getStatus() == HttpStatus.CREATED.value()) {
                final String locationPath = response.getLocation().getPath();
                final String id = locationPath.substring(locationPath.lastIndexOf('/') + 1);
                final UserResource userResource = keycloakAdminClient
                        .realm(keycloakRealm)
                        .users()
                        .get(id);

                if (roles.length != 0) {
                    final List<RoleRepresentation> roleRepresentations = getRoleRepresentationsInKeycloak().stream()
                            .filter(r -> List.of(roles).contains(r.getName()))
                            .toList();

                    userResource
                            .roles()
                            .clientLevel(keycloakClientId)
                            .add(roleRepresentations);
                }

                return userResource.toRepresentation();
            } else {
                throw new RuntimeException("Could not persist user in Keycloak");
            }
        }
    }

    private List<RoleRepresentation> getRoleRepresentationsInKeycloak() {
        return keycloakAdminClient
                .realm(keycloakRealm)
                .clients()
                .get(keycloakClientId)
                .roles()
                .list();
    }

    private List<RoleRepresentation> getRoleRepresentationsForUserInKeycloak(final User user) {
        return keycloakAdminClient
                .realm(keycloakRealm)
                .users()
                .get(user.getExternalId())
                .roles()
                .clientLevel(keycloakClientId)
                .listAll();
    }

    private UserRepresentation getUserRepresentationForUserInKeycloak(final User user) {
        return keycloakAdminClient
                .realm(keycloakRealm)
                .users()
                .get(user.getExternalId())
                .toRepresentation();
    }

    private Integer getUsersCountInKeycloak() {
        return keycloakAdminClient
                .realm(keycloakRealm)
                .users()
                .count();
    }

    private String generateTemporaryPassword() {
        final PasswordGenerator passwordGenerator = new PasswordGenerator();

        final CharacterRule lowerCaseRule = new CharacterRule(EnglishCharacterData.LowerCase);
        lowerCaseRule.setNumberOfCharacters(2);

        final CharacterRule upperCaseRule = new CharacterRule(EnglishCharacterData.UpperCase);
        upperCaseRule.setNumberOfCharacters(2);

        final CharacterRule digitRule = new CharacterRule(EnglishCharacterData.Digit);
        digitRule.setNumberOfCharacters(2);

        final CharacterRule specialCharacterRule = new CharacterRule(new CharacterData() {
            public String getErrorCode() {
                return ERROR_CODE;
            }

            public String getCharacters() {
                return "!@#$%^&*()_+";
            }
        });
        specialCharacterRule.setNumberOfCharacters(2);

        return passwordGenerator.generatePassword(15, List.of(specialCharacterRule, lowerCaseRule, upperCaseRule, digitRule));
    }

    private String getRandomAuthority() {
        return AUTHORITIES.get(rnd.nextInt(AUTHORITIES.size()));
    }

    private List<String> getRandomAuthorities(final int numberOfAuthorities) {
        if (numberOfAuthorities > AUTHORITIES.size()) {
            throw new IllegalArgumentException();
        }
        final List<String> randomAuthorities = new ArrayList<>(AUTHORITIES);
        Collections.shuffle(randomAuthorities);
        return randomAuthorities.subList(0, numberOfAuthorities);
    }

    private Authority randomizeAuthority() {
        return random.nextObject(Authority.class);
    }

    private Authority persistAuthority(final Authority authority) {
        return authorityRepository.save(authority);
    }

    private State randomizeState() {
        return random.nextObject(State.class);
    }

    private State persistState(final State state) {
        return stateRepository.save(state);
    }

    private Spexare randomizeSpexare() {
        return random.nextObject(Spexare.class);
    }

    private Spexare persistSpexare(final Spexare spexare) {
        spexare.setId(null);

        return spexareRepository.save(spexare);
    }
}
