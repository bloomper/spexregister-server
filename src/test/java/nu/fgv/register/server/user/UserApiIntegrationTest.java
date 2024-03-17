package nu.fgv.register.server.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
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
import nu.fgv.register.server.util.randomizer.SocialSecurityNumberRandomizer;
import nu.fgv.register.server.util.security.SecurityUtil;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.EmailRandomizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.inClass;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;
import static org.passay.AllowedCharacterRule.ERROR_CODE;

@Disabled
class UserApiIntegrationTest extends AbstractIntegrationTest {

    private static String basePath;
    private final EasyRandom random;
    @LocalServerPort
    private int localPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository repository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private SpexareRepository spexareRepository;

    @Autowired
    private EventRepository eventRepository;

    private final EmailRandomizer emailRandomizer = new EmailRandomizer();

    private final Random rnd = new Random();

    public UserApiIntegrationTest() {
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

    @BeforeAll
    public static void beforeClass() {
        basePath = UserApi.class.getAnnotation(RequestMapping.class).value()[0];
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
        stateRepository.deleteAll();
        spexareRepository.deleteAll();
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
            //@formatter:off
            final List<UserDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.users", UserDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_one() {
            var state = persistState(randomizeState());
            persistUser(randomizeUser(state));

            //@formatter:off
            final List<UserDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.users", UserDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            int size = 42;
            var state = persistState(randomizeState());
            IntStream.range(0, size).forEach(i -> persistUser(randomizeUser(state)));

            //@formatter:off
            final List<UserDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .queryParam("size", size)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.users", UserDto.class);
            //@formatter:on

            assertThat(result).hasSize(size);
        }

    }

    @Nested
    @DisplayName("Retrieve paged with filtering")
    class RetrievePagedWithFilteringTests {

