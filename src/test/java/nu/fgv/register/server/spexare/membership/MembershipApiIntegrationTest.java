package nu.fgv.register.server.spexare.membership;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.settings.TypeRepository;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.user.UserDetails;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.randomizer.SocialSecurityNumberRandomizer;
import nu.fgv.register.server.util.randomizer.YearRandomizer;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;

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

public class MembershipApiIntegrationTest extends AbstractIntegrationTest {

    private static String basePath;
    private final EasyRandom random;
    @LocalServerPort
    private int localPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MembershipRepository repository;

    @Autowired
    private TypeRepository typeRepository;

    @Autowired
    private SpexareRepository spexareRepository;

    public MembershipApiIntegrationTest() {
        final EasyRandomParameters parameters = new EasyRandomParameters();
        parameters
                .randomize(
                        named("year"), new YearRandomizer()
                )
                .randomize(
                        named("socialSecurityNumber"), new SocialSecurityNumberRandomizer()
                )
                .excludeField(named("spouse").and(ofType(Spexare.class)).and(inClass(Spexare.class)))
                .excludeField(named("userDetails").and(ofType(UserDetails.class)).and(inClass(Spexare.class)))
                .excludeField(named("activities").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("tags").and(ofType(Set.class)).and(inClass(Spexare.class)))
                .excludeField(named("addresses").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("memberships").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("consents").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("toggles").and(ofType(List.class)).and(inClass(Spexare.class)))
                .randomizationDepth(1);
        random = new EasyRandom(parameters);
    }

    @BeforeAll
    public static void beforeClass() {
        basePath = MembershipApi.class.getAnnotation(RequestMapping.class).value()[0];
    }

