package nu.fgv.register.server.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext
@Import({AbstractIntegrationTest.TestConfig.class})
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final String TEST_REALM = "spexregister";
    private static final String TEST_GRANT_TYPE = "password";
    private static final String TEST_CLIENT_ID = "spexregister";
    private static final String TEST_DOMAIN = "@spexregister.com";
    private static final String TEST_PASSWORD = "s3cr3t";

    private static URI authorizationURI;

    @Container
    @ServiceConnection
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.33");

    /*
    @Container
    private static final OpensearchContainer opensearch;

    static {
        opensearch = new OpensearchContainer(DockerImageName.parse("opensearchproject/opensearch:2.0.0"));
        opensearch.start();
    }
    */

    @Container
    private static final KeycloakContainer keycloak = new KeycloakContainer().withRealmImportFile("/keycloak/spexregister.json");

    private final JacksonJsonParser jsonParser = new JacksonJsonParser();

    private final LoadingCache<String, String> accessTokenCache;

    protected AbstractIntegrationTest() {
        accessTokenCache = CacheBuilder.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    @Override
                    public @NotNull String load(@NotNull String key) {
                        return key.toUpperCase();
                    }
                });
    }

    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) throws URISyntaxException {
        //registry.add("spring.jpa.properties.hibernate.search.backend.hosts", () -> String.format("%s:%s", opensearch.getHost(), opensearch.getMappedPort(9200)));
        //registry.add("spring.jpa.properties.hibernate.search.backend.username", opensearch::getUsername);
        //registry.add("spring.jpa.properties.hibernate.search.backend.password", opensearch::getPassword);

        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> keycloak.getAuthServerUrl() + "/realms/" + TEST_REALM);
        authorizationURI = new URIBuilder(keycloak.getAuthServerUrl() + String.format("/realms/%s/protocol/openid-connect/token", TEST_REALM)).build();
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public AuditorAware<String> auditorAware() {
            return () -> Optional.of("dummy");
        }
    }

    protected String obtainAccessToken(final String username, final String password) {
        try {
            return accessTokenCache.get(username, () -> {
                final WebClient webClient = WebClient.builder().build();
                final MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

                formData.put("grant_type", Collections.singletonList(TEST_GRANT_TYPE));
                formData.put("client_id", Collections.singletonList(TEST_CLIENT_ID));
                formData.put("username", Collections.singletonList(username));
                formData.put("password", Collections.singletonList(password));

                final String response = webClient
                        .post()
                        .uri(authorizationURI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(BodyInserters.fromFormData(formData))
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                return "Bearer " + jsonParser.parseMap(response)
                        .get("access_token")
                        .toString();
            });
        } catch (final Exception e) {
            throw new RuntimeException("Could not obtain access token for username " + username, e);
        }
    }

    protected String obtainUserAccessToken() {
        return obtainAccessToken("user" + TEST_DOMAIN, TEST_PASSWORD);
    }

    protected String obtainEditorAccessToken() {
        return obtainAccessToken("editor" + TEST_DOMAIN, TEST_PASSWORD);
    }

    protected String obtainAdminAccessToken() {
        return obtainAccessToken("admin" + TEST_DOMAIN, TEST_PASSWORD);
    }

}
