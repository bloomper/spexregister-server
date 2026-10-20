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

package nu.fgv.register.server.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import jakarta.persistence.EntityManagerFactory;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.utils.URIBuilder;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.migration.JavaMigration;
import org.hibernate.search.mapper.orm.Search;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({AbstractIntegrationTest.TestConfig.class})
@ActiveProfiles("integrationtest")
@DisabledInAotMode
public abstract class AbstractIntegrationTest {

    protected static final Authentication TEST_AUTH = new TestingAuthenticationToken("whoever", "ignored", "ROLE_ADMIN");
    protected static final String AUTHORITY_ADMIN = "ADMIN";
    protected static final String AUTHORITY_EDITOR = "EDITOR";
    protected static final String AUTHORITY_USER = "USER";
    protected static final List<String> AUTHORITIES = List.of(AUTHORITY_ADMIN, AUTHORITY_EDITOR, AUTHORITY_USER);
    private static final String TEST_REALM = "fgv";
    private static final String TEST_GRANT_TYPE = "password";
    private static final String TEST_DOMAIN = "@fgv.nu";
    protected static final String TEST_ADMIN = "admin" + TEST_DOMAIN;
    protected static final String TEST_EDITOR = "editor" + TEST_DOMAIN;
    protected static final String TEST_USER = "user" + TEST_DOMAIN;
    private static final String TEST_PASSWORD = "s3cr3t-s3cr3t";
    private static final String TEST_CLIENT_ID = "spexregister";
    protected static final String TEST_MCP_CLIENT_ID = "spexregister-mcp";
    private static final String TEST_UNBOUND_MCP_CLIENT_ID = "spexregister-mcp-unbound";
    @ServiceConnection
    private static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.11")
            .withTmpFs(Map.of("/var/lib/mysql", "rw"))
            .withCommand("mysqld",
                    "--innodb-flush-log-at-trx-commit=0",
                    "--innodb-doublewrite=0",
                    "--skip-log-bin",
                    "--performance-schema=OFF");
    private static final KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.8").withRealmImportFile("/keycloak/fgv.json");
    private static final Set<String> RESET_TEST_CLASSES = ConcurrentHashMap.newKeySet();
    private static final JacksonJsonParser jsonParser = new JacksonJsonParser();
    private static final LoadingCache<String, String> accessTokenCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(2))
            .build(new CacheLoader<>() {
                @Override
                public @NotNull String load(@NotNull final String key) {
                    return key.toUpperCase();
                }
            });
    private static URI authorizationURI;

    static {
        Startables.deepStart(mysql, keycloak).join();
        prepareKeycloak();
    }

    protected final JdbcClient jdbcClient;
    protected final Keycloak keycloakAdminClient;
    protected final String keycloakClientId;
    protected final ObjectMapper objectMapper;
    protected RestTestClient restTestClient;
    @Value("${spexregister.keycloak.realm}")
    protected String keycloakRealm;
    @Value("${spexregister.keycloak.client.client-id}")
    protected String keycloakClientClientId;
    @Value("${spexregister.keycloak.client.client-secret}")
    protected String keycloakClientClientSecret;

    /*
    @Container
    private static final OpensearchContainer opensearch;

    static {
        opensearch = new OpensearchContainer(DockerImageName.parse("opensearchproject/opensearch:3.7.0"));
        opensearch.start();
    }
    */
    @LocalServerPort
    protected int localPort;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    protected AbstractIntegrationTest(final JdbcClient jdbcClient,
                                      final Keycloak keycloakAdminClient,
                                      final String keycloakClientId,
                                      final ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.keycloakAdminClient = keycloakAdminClient;
        this.keycloakClientId = keycloakClientId;
        this.objectMapper = objectMapper;
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

    private static void prepareKeycloak() {
        try (final Keycloak admin = keycloak.getKeycloakAdminClient()) {
            final RealmResource realm = admin.realm(TEST_REALM);
            final ClientRepresentation mcpClient = realm.clients().findByClientId(TEST_MCP_CLIENT_ID).getFirst();
            final ClientRepresentation webClient = realm.clients().findByClientId(TEST_CLIENT_ID).getFirst();
            final ClientResource client = realm.clients().get(webClient.getId());

            mcpClient.setDirectAccessGrantsEnabled(true);
            mcpClient.setConsentRequired(false);
            realm.clients().get(mcpClient.getId()).update(mcpClient);
            webClient.setDirectAccessGrantsEnabled(true);
            client.update(webClient);

            final ClientRepresentation unboundMcpClient = new ClientRepresentation();
            unboundMcpClient.setClientId(TEST_UNBOUND_MCP_CLIENT_ID);
            unboundMcpClient.setPublicClient(true);
            unboundMcpClient.setDirectAccessGrantsEnabled(true);
            realm.clients().create(unboundMcpClient).close();

            createTestUser(realm, client, TEST_ADMIN, List.of(AUTHORITY_USER, AUTHORITY_EDITOR, AUTHORITY_ADMIN));
            createTestUser(realm, client, TEST_EDITOR, List.of(AUTHORITY_USER, AUTHORITY_EDITOR));
            createTestUser(realm, client, TEST_USER, List.of(AUTHORITY_USER));
        }
    }

    private static void createTestUser(final RealmResource realm, final ClientResource client, final String email, final List<String> authorities) {
        final CredentialRepresentation credential = new CredentialRepresentation();
        final UserRepresentation user = new UserRepresentation();

        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(TEST_PASSWORD);
        credential.setTemporary(false);
        user.setEmail(email);
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setCredentials(List.of(credential));

        try (final Response response = realm.users().create(user)) {
            final String locationPath = response.getLocation().getPath();

            realm.users()
                    .get(locationPath.substring(locationPath.lastIndexOf('/') + 1))
                    .roles()
                    .clientLevel(client.toRepresentation().getId())
                    .add(authorities.stream().map(authority -> client.roles().get(authority).toRepresentation()).toList());
        }
    }

    private static Class<?> topLevelClass(final Class<?> clazz) {
        Class<?> current = clazz;

        while (current.getEnclosingClass() != null) {
            current = current.getEnclosingClass();
        }
        return current;
    }

    @BeforeEach
    public void baseSetUp() {
        resetStateOncePerTestClass();
        SecurityContextHolder.getContext().setAuthentication(TEST_AUTH);
    }

    private void resetStateOncePerTestClass() {
        if (!RESET_TEST_CLASSES.add(topLevelClass(getClass()).getName())) {
            return;
        }
        final Flyway flyway = flyway();

        flyway.clean();
        flyway.migrate();
        Search.mapping(entityManagerFactory).scope(Object.class).workspace().purge();
        cacheManager.getCacheNames().forEach(name -> Objects.requireNonNull(cacheManager.getCache(name)).clear());
    }

    private Flyway flyway() {
        return Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .installedBy("system")
                .executeInTransaction(false)
                .cleanDisabled(false)
                .javaMigrations(applicationContext.getBeansOfType(JavaMigration.class).values().toArray(new JavaMigration[0]))
                .callbacks(applicationContext.getBeansOfType(Callback.class).values().toArray(new Callback[0]))
                .load();
    }

    @AfterEach
    public void baseTearDown() {
        SecurityContextHolder.clearContext();
        keycloakAdminClient
                .realm(keycloakRealm)
                .users()
                .list()
                .stream()
                .filter(u -> !u.getEmail().contains(TEST_DOMAIN))
                .forEach(u -> keycloakAdminClient.realm(keycloakRealm).users().delete(u.getId()).close());
    }

    protected String obtainAccessToken(final String username, final String password) {
        return obtainAccessToken(username, password, keycloakClientClientId);
    }

    protected String obtainAccessToken(final String username, final String password, final String clientId) {
        try {
            return accessTokenCache.get("%s:%s".formatted(clientId, username), () -> requestToken(username, password, clientId, null, "access_token"));
        } catch (final Exception e) {
            throw new RuntimeException("Could not obtain access token for username " + username, e);
        }
    }

    protected String obtainIdToken(final String username, final String clientId) {
        return requestToken(username, TEST_PASSWORD, clientId, "openid", "id_token");
    }

    private String requestToken(final String username, final String password, final String clientId, final @Nullable String scope, final String tokenField) {
        final WebClient webClient = WebClient.builder().build();
        final MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.put("grant_type", Collections.singletonList(TEST_GRANT_TYPE));
        formData.put("client_id", Collections.singletonList(clientId));
        formData.put("client_secret", Collections.singletonList(keycloakClientClientSecret));
        formData.put("username", Collections.singletonList(username));
        formData.put("password", Collections.singletonList(password));
        if (scope != null) {
            formData.put("scope", Collections.singletonList(scope));
        }

        final String response = webClient
                .post()
                .uri(authorizationURI)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return "Bearer " + jsonParser.parseMap(response)
                .get(tokenField)
                .toString();
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

    protected String obtainMcpAccessToken(final String username) {
        return obtainAccessToken(username, TEST_PASSWORD, TEST_MCP_CLIENT_ID);
    }

    protected String obtainUnboundMcpAccessToken(final String username) {
        return obtainAccessToken(username, TEST_PASSWORD, TEST_UNBOUND_MCP_CLIENT_ID);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public AuditorAware<String> auditorAware() {
            return () -> Optional.of("dummy");
        }

    }
}