        @Test
        void should_return_zero() {
            var state = persistState(randomizeState());
            persistUser(randomizeUser(state));

            //@formatter:off
            final List<UserDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .queryParam("filter", User_.EXTERNAL_ID + ":whatever")
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.users", UserDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_one() {
            var state = persistState(randomizeState());
            var user = persistUser(randomizeUser(state));

            //@formatter:off
            final List<UserDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .queryParam("filter", User_.ID + ":" + user.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.users", UserDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        void should_return_many() {
            int size = 42;
            var state = persistState(randomizeState());
            IntStream.range(0, size).forEach(i -> {
                var user = randomizeUser(state);
                persistUser(user);
            });

            //@formatter:off
            final List<UserDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .queryParam("filter", User_.ID + "=whatever*")
                        .queryParam("size", size)
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.users", UserDto.class);
            //@formatter:on

            assertThat(result).hasSize(size);
        }

    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create_and_return_201() throws Exception {
            final UserCreateDto dto = random.nextObject(UserCreateDto.class);

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

            final UserDto result = objectMapper.readValue(json, UserDto.class);
            assertThat(result)
                    .extracting("email")
                    .isEqualTo(dto.getEmail());
            assertThat(repository.count()).isEqualTo(1);
            assertThat(getUsersCountInKeycloak()).isEqualTo(4);
        }

        @Test
        void should_return_400_when_invalid_input() {
            final UserCreateDto dto = random.nextObject(UserCreateDto.class);
            dto.setEmail(null);

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

            assertThat(repository.count()).isZero();
            assertThat(getUsersCountInKeycloak()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        void should_return_found() {
            var state = persistState(randomizeState());
            var user = persistUser(randomizeUser(state));

            //@formatter:off
            final UserDto result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", user.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(UserDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("email")
                    .isEqualTo(result.getEmail());
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
            var state = persistState(randomizeState());
            var user = persistUser(randomizeUser(state));

            //@formatter:off
            final UserDto before =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", user.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(UserDto.class);
            //@formatter:on

            final UserUpdateDto dto = UserUpdateDto.builder()
                    .id(before.getId())
                    .email("a" + before.getEmail())
                    .build();

            //@formatter:off
            final String json =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .body(dto)
                    .when()
                        .put("/{id}", user.getId())
                    .then()
                        .statusCode(HttpStatus.ACCEPTED.value())
                        .extract().body().asString();
            //@formatter:on

            final UserDto updated = objectMapper.readValue(json, UserDto.class);

            //@formatter:off
            final UserDto after =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", user.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(UserDto.class);
            //@formatter:on

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
            assertThat(getUsersCountInKeycloak()).isEqualTo(4);
        }

        @Test
        void should_return_400_when_invalid_input() {
            final UserUpdateDto dto = random.nextObject(UserUpdateDto.class);
            dto.setEmail(null);

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

            assertThat(repository.count()).isZero();
            assertThat(getUsersCountInKeycloak()).isEqualTo(3);
        }

        @Test
        void should_return_404_when_not_found() {
            final UserUpdateDto dto = random.nextObject(UserUpdateDto.class);

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

            assertThat(repository.count()).isZero();
            assertThat(getUsersCountInKeycloak()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Partial update")
    class PartialUpdateTests {

        @Test
        void should_update_and_return_202() throws Exception {
            var state = persistState(randomizeState());
            var user = persistUser(randomizeUser(state));

            //@formatter:off
            final UserDto before =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", user.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                    .extract().body().as(UserDto.class);
            //@formatter:on

            final UserUpdateDto dto = UserUpdateDto.builder()
                    .id(before.getId())
                    .email("a" + before.getEmail())
                    .build();

            //@formatter:off
            final String json =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                        .body(dto)
                    .when()
                        .patch("/{id}", user.getId())
                    .then()
                        .statusCode(HttpStatus.ACCEPTED.value())
                        .extract().body().asString();
            //@formatter:on

            final UserDto updated = objectMapper.readValue(json, UserDto.class);

            //@formatter:off
            final UserDto after =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{id}", user.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(UserDto.class);
            //@formatter:on

            assertThat(after)
                    .usingRecursiveComparison()
                    .ignoringFields("createdBy", "createdAt", "lastModifiedBy", "lastModifiedAt")
                    .isEqualTo(updated);
            assertThat(repository.count()).isEqualTo(1);
            assertThat(getUsersCountInKeycloak()).isEqualTo(4);
        }

        @Test
        void should_return_404_when_not_found() {
            final UserUpdateDto dto = random.nextObject(UserUpdateDto.class);

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

            assertThat(repository.count()).isZero();
            assertThat(getUsersCountInKeycloak()).isEqualTo(3);
        }

    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        void should_delete_and_return_204() {
            var state = persistState(randomizeState());
            var user = persistUser(randomizeUser(state));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}", user.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
            assertThat(getUsersCountInKeycloak()).isEqualTo(3);
        }

        @Test
        void should_return_404_when_not_found() {
            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .delete("/{id}", 123)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(repository.count()).isZero();
            assertThat(getUsersCountInKeycloak()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Authorities")
    class AuthorityTests {

        @Test
        void should_return_404() {
            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .get("/{userId}/authorities", 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(getUsersCountInKeycloak()).isEqualTo(3);
        }

        @Test
        void should_return_zero() {
            var state = persistState(randomizeState());
            var user = persistUser(randomizeUser(state));

            //@formatter:off
            final List<AuthorityDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{userId}/authorities", user.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.authorities", AuthorityDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
            assertThat(getUsersCountInKeycloak()).isEqualTo(4);
            assertThat(getRoleRepresentationsForUserInKeycloak(user)).isEmpty();
        }

        @Test
        void should_return_one() {
            var state = persistState(randomizeState());
            var role = randomizeRole();
            var user = persistUser(randomizeUser(state), role);

            //@formatter:off
            final List<AuthorityDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{userId}/authorities", user.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.authorities", AuthorityDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
            assertThat(getUsersCountInKeycloak()).isEqualTo(4);
            final List<RoleRepresentation> assignedRoles = getRoleRepresentationsForUserInKeycloak(user);
            assertThat(assignedRoles).hasSize(1);
            assertThat(assignedRoles.getFirst().getName()).isEqualTo(role);
        }

        @Test
        void should_return_many() {
            var state = persistState(randomizeState());
            var roles = randomizeRoles(2);
            var user = persistUser(randomizeUser(state), roles.getFirst(), roles.get(1));

            //@formatter:off
            final List<AuthorityDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{userId}/authorities", user.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.authorities", AuthorityDto.class);
            //@formatter:on

            assertThat(result).hasSize(2);
            assertThat(getUsersCountInKeycloak()).isEqualTo(4);
            final List<RoleRepresentation> assignedRoles = getRoleRepresentationsForUserInKeycloak(user);
            assertThat(assignedRoles).hasSize(2);
            assertThat(assignedRoles.stream().anyMatch(r -> roles.getFirst().equals(r.getName()))).isTrue();
            assertThat(assignedRoles.stream().anyMatch(r -> roles.get(1).equals(r.getName()))).isTrue();
        }

        @Test
        void should_add_and_return_202() {
            var state = persistState(randomizeState());
            var roles = randomizeRoles(2);
            var user = persistUser(randomizeUser(state), roles.getFirst());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .put("/{userId}/authorities/{id}", user.getId(), roles.get(1))
            .then()
                .statusCode(HttpStatus.ACCEPTED.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(4);
            final List<RoleRepresentation> assignedRoles = getRoleRepresentationsForUserInKeycloak(user);
            assertThat(assignedRoles).hasSize(2);
            assertThat(assignedRoles.stream().anyMatch(r -> roles.getFirst().equals(r.getName()))).isTrue();
            assertThat(assignedRoles.stream().anyMatch(r -> roles.get(1).equals(r.getName()))).isTrue();
        }

        @Test
        void should_return_404_when_adding_and_user_not_found() {
            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .put("/{userId}/authorities/{id}", 1L, randomizeRole())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(3);
        }

        @Test
        void should_return_404_when_adding_and_authority_not_found() {
            var state = persistState(randomizeState());
            var user = persistUser(randomizeUser(state));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .put("/{userId}/authorities/{id}", user.getId(), "whatever")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(4);
            assertThat(getRoleRepresentationsForUserInKeycloak(user)).isEmpty();
        }

        @Test
        void should_add_multiple_and_return_202() {
            var state = persistState(randomizeState());
            var roles = randomizeRoles(2);
            var user = persistUser(randomizeUser(state));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .queryParam("ids", String.join(",", roles.getFirst(), roles.get(1)))
            .when()
                .put("/{userId}/authorities", user.getId())
            .then()
                .statusCode(HttpStatus.ACCEPTED.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(4);
            final List<RoleRepresentation> assignedRoles = getRoleRepresentationsForUserInKeycloak(user);
            assertThat(assignedRoles).hasSize(2);
            assertThat(assignedRoles.stream().anyMatch(r -> roles.getFirst().equals(r.getName()))).isTrue();
            assertThat(assignedRoles.stream().anyMatch(r -> roles.get(1).equals(r.getName()))).isTrue();
        }

        @Test
        void should_return_404_when_adding_multiple_and_user_not_found() {
            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .queryParam("ids", String.join(",", randomizeRole(), randomizeRole()))
            .when()
                .put("/{userId}/authorities", 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(3);
        }

        @Test
        void should_return_404_when_adding_multiple_and_authorities_not_found() {
            var state = persistState(randomizeState());
            var user = persistUser(randomizeUser(state));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .queryParam("ids", String.join(",", "whatever1", "whatever2"))
            .when()
                .put("/{userId}/authorities", user.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(4);
            assertThat(getRoleRepresentationsForUserInKeycloak(user)).isEmpty();
        }

        @Test
        void should_return_404_when_adding_multiple_and_authority_not_found() {
            var state = persistState(randomizeState());
            var role = randomizeRole();
            var user = persistUser(randomizeUser(state));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .queryParam("ids", String.join(",", role, "whatever"))
            .when()
                .put("/{userId}/authorities", user.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(4);
            assertThat(getRoleRepresentationsForUserInKeycloak(user)).isEmpty();
        }

        @Test
        void should_remove_and_return_204() {
            var state = persistState(randomizeState());
            var role = randomizeRole();
            var user = persistUser(randomizeUser(state), role);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .delete("/{userId}/authorities/{id}", user.getId(), role)
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(4);
            assertThat(getRoleRepresentationsForUserInKeycloak(user)).isEmpty();
        }

        @Test
        void should_return_404_when_removing_and_user_not_found() {
            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .delete("/{userId}/authorities/{id}", 1L, randomizeRole())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(3);
        }

        @Test
        void should_return_404_when_removing_and_authority_not_found() {
            var state = persistState(randomizeState());
            var user = persistUser(randomizeUser(state));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .delete("/{userId}/authorities/{id}", user.getId(), "whatever")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(4);
            assertThat(getRoleRepresentationsForUserInKeycloak(user)).isEmpty();
        }

        @Test
        void should_remove_multiple_and_return_202() {
            var state = persistState(randomizeState());
            var roles = randomizeRoles(2);
            var user = persistUser(randomizeUser(state), roles.getFirst(), roles.get(1));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .queryParam("ids", String.join(",", roles.getFirst(), roles.get(1)))
            .when()
                .delete("/{userId}/authorities", user.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(4);
            assertThat(getRoleRepresentationsForUserInKeycloak(user)).isEmpty();
        }

        @Test
        void should_return_404_when_removing_multiple_and_user_not_found() {
            var roles = randomizeRoles(2);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .queryParam("ids", String.join(",", roles.getFirst(), roles.get(1)))
            .when()
                .delete("/{userId}/authorities", 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(3);
        }

        @Test
        void should_return_404_when_removing_multiple_and_authorities_not_found() {
            var state = persistState(randomizeState());
            var user = persistUser(randomizeUser(state));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .queryParam("ids", String.join(",", "whatever1", "whatever2"))
            .when()
                .delete("/{userId}/authorities", user.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(4);
            assertThat(getRoleRepresentationsForUserInKeycloak(user)).isEmpty();
        }

        @Test
        void should_return_404_when_removing_multiple_and_authority_not_found() {
            var state = persistState(randomizeState());
            var role = randomizeRole();
            var user = persistUser(randomizeUser(state), role);

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .queryParam("ids", String.join(",", role, "whatever"))
            .when()
                .delete("/{userId}/authorities", user.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(3);
            assertThat(getUsersCountInKeycloak()).isEqualTo(4);
            final List<RoleRepresentation> assignedRoles = getRoleRepresentationsForUserInKeycloak(user);
            assertThat(assignedRoles).hasSize(1);
            assertThat(assignedRoles.stream().anyMatch(r -> role.equals(r.getName()))).isTrue();
        }
    }

    @Nested
    @DisplayName("State")
    class StateTests {

        @Test
        void should_return_404() {
            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .get("/{userId}/state", 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return() {
            var state = persistState(randomizeState());
            var user = persistUser(randomizeUser(state));

            //@formatter:off
            final StateDto result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{userId}/state", user.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(StateDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(user.getState().getId());
        }

        @Test
        void should_set_and_return_202() {
            var state = persistState(randomizeState());
            var user = persistUser(randomizeUser(state));
            var newState = persistState(randomizeState());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .put("/{userId}/state/{id}", user.getId(), newState.getId())
            .then()
                .statusCode(HttpStatus.ACCEPTED.value());
            //@formatter:on

            assertThat(repository.findById(user.getId()).map(User::getState).orElseThrow(() -> new RuntimeException("User not found"))).isEqualTo(newState);
        }

        @Test
        void should_return_404_when_setting_and_user_not_found() {
            var state = persistState(randomizeState());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .put("/{userId}/state/{id}", 1L, state.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return_404_when_setting_and_state_not_found() {
            var state = persistState(randomizeState());
            var user = persistUser(randomizeUser(state));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .put("/{userId}/state/{id}", user.getId(), "WHATEVER")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

    }

    @Nested
    @DisplayName("Spexare")
    class SpexareTests {

        @Test
        void should_return_404() {
            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .get("/{userId}/spexare", 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return() {
            var state = persistState(randomizeState());
            var user = persistUser(randomizeUser(state));
            var spexare = persistSpexare(randomizeSpexare());
            user.setSpexare(spexare);
            repository.save(user);

            //@formatter:off
            final SpexareDto result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/{userId}/spexare", user.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(SpexareDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(spexare.getId());
        }

        @Test
        void should_add_and_return_202() {
            var state = persistState(randomizeState());
            var user = persistUser(randomizeUser(state));
            var spexare = persistSpexare(randomizeSpexare());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .put("/{userId}/spexare/{id}", user.getId(), spexare.getId())
            .then()
                .statusCode(HttpStatus.ACCEPTED.value());
            //@formatter:on

            assertThat(repository.findById(user.getId()).map(User::getSpexare).orElseThrow(() -> new RuntimeException("User not found"))).isEqualTo(spexare);
        }

        @Test
        void should_return_404_when_adding_and_user_not_found() {
            var spexare = persistSpexare(randomizeSpexare());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .put("/{userId}/spexare/{id}", 1L, spexare.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        void should_return_404_when_adding_and_spexare_not_found() {
            var state = persistState(randomizeState());
            var user = persistUser(randomizeUser(state));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .put("/{userId}/spexare/{id}", user.getId(), 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

    }

    @Nested
    @DisplayName("Events")
    class EventTests {

        @Test
        void should_return_found() {
            var state = persistState(randomizeState());
            var user = persistUser(randomizeUser(state));

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

            assertThat(eventRepository.count()).isEqualTo(7L);
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getEvent()).isEqualTo(Event.EventType.CREATE.name());
            assertThat(result.getFirst().getSource()).isEqualTo(Event.SourceType.USER.name());
            assertThat(result.getFirst().getCreatedBy()).isEqualTo(user.getCreatedBy());
        }

        @Test
        void should_return_403_when_not_permitted() {
            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .get("/events")
            .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
            //@formatter:on
        }
    }

    private User randomizeUser(State state) {
        var user = random.nextObject(User.class);
        if (state != null) {
            user.setState(state);
        }
        return user;
    }

    private User persistUser(User user, String... roles) {
        var representation = persistUserInKeycloak(roles);
        user.setExternalId(representation.getId());
        return repository.save(user);
    }

    private UserRepresentation persistUserInKeycloak(String... roles) {
        final UserRepresentation userRepresentation = new UserRepresentation();

        userRepresentation.setEmail(emailRandomizer.getRandomValue());
        userRepresentation.setEnabled(true);

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

    private List<RoleRepresentation> getRoleRepresentationsForUserInKeycloak(User user) {
        return keycloakAdminClient
                .realm(keycloakRealm)
                .users()
                .get(user.getExternalId())
                .roles()
                .clientLevel(keycloakClientId)
                .listAll();
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

        return passwordGenerator.generatePassword(15, specialCharacterRule, lowerCaseRule, upperCaseRule, digitRule);
    }

    private String randomizeRole() {
        return SecurityUtil.ROLES.get(rnd.nextInt(SecurityUtil.ROLES.size()));
    }

    private List<String> randomizeRoles(int numberOfRoles) {
        if (numberOfRoles > SecurityUtil.ROLES.size()) {
            throw new IllegalArgumentException();
        }
        final List<String> randomRoles = new ArrayList<>(SecurityUtil.ROLES);
        Collections.shuffle(randomRoles);
        return randomRoles.subList(0, numberOfRoles);
    }

    private Authority randomizeAuthority() {
        return random.nextObject(Authority.class);
    }

    private Authority persistAuthority(Authority authority) {
        return authorityRepository.save(authority);
    }

    private State randomizeState() {
        return random.nextObject(State.class);
    }

    private State persistState(State state) {
        return stateRepository.save(state);
    }

    private Spexare randomizeSpexare() {
        return random.nextObject(Spexare.class);
    }

    private Spexare persistSpexare(Spexare spexare) {
        return spexareRepository.save(spexare);
    }
}
