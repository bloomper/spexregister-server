package nu.fgv.register.server.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static org.assertj.core.api.Assertions.assertThat;

class SessionApiIntegrationTest extends AbstractIntegrationTest {

    private static String basePath;
    private final EasyRandom random;
    @LocalServerPort
    private int localPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    public SessionApiIntegrationTest() {
        final EasyRandomParameters parameters = new EasyRandomParameters();
        random = new EasyRandom(parameters);
    }

    @BeforeAll
    public static void beforeClass() {
        basePath = SessionApi.class.getAnnotation(RequestMapping.class).value()[0];
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

        eventRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        RestAssured.reset();
    }

    @Nested
    @DisplayName("Events")
    class EventTests {

        @Test
        void should_return_found() {
            var event = persistEvent(randomizeEvent());

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
            assertThat(result.getFirst().getEvent()).isEqualTo(event.getEvent().name());
            assertThat(result.getFirst().getSource()).isEqualTo(Event.SourceType.SESSION.name());
            assertThat(result.getFirst().getCreatedBy()).isEqualTo(event.getCreatedBy());
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

    private Event randomizeEvent() {
        var event = random.nextObject(Event.class);
        event.setSource(Event.SourceType.SESSION);
        return event;
    }

    private Event persistEvent(Event event) {
        return eventRepository.save(event);
    }

}
