package nu.fgv.register.server.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import jakarta.ws.rs.core.Response;
import nu.fgv.register.server.acl.PermissionService;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.aot.DisabledInAotMode;
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

import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_USER_SID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext
@Import({AbstractIntegrationTest.TestConfig.class})
@ActiveProfiles("test")
@DisabledInAotMode
public abstract class AbstractIntegrationTest {

    private static final String TEST_REALM = "fgv";
    private static final String TEST_GRANT_TYPE = "password";
    private static final String TEST_CLIENT_ID = "spexregister";
    private static final String TEST_DOMAIN = "@spexregister.com";
    private static final String TEST_PASSWORD = "s3cr3t";
    protected static final String TEST_ADMIN = "admin" + TEST_DOMAIN;
    protected static final String TEST_EDITOR = "editor" + TEST_DOMAIN;
    protected static final String TEST_USER = "user" + TEST_DOMAIN;
    protected static final PrincipalSid TEST_ADMIN_SID = new PrincipalSid(TEST_ADMIN);
    protected static final PrincipalSid TEST_EDITOR_SID = new PrincipalSid(TEST_EDITOR);
    protected static final PrincipalSid TEST_USER_SID = new PrincipalSid(TEST_USER);
    protected final Authentication TEST_AUTH = new TestingAuthenticationToken("whoever", "ignored", "ROLE_ADMINISTRATOR");

    private static URI authorizationURI;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private AclCache aclCache;

    @Autowired
    protected Keycloak keycloakAdminClient;

    @Autowired
    protected String keycloakClientId;

    @Value("${spexregister.keycloak.realm}")
    protected String keycloakRealm;

    @Value("${spexregister.keycloak.client.client-id}")
    protected String keycloakClientClientId;

    @Autowired
    protected PermissionService permissionService;

    @Container
    @ServiceConnection
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.36");

    /*
    @Container
    private static final OpensearchContainer opensearch;

    static {
        opensearch = new OpensearchContainer(DockerImageName.parse("opensearchproject/opensearch:2.0.1"));
        opensearch.start();
    }
    */

    @Container
    private static final KeycloakContainer keycloak = new KeycloakContainer().withRealmImportFile("/keycloak/fgv.json");

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
        registry.add("spexregister.keycloak.url", keycloak::getAuthServerUrl);
        authorizationURI = new URIBuilder(keycloak.getAuthServerUrl() + String.format("/realms/%s/protocol/openid-connect/token", TEST_REALM)).build();
    }

    @BeforeEach
    public void baseSetUp() {
        SecurityContextHolder.getContext().setAuthentication(TEST_AUTH); // Needed when manually granting permissions
    }

    @AfterEach
    public void baseTearDown() {
        jdbcClient.sql("DELETE FROM acl_entry").update();
        jdbcClient.sql("DELETE FROM acl_object_identity").update();
        jdbcClient.sql("DELETE FROM acl_class").update();
        jdbcClient.sql("DELETE FROM acl_sid").update();
        SecurityContextHolder.clearContext();
        aclCache.clearCache();
        keycloakAdminClient
                .realm(keycloakRealm)
                .users()
                .list()
                .stream()
                .filter(u -> !u.getEmail().contains(TEST_DOMAIN))
                .forEach(u -> {
                    try (final Response ignored = keycloakAdminClient.realm(keycloakRealm).users().delete(u.getId())) {
                        // Ignored
                    }
                });
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
        return obtainAccessToken(TEST_USER, TEST_PASSWORD);
    }

    protected String obtainEditorAccessToken() {
        return obtainAccessToken(TEST_EDITOR, TEST_PASSWORD);
    }

    protected String obtainAdminAccessToken() {
        return obtainAccessToken(TEST_ADMIN, TEST_PASSWORD);
    }

    protected void grantPermission(final ObjectIdentity oid, final Sid sid, final Permission permission) {
        permissionService.grantPermission(oid, sid, permission);
    }

    protected void grantReadPermissionToUser(final ObjectIdentity oid) {
        grantPermission(oid, TEST_USER_SID, BasePermission.READ);
    }

    protected void grantReadPermissionToRoleUser(final ObjectIdentity oid) {
        grantPermission(oid, ROLE_USER_SID, BasePermission.READ);
    }

}