    @BeforeEach
    public void setUp() {
        RestAssured.port = localPort;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        final RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder();
        requestSpecBuilder.setBasePath(basePath);
        RestAssured.requestSpecification = requestSpecBuilder.build();
        RestAssured.config = config().encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false));
        repository.deleteAll();
        spexareRepository.deleteAll();
        typeRepository.deleteAll();
    }

    @AfterEach
    public void tearDown() {
        RestAssured.reset();
    }

    @Nested
    @DisplayName("Retrieve paged by spexare")
    class RetrievePagedBySpexareTests {

        @Test
        public void should_return_404() {
            //@formatter:off
            given()
                .contentType(ContentType.JSON)
                .pathParam("spexareId","1")
            .when()
                .get()
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        public void should_return_zero() {
            var spexare = persistSpexare(randomizeSpexare());

            //@formatter:off
            final List<MembershipDto> result =
                    given()
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.memberships", MembershipDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        public void should_return_one() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            persistMembership(randomizeMembership(type, spexare));

            //@formatter:off
            final List<MembershipDto> result =
                    given()
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.memberships", MembershipDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        public void should_return_many() {
            int size = 42;
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            IntStream.range(0, size).forEach(i -> persistMembership(randomizeMembership(type, spexare)));

            //@formatter:off
            final List<MembershipDto> result =
                    given()
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .queryParam("size", size)
                        .get()
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.memberships", MembershipDto.class);
            //@formatter:on

            assertThat(result).hasSize(size);
        }

    }

    @Nested
    @DisplayName("Retrieve paged by spexare and type")
    class RetrievePagedBySpexareAndTypeTests {

        @Test
        public void should_return_404() {
            var spexare = persistSpexare(randomizeSpexare());

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .get("/type/{type}", "dummy")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        public void should_return_zero() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());

            //@formatter:off
            final List<MembershipDto> result =
                    given()
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .get("/type/{type}", type.getValue())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.memberships", MembershipDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        public void should_return_one() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            persistMembership(randomizeMembership(type, spexare));

            //@formatter:off
            final List<MembershipDto> result =
                    given()
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .get("/type/{type}", type.getValue())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.memberships", MembershipDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        public void should_return_many() {
            int size = 42;
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            IntStream.range(0, size).forEach(i -> persistMembership(randomizeMembership(type, spexare)));

            //@formatter:off
            final List<MembershipDto> result =
                    given()
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .queryParam("size", size)
                        .get("/type/{type}", type.getValue())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.memberships", MembershipDto.class);
            //@formatter:on

            assertThat(result).hasSize(size);
        }

    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {
        @Test
        public void should_return_found() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());
            var persisted = persistMembership(randomizeMembership(type, spexare));

            //@formatter:off
            final MembershipDto result =
                    given()
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .get("/{id}", persisted.getId())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body().as(MembershipDto.class);
            //@formatter:on

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id", "year")
                    .contains(persisted.getId(), persisted.getYear());
        }

        @Test
        public void should_return_404_when_not_found() {
            //@formatter:off
            given()
                .contentType(ContentType.JSON)
                .pathParam("spexareId", 1)
            .when()
                .get("/{id}", "123")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }
    }

    @Nested
    @DisplayName("Add membership")
    class AddMembershipTests {

        @Test
        public void should_add_and_return_201() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .put("/{type}/{year}", type.getValue(), "2023")
            .then()
                .statusCode(HttpStatus.ACCEPTED.value())
                .extract().body().asString();
            //@formatter:on

            //@formatter:off
            final List<MembershipDto> result =
                    given()
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .get("/type/{type}", type.getValue())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.memberships", MembershipDto.class);
            //@formatter:on

            assertThat(result).hasSize(1);
        }

        @Test
        public void should_return_400_when_adding_already_existing_year() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .put("/{type}/{year}", type.getValue(), "2023")
            .then()
                .statusCode(HttpStatus.ACCEPTED.value())
                .extract().body().asString();
            //@formatter:on

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .put("/{type}/{year}", type.getValue(), "2023")
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
            //@formatter:on
        }

        @Test
        public void should_return_404_when_adding_and_spexare_not_found() {
            var type = persistType(randomizeType());

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
                .pathParam("spexareId", "1")
            .when()
                .put("/{type}/{year}", type.getValue(), "2023")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        public void should_return_404_when_adding_and_type_not_found() {
            var spexare = persistSpexare(randomizeSpexare());

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .put("/{type}/{year}", "dummy", "2023")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

    }

    @Nested
    @DisplayName("Remove membership")
    class RemoveMembershipTests {

        @Test
        public void should_remove_and_return_204() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .put("/{type}/{year}", type.getValue(), "2023")
            .then()
                .statusCode(HttpStatus.ACCEPTED.value())
                .extract().body().asString();
            //@formatter:on

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .delete("/{type}/{year}", type.getValue(), "2023")
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value())
                .extract().body().asString();
            //@formatter:on

            //@formatter:off
            final List<MembershipDto> result =
                    given()
                        .contentType(ContentType.JSON)
                        .pathParam("spexareId", spexare.getId())
                    .when()
                        .get("/type/{type}", type.getValue())
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract().body()
                        .jsonPath().getList("_embedded.memberships", MembershipDto.class);
            //@formatter:on

            assertThat(result).isEmpty();
        }

        @Test
        public void should_return_400_when_removing_non_existing_year() {
            var spexare = persistSpexare(randomizeSpexare());
            var type = persistType(randomizeType());

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .delete("/{type}/{year}", type.getValue(), "2023")
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
            //@formatter:on
        }

        @Test
        public void should_return_404_when_removing_and_spexare_not_found() {
            var type = persistType(randomizeType());

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
                .pathParam("spexareId", "1")
            .when()
                .delete("/{type}/{year}", type.getValue(), "2023")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

        @Test
        public void should_return_404_when_removing_and_type_not_found() {
            var spexare = persistSpexare(randomizeSpexare());

            //@formatter:off
            given()
                .contentType(ContentType.JSON)
                .pathParam("spexareId", spexare.getId())
            .when()
                .delete("/{type}/{year}", "dummy", "2023")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
            //@formatter:on
        }

    }

    private Membership randomizeMembership(Type type, Spexare spexare) {
        var membership = random.nextObject(Membership.class);
        membership.setSpexare(spexare);
        membership.setType(type);
        return membership;
    }

    private Membership persistMembership(Membership membership) {
        return repository.save(membership);
    }

    private Type randomizeType() {
        return random.nextObject(Type.class);
    }

    private Type persistType(Type type) {
        return typeRepository.save(type);
    }

    private Spexare randomizeSpexare() {
        return random.nextObject(Spexare.class);
    }

    private Spexare persistSpexare(Spexare spexare) {
        return spexareRepository.save(spexare);
    }

}
