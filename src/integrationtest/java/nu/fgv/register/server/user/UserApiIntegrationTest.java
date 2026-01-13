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

import jakarta.ws.rs.core.Response;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventRepository;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareDto;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.user.authority.Authority;
import nu.fgv.register.server.user.authority.AuthorityDto;
import nu.fgv.register.server.user.authority.AuthorityRepository;
import nu.fgv.register.server.user.state.State;
import nu.fgv.register.server.user.state.StateDto;
import nu.fgv.register.server.user.state.StateRepository;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.HalEmbeddedResponse;
import nu.fgv.register.server.util.randomizer.SocialSecurityNumberRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.EmailRandomizer;
import org.jspecify.annotations.NonNull;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.ApiVersionInserter;
import tools.jackson.databind.ObjectMapper;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
class UserApiIntegrationTest extends AbstractIntegrationTest {

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
    public UserApiIntegrationTest(final JdbcClient jdbcClient,
                                  final AclCache aclCache,
                                  final Keycloak keycloakAdminClient,
                                  final String keycloakClientId,
                                  final PermissionService permissionService,
                                  final UserRepository repository,
                                  final AuthorityRepository authorityRepository,
                                  final StateRepository stateRepository,
                                  final SpexareRepository spexareRepository,
                                  final EventRepository eventRepository,
                                  final ObjectMapper objectMapper) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.repository = repository;
        this.authorityRepository = authorityRepository;
        this.stateRepository = stateRepository;
        this.spexareRepository = spexareRepository;
        this.eventRepository = eventRepository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

