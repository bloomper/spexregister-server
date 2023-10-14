package nu.fgv.register.server.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
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
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.EmailRandomizer;
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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.inClass;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;

public class UserApiIntegrationTest extends AbstractIntegrationTest {

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

    public UserApiIntegrationTest() {
        final EasyRandomParameters parameters = new EasyRandomParameters();
        parameters
                .randomize(
                        named("username"), new EmailRandomizer()
                )
                .excludeField(named("password").and(ofType(String.class)).and(inClass(User.class)))
                .excludeField(named("spexare").and(ofType(Spexare.class)).and(inClass(User.class)))
                .excludeField(named("authorities").and(ofType(Set.class)).and(inClass(User.class)))
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

    @BeforeAll
    public static void beforeClass() {
        basePath = UserApi.class.getAnnotation(RequestMapping.class).value()[0];
    }

    @BeforeEach
    public void setUp() {
        RestAssured.port = localPort;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        final RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder();
        requestSpecBuilder.setBasePath(basePath);
        RestAssured.requestSpecification = requestSpecBuilder.build();
        RestAssured.config = config()
                .encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))
                .logConfig(LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails());

        repository.deleteAll();
        authorityRepository.deleteAll();
        stateRepository.deleteAll();
        spexareRepository.deleteAll();
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
        public void should_return_one() {
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
        public void should_return_many() {
            int size = 42;
            var state = persistState(randomizeState());
            IntStream.range(0, size).forEach(i -> persistUser(randomizeUser(state)));

            //@formatter:off
            final List<UserDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .queryParam("size", size)
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
        public void should_create_and_return_201() throws Exception {
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
                    .extracting("username")
                    .isEqualTo(dto.getUsername());
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_400_when_invalid_input() {
            final UserCreateDto dto = random.nextObject(UserCreateDto.class);
            dto.setUsername(null);

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

            assertThat(repository.count()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        public void should_return_found() {
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
                    .extracting("username")
                    .isEqualTo(result.getUsername());
        }

        @Test
        public void should_return_404_when_not_found() {
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
        public void should_update_and_return_202() throws Exception {
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
                    .username(before.getUsername() + "_")
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
        }

        @Test
        public void should_return_400_when_invalid_input() {
            final UserUpdateDto dto = random.nextObject(UserUpdateDto.class);
            dto.setUsername(null);

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

            assertThat(repository.count()).isEqualTo(0);
        }

        @Test
        public void should_return_404_when_not_found() {
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

            assertThat(repository.count()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Partial update")
    class PartialUpdateTests {

        @Test
        public void should_update_and_return_202() throws Exception {
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
                    .username(before.getUsername() + "_")
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
        }

        @Test
        public void should_return_404_when_not_found() {
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

            assertThat(repository.count()).isEqualTo(0);
        }

    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        public void should_delete_and_return_204() {
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

            assertThat(repository.count()).isEqualTo(0);
        }

        @Test
        public void should_return_404_when_not_found() {
            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
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
    @DisplayName("Authorities")
    class AuthorityTests {

        @Test
        public void should_return_404() {
            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .get("/{userId}/authorities", 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        public void should_return_zero() {
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
        }

        @Test
        public void should_return_one() {
            var state = persistState(randomizeState());
            var authority = persistAuthority(randomizeAuthority());
            var user = persistUser(randomizeUser(state, authority));

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
        }

        @Test
        public void should_return_many() {
            var state = persistState(randomizeState());
            var authority1 = persistAuthority(randomizeAuthority());
            var authority2 = persistAuthority(randomizeAuthority());
            var user = persistUser(randomizeUser(state, authority1, authority2));

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
        }

        @Test
        public void should_add_and_return_202() {
            var state = persistState(randomizeState());
            var authority = persistAuthority(randomizeAuthority());
            var user = persistUser(randomizeUser(state, authority));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .put("/{userId}/authorities/{id}", user.getId(), authority.getId())
            .then()
                .statusCode(HttpStatus.ACCEPTED.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(1);
            assertThat(repository.findById(user.getId()).map(User::getAuthorities).orElseThrow(() -> new RuntimeException("User not found"))).hasSize(1);
        }

        @Test
        public void should_return_404_when_adding_and_user_not_found() {
            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .put("/{userId}/authorities/{id}", 1L, "ROLE_USER")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(0);
        }

        @Test
        public void should_return_404_when_adding_and_authority_not_found() {
            var state = persistState(randomizeState());
            var user = persistUser(randomizeUser(state));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .put("/{userId}/authorities/{id}", user.getId(), "ROLE_USER")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(0);
            assertThat(repository.findById(user.getId()).map(User::getAuthorities).orElseThrow(() -> new RuntimeException("User not found"))).hasSize(0);
        }

        @Test
        public void should_add_multiple_and_return_202() {
            var state = persistState(randomizeState());
            var authority1 = persistAuthority(randomizeAuthority());
            var authority2 = persistAuthority(randomizeAuthority());
            var user = persistUser(randomizeUser(state));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .queryParam("ids", String.join(",", authority1.getId(), authority2.getId()))
            .when()
                .put("/{userId}/authorities", user.getId())
            .then()
                .statusCode(HttpStatus.ACCEPTED.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(2);
            assertThat(repository.findById(user.getId()).map(User::getAuthorities).orElseThrow(() -> new RuntimeException("User not found"))).hasSize(2);
        }

        @Test
        public void should_return_404_when_adding_multiple_and_user_not_found() {
            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .queryParam("ids", String.join(",", "ROLE_USER", "ROLE_EDITOR"))
            .when()
                .put("/{userId}/authorities", 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(0);
        }

        @Test
        public void should_return_404_when_adding_multiple_and_authorities_not_found() {
            var state = persistState(randomizeState());
            var user = persistUser(randomizeUser(state));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .queryParam("ids", String.join(",", "ROLE_USER", "ROLE_EDITOR"))
            .when()
                .put("/{userId}/authorities", user.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(0);
            assertThat(repository.findById(user.getId()).map(User::getAuthorities).orElseThrow(() -> new RuntimeException("User not found"))).hasSize(0);
        }

        @Test
        public void should_return_404_when_adding_multiple_and_authority_not_found() {
            var state = persistState(randomizeState());
            var authority = persistAuthority(randomizeAuthority());
            var user = persistUser(randomizeUser(state));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .queryParam("ids", String.join(",", authority.getId(), "ROLE_USER"))
            .when()
                .put("/{userId}/authorities", user.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(1);
            assertThat(repository.findById(user.getId()).map(User::getAuthorities).orElseThrow(() -> new RuntimeException("User not found"))).hasSize(0);
        }

        @Test
        public void should_remove_and_return_204() {
            var state = persistState(randomizeState());
            var authority = persistAuthority(randomizeAuthority());
            var user = persistUser(randomizeUser(state, authority));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .delete("/{userId}/authorities/{id}", user.getId(), authority.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(1);
            assertThat(repository.findById(user.getId()).map(User::getAuthorities).orElseThrow(() -> new RuntimeException("User not found"))).hasSize(0);
        }

        @Test
        public void should_return_404_when_removing_and_user_not_found() {
            var authority = persistAuthority(randomizeAuthority());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .delete("/{userId}/authorities/{id}", 1L, authority.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(1);
        }

        @Test
        public void should_return_404_when_removing_and_authority_not_found() {
            var state = persistState(randomizeState());
            var user = persistUser(randomizeUser(state));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
            .when()
                .delete("/{userId}/authorities/{id}", user.getId(), "ROLE_USER")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(0);
            assertThat(repository.findById(user.getId()).map(User::getAuthorities).orElseThrow(() -> new RuntimeException("User not found"))).hasSize(0);
        }

        @Test
        public void should_remove_multiple_and_return_202() {
            var state = persistState(randomizeState());
            var authority1 = persistAuthority(randomizeAuthority());
            var authority2 = persistAuthority(randomizeAuthority());
            var user = persistUser(randomizeUser(state, authority1, authority2));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .queryParam("ids", String.join(",", authority1.getId(), authority2.getId()))
            .when()
                .delete("/{userId}/authorities", user.getId())
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(2);
            assertThat(repository.findById(user.getId()).map(User::getAuthorities).orElseThrow(() -> new RuntimeException("User not found"))).hasSize(0);
        }

        @Test
        public void should_return_404_when_removing_multiple_and_user_not_found() {
            var authority1 = persistAuthority(randomizeAuthority());
            var authority2 = persistAuthority(randomizeAuthority());

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .queryParam("ids", String.join(",", authority1.getId(), authority2.getId()))
            .when()
                .delete("/{userId}/authorities", 1L)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(2);
        }

        @Test
        public void should_return_404_when_removing_multiple_and_authorities_not_found() {
            var state = persistState(randomizeState());
            var user = persistUser(randomizeUser(state));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .queryParam("ids", String.join(",", "ROLE_USER", "ROLE_EDITOR"))
            .when()
                .delete("/{userId}/authorities", user.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(0);
            assertThat(repository.findById(user.getId()).map(User::getAuthorities).orElseThrow(() -> new RuntimeException("User not found"))).hasSize(0);
        }

        @Test
        public void should_return_404_when_removing_multiple_and_authority_not_found() {
            var state = persistState(randomizeState());
            var authority = persistAuthority(randomizeAuthority());
            var user = persistUser(randomizeUser(state, authority));

            //@formatter:off
            given()
                .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                .contentType(ContentType.JSON)
                .queryParam("ids", String.join(",", authority.getId(), "ROLE_USER"))
            .when()
                .delete("/{userId}/authorities", user.getId())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on

            assertThat(authorityRepository.count()).isEqualTo(1);
            assertThat(repository.findById(user.getId()).map(User::getAuthorities).orElseThrow(() -> new RuntimeException("User not found"))).hasSize(1);
        }
    }

    @Nested
    @DisplayName("State")
    class StateTests {

        @Test
        public void should_return_404() {
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
        public void should_return() {
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
        public void should_set_and_return_202() {
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
        public void should_return_404_when_setting_and_user_not_found() {
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
        public void should_return_404_when_setting_and_state_not_found() {
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
        public void should_return_404() {
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
        public void should_return() {
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
        public void should_add_and_return_202() {
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
        public void should_return_404_when_adding_and_user_not_found() {
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
        public void should_return_404_when_adding_and_spexare_not_found() {
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
        public void should_return_found() {
            var state = persistState(randomizeState());
            var user = persistUser(randomizeUser(state));

            //@formatter:off
            final List<EventDto> result =
                    given()
                        .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                        .contentType(ContentType.JSON)
                    .when()
                        .get("/events")
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.events", EventDto.class);
            //@formatter:on

            assertThat(eventRepository.count()).isEqualTo(3L);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEvent()).isEqualTo(Event.EventType.CREATE.name());
            assertThat(result.get(0).getSource()).isEqualTo(Event.SourceType.USER.name());
            assertThat(result.get(0).getCreatedBy()).isEqualTo(user.getCreatedBy());
        }

    }

    private User randomizeUser(State state, Authority... authorities) {
        var user = random.nextObject(User.class);
        if (authorities.length != 0) {
            user.setAuthorities(Set.copyOf(Arrays.asList(authorities)));
        }
        if (state != null) {
            user.setState(state);
        }
        return user;
    }

    private User persistUser(User user) {
        return repository.save(user);
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
