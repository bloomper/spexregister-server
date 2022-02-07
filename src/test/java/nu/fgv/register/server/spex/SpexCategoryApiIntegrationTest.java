package nu.fgv.register.server.spex;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

public class SpexCategoryApiIntegrationTest extends AbstractIntegrationTest {

    private static String basePath;
    private final EasyRandom random = new EasyRandom();
    @LocalServerPort
    private int localPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpexCategoryRepository repository;

    @BeforeAll
    public static void beforeClass() {
        basePath = SpexCategoryApi.class.getAnnotation(RequestMapping.class).value()[0];
    }

    @BeforeEach
    public void setUp() {
        RestAssured.port = localPort;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        final RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder();
        requestSpecBuilder.setBasePath(basePath);
        RestAssured.requestSpecification = requestSpecBuilder.build();
        repository.deleteAll();
    }

    @AfterEach
    public void tearDown() {
        repository.deleteAll();
        RestAssured.reset();
    }

    @Nested
    class RetrieveAllTests {

        @Test
        public void shouldReturnZero() {
            repository.deleteAll();
            //@formatter:off
            final List<SpexCategoryDto> result =
                    given()
                            .contentType(ContentType.JSON)
                            .when()
                            .get()
                            .then()
                            .statusCode(HttpStatus.OK.value())
                            .extract().body()
                            .jsonPath().getList("_embedded.spexCategories", SpexCategoryDto.class);
            //@formatter:on
            assertThat(result, empty());
        }

    }

}