        parameters
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
        restTestClient = RestTestClient
                .bindToServer()
                .baseUrl("http://localhost:%s/api/users".formatted(localPort))
                .apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
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
            final List<UserDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<UserDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("users");

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_one() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final List<UserDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<UserDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("users");

            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            final int size = 42;
            final var state = persistState(randomizeState());
            IntStream.range(0, size).forEach(i -> {
                final var user = persistUser(randomizeUser(state));
                grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));
            });

            final List<UserDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("size", size)
                                            .build()
                                    )
                                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<UserDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("users");

            assertThat(result).hasSize(size);
        }

        @Test
        void should_return_403_when_not_permitted() {
            final ProblemDetail result = restTestClient
                    .get()
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
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

            final List<UserDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("filter", User_.EXTERNAL_ID + ":whatever")
                                            .build()
                                    )
                                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<UserDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("users");

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_one() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final List<UserDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("filter", User_.ID + ":" + user.getId())
                                            .build()
                                    )
                                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<UserDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("users");

            assertThat(result).hasSize(1);
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

            final List<UserDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParam("filter", "( " + externalIds.stream().map(e -> "%s:%s".formatted(User_.EXTERNAL_ID, e)).collect(Collectors.joining(" OR ")) + " )")
                                            .queryParam("size", size)
                                            .build()
                                    )
                                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<UserDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("users");

            assertThat(result).hasSize(size);
        }

        @Test
        void should_return_403_when_not_permitted() {
            final ProblemDetail result = restTestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("filter", User_.EXTERNAL_ID + ":whatever")
                            .build()
                    )
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }
    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create_and_return_201() {
            final UserCreateDto dto = random.nextObject(UserCreateDto.class);
            final var state = persistState(randomizeState());
            state.setInitial(true);
            stateRepository.save(state);

            final UserDto result = restTestClient
                    .post()
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(UserDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result)
                    .extracting("email")
                    .isEqualTo(dto.email());
            assertThat(repository.count()).isEqualTo(1);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
        }

        @Test
        void should_return_400_when_invalid_input() {
            final UserCreateDto randDto = random.nextObject(UserCreateDto.class);
            final var dto = randDto.toBuilder()
                    .email(null)
                    .build();

            restTestClient
                    .post()
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isBadRequest();

            assertThat(repository.count()).isZero();
            assertThat(getUsersCountInKeycloak()).isEqualTo(PRE_CREATED_USERS_IN_KEYCLOAK);
        }

        @Test
        void should_return_403_when_not_permitted() {
            final UserCreateDto dto = random.nextObject(UserCreateDto.class);

            final ProblemDetail result = restTestClient
                    .post()
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

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
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final UserDto result = restTestClient
                    .get()
                    .uri("/{id}", user.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id")
                    .isEqualTo(result.getId());
        }

        @Test
        void should_return_404_when_not_found() {
            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/{id}", 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_403_when_not_permitted() {
            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/{id}", 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }
    }

    @Nested
    @DisplayName("Update")
    class UpdateTests {

        @Test
        void should_update_and_return_200() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final UserDto before = restTestClient
                    .get()
                    .uri("/{id}", user.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserDto.class)
                    .returnResult()
                    .getResponseBody();

            final UserUpdateDto dto = UserUpdateDto.builder()
                    .id(before.getId())
                    .email("a" + before.getEmail())
                    .build();

            final UserDto updated = restTestClient
                    .put()
                    .uri("/{id}", user.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserDto.class)
                    .returnResult()
                    .getResponseBody();

            final UserDto after = restTestClient
                    .get()
                    .uri("/{id}", user.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
        }

        @Test
        void should_return_400_when_invalid_input() {
            final UserUpdateDto randDto = random.nextObject(UserUpdateDto.class);
            final var dto = randDto.toBuilder()
                    .email(null)
                    .build();

            restTestClient
                    .put()
                    .uri("/{id}", dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isBadRequest();

            assertThat(repository.count()).isZero();
            assertThat(getUsersCountInKeycloak()).isEqualTo(PRE_CREATED_USERS_IN_KEYCLOAK);
        }

        @Test
        void should_return_404_when_not_found() {
            final UserUpdateDto dto = random.nextObject(UserUpdateDto.class);

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{id}", dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(getUsersCountInKeycloak()).isEqualTo(PRE_CREATED_USERS_IN_KEYCLOAK);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_permission() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final UserDto before = restTestClient
                    .get()
                    .uri("/{id}", user.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserDto.class)
                    .returnResult()
                    .getResponseBody();

            final UserUpdateDto dto = UserUpdateDto.builder()
                    .id(before.getId())
                    .email("a" + before.getEmail())
                    .build();

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{id}", dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_role() {
            final UserUpdateDto dto = random.nextObject(UserUpdateDto.class);

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{id}", dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }
    }

    @Nested
    @DisplayName("Partial update")
    class PartialUpdateTests {

        @Test
        void should_update_and_return_200() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final UserDto before = restTestClient
                    .get()
                    .uri("/{id}", user.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserDto.class)
                    .returnResult()
                    .getResponseBody();

            final UserUpdateDto dto = UserUpdateDto.builder()
                    .id(before.getId())
                    .email("a" + before.getEmail())
                    .build();

            final UserDto updated = restTestClient
                    .patch()
                    .uri("/{id}", user.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserDto.class)
                    .returnResult()
                    .getResponseBody();

            final UserDto after = restTestClient
                    .get()
                    .uri("/{id}", user.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
        }

        @Test
        void should_return_404_when_not_found() {
            final UserUpdateDto dto = random.nextObject(UserUpdateDto.class);

            final ProblemDetail result = restTestClient
                    .patch()
                    .uri("/{id}", dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(getUsersCountInKeycloak()).isEqualTo(PRE_CREATED_USERS_IN_KEYCLOAK);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_permission() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final UserDto before = restTestClient
                    .get()
                    .uri("/{id}", user.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserDto.class)
                    .returnResult()
                    .getResponseBody();

            final UserUpdateDto dto = UserUpdateDto.builder()
                    .id(before.getId())
                    .email("a" + before.getEmail())
                    .build();

            final ProblemDetail result = restTestClient
                    .patch()
                    .uri("/{id}", dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_role() {
            final UserUpdateDto dto = random.nextObject(UserUpdateDto.class);

            final ProblemDetail result = restTestClient
                    .patch()
                    .uri("/{id}", dto.id())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(dto)
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

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
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));
            grantDeletePermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            restTestClient
                    .delete()
                    .uri("/{id}", user.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(repository.count()).isZero();
            assertThat(getUsersCountInKeycloak()).isEqualTo(PRE_CREATED_USERS_IN_KEYCLOAK);
        }

        @Test
        void should_return_404_when_not_found() {
            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{id}", 123)
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(getUsersCountInKeycloak()).isEqualTo(PRE_CREATED_USERS_IN_KEYCLOAK);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_permission() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{id}", user.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_role() {
            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{id}", 123)
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }
    }

    @Nested
    @DisplayName("Authorities")
    class AuthorityTests {

        @Test
        void should_return_404() {
            restTestClient
                    .get()
                    .uri("/{userId}/authorities", 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound();

            assertThat(getUsersCountInKeycloak()).isEqualTo(PRE_CREATED_USERS_IN_KEYCLOAK);
        }

        @Test
        void should_return_zero() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final List<AuthorityDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri("/{userId}/authorities", user.getId())
                                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<AuthorityDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("authorities");

            assertThat(result).isEmpty();
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
            assertThat(getRoleRepresentationsForUserInKeycloak(user)).isEmpty();
        }

        @Test
        void should_return_one() {
            final var state = persistState(randomizeState());
            final var authority = getRandomAuthority();
            final var user = persistUser(randomizeUser(state), authority);
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final List<AuthorityDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri("/{userId}/authorities", user.getId())
                                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<AuthorityDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("authorities");

            assertThat(result).hasSize(1);
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

            final List<AuthorityDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri("/{userId}/authorities", user.getId())
                                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<AuthorityDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("authorities");

            assertThat(result).hasSize(2);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
            final List<RoleRepresentation> assignedRoles = getRoleRepresentationsForUserInKeycloak(user);
            assertThat(assignedRoles).hasSize(2);
            assertThat(assignedRoles.stream().anyMatch(r -> authorities.getFirst().equals(r.getName()))).isTrue();
            assertThat(assignedRoles.stream().anyMatch(r -> authorities.get(1).equals(r.getName()))).isTrue();
        }

        @Test
        void should_add_and_return_204() {
            final var state = persistState(randomizeState());
            final var authorities = getRandomAuthorities(1);
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            restTestClient
                    .put()
                    .uri("/{userId}/authorities/{id}", user.getId(), authorities.getFirst())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
            final List<RoleRepresentation> assignedRoles = getRoleRepresentationsForUserInKeycloak(user);
            assertThat(assignedRoles).hasSize(1);
            assertThat(assignedRoles.stream().anyMatch(r -> authorities.getFirst().equals(r.getName()))).isTrue();
        }

        @Test
        void should_return_404_when_adding_and_user_not_found() {
            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{userId}/authorities/{id}", 1L, getRandomAuthority())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(PRE_CREATED_USERS_IN_KEYCLOAK);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_adding_and_authority_not_found() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{userId}/authorities/{id}", user.getId(), "whatever")
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
            assertThat(getRoleRepresentationsForUserInKeycloak(user)).isEmpty();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_add_multiple_and_return_204() {
            final var state = persistState(randomizeState());
            final var authorities = getRandomAuthorities(2);
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            restTestClient
                    .put()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{userId}/authorities")
                            .queryParam("ids", String.join(",", authorities.getFirst(), authorities.get(1)))
                            .build(user.getId())
                    )
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
            final List<RoleRepresentation> assignedRoles = getRoleRepresentationsForUserInKeycloak(user);
            assertThat(assignedRoles).hasSize(2);
            assertThat(assignedRoles.stream().anyMatch(r -> authorities.getFirst().equals(r.getName()))).isTrue();
            assertThat(assignedRoles.stream().anyMatch(r -> authorities.get(1).equals(r.getName()))).isTrue();
        }

        @Test
        void should_return_404_when_adding_multiple_and_user_not_found() {
            final ProblemDetail result = restTestClient
                    .put()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{userId}/authorities")
                            .queryParam("ids", String.join(",", getRandomAuthority(), getRandomAuthority()))
                            .build(1L)
                    )
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(PRE_CREATED_USERS_IN_KEYCLOAK);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_adding_multiple_and_authorities_not_found() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final ProblemDetail result = restTestClient
                    .put()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{userId}/authorities")
                            .queryParam("ids", String.join(",", "whatever1", "whatever2"))
                            .build(user.getId())
                    )
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
            assertThat(getRoleRepresentationsForUserInKeycloak(user)).isEmpty();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_adding_multiple_and_authority_not_found() {
            final var state = persistState(randomizeState());
            final var authority = getRandomAuthority();
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final ProblemDetail result = restTestClient
                    .put()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{userId}/authorities")
                            .queryParam("ids", String.join(",", authority, "whatever"))
                            .build(user.getId())
                    )
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
            assertThat(getRoleRepresentationsForUserInKeycloak(user)).isEmpty();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_remove_and_return_204() {
            final var state = persistState(randomizeState());
            final var authority = getRandomAuthority();
            final var user = persistUser(randomizeUser(state), authority);
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            restTestClient
                    .delete()
                    .uri("/{userId}/authorities/{id}", user.getId(), authority)
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
            assertThat(getRoleRepresentationsForUserInKeycloak(user)).isEmpty();
        }

        @Test
        void should_return_404_when_removing_and_user_not_found() {
            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{userId}/authorities/{id}", 1L, getRandomAuthority())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(PRE_CREATED_USERS_IN_KEYCLOAK);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_removing_and_authority_not_found() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{userId}/authorities/{id}", user.getId(), "whatever")
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
            assertThat(getRoleRepresentationsForUserInKeycloak(user)).isEmpty();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_remove_multiple_and_return_204() {
            final var state = persistState(randomizeState());
            final var authorities = getRandomAuthorities(2);
            final var user = persistUser(randomizeUser(state), authorities.getFirst(), authorities.get(1));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            restTestClient
                    .delete()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{userId}/authorities")
                            .queryParam("ids", String.join(",", authorities.getFirst(), authorities.get(1)))
                            .build(user.getId())
                    )
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
            assertThat(getRoleRepresentationsForUserInKeycloak(user)).isEmpty();
        }

        @Test
        void should_return_404_when_removing_multiple_and_user_not_found() {
            final var authorities = getRandomAuthorities(2);

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{userId}/authorities")
                            .queryParam("ids", String.join(",", authorities.getFirst(), authorities.get(1)))
                            .build(1L)
                    )
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(PRE_CREATED_USERS_IN_KEYCLOAK);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_removing_multiple_and_authorities_not_found() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{userId}/authorities")
                            .queryParam("ids", String.join(",", "whatever1", "whatever2"))
                            .build(user.getId())
                    )
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
            assertThat(getRoleRepresentationsForUserInKeycloak(user)).isEmpty();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_removing_multiple_and_authority_not_found() {
            final var state = persistState(randomizeState());
            final var authority = getRandomAuthority();
            final var user = persistUser(randomizeUser(state), authority);
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{userId}/authorities")
                            .queryParam("ids", String.join(",", authority, "whatever"))
                            .build(user.getId())
                    )
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(1 + PRE_CREATED_USERS_IN_KEYCLOAK);
            final List<RoleRepresentation> assignedRoles = getRoleRepresentationsForUserInKeycloak(user);
            assertThat(assignedRoles).hasSize(1);
            assertThat(assignedRoles.stream().anyMatch(r -> authority.equals(r.getName()))).isTrue();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_role() {
            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/{userId}/authorities", 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_adding_not_permitted_due_to_insufficient_permission() {
            final var state = persistState(randomizeState());
            final var authorities = getRandomAuthorities(2);
            final var user = persistUser(randomizeUser(state), authorities.getFirst());
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{userId}/authorities/{id}", user.getId(), getRandomAuthority())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_adding_not_permitted_due_to_insufficient_role() {
            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{userId}/authorities/{id}", 1L, getRandomAuthority())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_adding_multiple_not_permitted_due_to_insufficient_permission() {
            final var state = persistState(randomizeState());
            final var authorities = getRandomAuthorities(2);
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final ProblemDetail result = restTestClient
                    .put()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{userId}/authorities")
                            .queryParam("ids", String.join(",", authorities))
                            .build(user.getId())
                    )
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_adding_multiple_not_permitted_due_to_insufficient_role() {
            final ProblemDetail result = restTestClient
                    .put()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{userId}/authorities")
                            .queryParam("ids", String.join(",", "1", "2"))
                            .build(1L)
                    )
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_deleting_not_permitted_due_to_insufficient_permission() {
            final var state = persistState(randomizeState());
            final var authority = getRandomAuthority();
            final var user = persistUser(randomizeUser(state), authority);
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{userId}/authorities/{id}", user.getId(), authority)
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_deleting_not_permitted_due_to_insufficient_role() {
            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{userId}/authorities/{id}", 1L, getRandomAuthority())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_deleting_multiple_not_permitted_due_to_insufficient_permission() {
            final var state = persistState(randomizeState());
            final var authorities = getRandomAuthorities(2);
            final var user = persistUser(randomizeUser(state), authorities.getFirst(), authorities.get(1));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{userId}/authorities")
                            .queryParam("ids", String.join(",", authorities))
                            .build(user.getId())
                    )
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_deleting_multiple_not_permitted_due_to_insufficient_role() {
            final ProblemDetail result = restTestClient
                    .delete()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{userId}/authorities")
                            .queryParam("ids", String.join(",", "1", "2"))
                            .build(1L)
                    )
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }
    }

    @Nested
    @DisplayName("State")
    class StateTests {

        @Test
        void should_return_404() {
            restTestClient
                    .get()
                    .uri("/{userId}/state", 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        void should_return() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final StateDto result = restTestClient
                    .get()
                    .uri("/{userId}/state", user.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(StateDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(user.getState().getId());
        }

        @Test
        void should_set_and_return_204() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            final var newState = persistState(randomizeState());
            newState.setEnabled(!state.getEnabled());
            stateRepository.save(newState);
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            restTestClient
                    .put()
                    .uri("/{userId}/state/{id}", user.getId(), newState.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(repository.findById(user.getId()).map(User::getState).orElseThrow(() -> new RuntimeException("User not found"))).isEqualTo(newState);
            assertThat(getUserRepresentationForUserInKeycloak(user).isEnabled()).isEqualTo(newState.getEnabled());
        }

        @Test
        void should_return_404_when_setting_and_user_not_found() {
            final var state = persistState(randomizeState());

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{userId}/state/{id}", 1L, state.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_setting_and_state_not_found() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{userId}/state/{id}", user.getId(), "whatever")
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_role() {
            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/{userId}/state", 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_setting_not_permitted_due_to_insufficient_permission() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            final var newState = persistState(randomizeState());
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{userId}/state/{id}", user.getId(), newState.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_setting_not_permitted_due_to_insufficient_role() {
            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{userId}/state/{id}", 1L, 2L)
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }
    }

    @Nested
    @DisplayName("Spexare")
    class SpexareTests {

        @Test
        void should_return_404() {
            restTestClient
                    .get()
                    .uri("/{userId}/spexare", 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound();
        }

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

            final SpexareDto result = restTestClient
                    .get()
                    .uri("/{userId}/spexare", user.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(SpexareDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(spexare.getId());
        }

        @Test
        void should_add_and_return_204() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            final var spexare = persistSpexare(randomizeSpexare());
            grantAdministrationPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantAdministrationPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            restTestClient
                    .put()
                    .uri("/{userId}/spexare/{id}", user.getId(), spexare.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(repository.findById(user.getId()).map(User::getSpexare).orElseThrow(() -> new RuntimeException("User not found"))).isEqualTo(spexare);
        }

        @Test
        void should_return_404_when_adding_and_user_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{userId}/spexare/{id}", 1L, spexare.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_adding_and_spexare_not_found() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{userId}/spexare/{id}", user.getId(), 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_remove_and_return_204() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            final var spexare = persistSpexare(randomizeSpexare());
            user.setSpexare(spexare);
            repository.save(user);
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            restTestClient
                    .delete()
                    .uri("/{userId}/spexare", user.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(repository.findById(user.getId()).map(User::getSpexare)).isEmpty();
        }

        @Test
        void should_return_404_when_removing_and_user_not_found() {
            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{userId}/spexare", 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_role() {
            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/{userId}/spexare", 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_adding_not_permitted_due_to_insufficient_permission() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{userId}/spexare/{id}", user.getId(), spexare.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_adding_not_permitted_due_to_insufficient_role() {
            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{userId}/spexare/{id}", 1L, 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_removing_not_permitted_due_to_insufficient_permission() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{userId}/spexare", user.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_403_when_removing_not_permitted_due_to_insufficient_role() {
            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{userId}/spexare", 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }
    }

    @Nested
    @DisplayName("Events")
    class EventTests {

        @Test
        void should_return_found() {
            final var state = persistState(randomizeState());
            final var user = persistUser(randomizeUser(state));
            grantReadPermissionToRoleAdmin(toObjectIdentity(User.class, user.getId()));

            final List<EventDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri("/events/{sourceId}", user.getId())
                                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<EventDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("events");

            assertThat(eventRepository.count()).isEqualTo(1);
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getEventType()).isEqualTo(Event.EventType.CREATE.name());
            assertThat(result.getFirst().getSourceType()).isEqualTo(Event.SourceType.USER.name());
            assertThat(result.getFirst().getCreatedBy()).isEqualTo(user.getCreatedBy());
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
