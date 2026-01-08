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
import jakarta.ws.rs.core.Response;
import nu.fgv.register.server.acl.PermissionService;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
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
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_ADMIN_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_EDITOR_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_USER_SID;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext
@Import({AbstractIntegrationTest.TestConfig.class})
@ActiveProfiles("integrationtest")
@DisabledInAotMode
public abstract class AbstractIntegrationTest {

    private static final String TEST_REALM = "fgv";
    private static final String TEST_GRANT_TYPE = "password";
    private static final String TEST_DOMAIN = "@spexregister.com";
    private static final String TEST_PASSWORD = "s3cr3t";
    protected static final String TEST_ADMIN = "admin" + TEST_DOMAIN;
    protected static final String TEST_EDITOR = "editor" + TEST_DOMAIN;
    protected static final String TEST_USER = "user" + TEST_DOMAIN;
    protected static final PrincipalSid TEST_ADMIN_SID = new PrincipalSid(TEST_ADMIN);
    protected static final PrincipalSid TEST_EDITOR_SID = new PrincipalSid(TEST_EDITOR);
    protected static final PrincipalSid TEST_USER_SID = new PrincipalSid(TEST_USER);
    protected static final Authentication TEST_AUTH = new TestingAuthenticationToken("whoever", "ignored", "ROLE_ADMIN");
    protected static final String AUTHORITY_ADMIN = "ADMIN";
    protected static final String AUTHORITY_EDITOR = "EDITOR";
    protected static final String AUTHORITY_USER = "USER";
    protected static final List<String> AUTHORITIES = List.of(AUTHORITY_ADMIN, AUTHORITY_EDITOR, AUTHORITY_USER);

    private static URI authorizationURI;

    protected final JdbcClient jdbcClient;
    protected final Keycloak keycloakAdminClient;
    protected final String keycloakClientId;
    protected final PermissionService permissionService;
    protected final ObjectMapper objectMapper;
    protected RestTestClient restTestClient;

    private final AclCache aclCache;

    @Value("${spexregister.keycloak.realm}")
    protected String keycloakRealm;

    @Value("${spexregister.keycloak.client.client-id}")
    protected String keycloakClientClientId;

    @Value("${spexregister.keycloak.client.client-secret}")
    protected String keycloakClientClientSecret;

    @LocalServerPort
    protected int localPort;

    @Container
    @ServiceConnection
    private static final MySQLContainer mysql = new MySQLContainer("mysql:8.0.44");

    /*
    @Container
    private static final OpensearchContainer opensearch;

    static {
        opensearch = new OpensearchContainer(DockerImageName.parse("opensearchproject/opensearch:3.4.0"));
        opensearch.start();
    }
    */

    @Container
    private static final KeycloakContainer keycloak = new KeycloakContainer().withRealmImportFile("/keycloak/fgv.json");

    private final JacksonJsonParser jsonParser = new JacksonJsonParser();

    private final LoadingCache<String, String> accessTokenCache;

    protected AbstractIntegrationTest(final JdbcClient jdbcClient,
                                      final AclCache aclCache,
                                      final Keycloak keycloakAdminClient,
                                      final String keycloakClientId,
                                      final PermissionService permissionService,
                                      final ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.aclCache = aclCache;
        this.keycloakAdminClient = keycloakAdminClient;
        this.keycloakClientId = keycloakClientId;
        this.permissionService = permissionService;
        this.objectMapper = objectMapper;

        accessTokenCache = CacheBuilder.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    @Override
                    public @NotNull String load(@NotNull final String key) {
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
                formData.put("client_id", Collections.singletonList(keycloakClientClientId));
                formData.put("client_secret", Collections.singletonList(keycloakClientClientSecret));
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

    protected void revokePermission(final ObjectIdentity oid, final Sid sid, final Permission permission) {
        permissionService.revokePermission(oid, sid, permission);
    }

    protected void grantReadPermissionToUser(final ObjectIdentity oid) {
        grantPermission(oid, TEST_USER_SID, BasePermission.READ);
    }

    protected void grantReadPermissionToRoleUser(final ObjectIdentity oid) {
        grantPermission(oid, ROLE_USER_SID, BasePermission.READ);
    }

    protected void grantReadPermissionToRoleEditor(final ObjectIdentity oid) {
        grantPermission(oid, ROLE_EDITOR_SID, BasePermission.READ);
    }

    protected void grantReadPermissionToRoleAdmin(final ObjectIdentity oid) {
        grantPermission(oid, ROLE_ADMIN_SID, BasePermission.READ);
    }

    protected void grantWritePermissionToRoleAdmin(final ObjectIdentity oid) {
        grantPermission(oid, ROLE_ADMIN_SID, BasePermission.WRITE);
    }

    protected void grantDeletePermissionToRoleAdmin(final ObjectIdentity oid) {
        grantPermission(oid, ROLE_ADMIN_SID, BasePermission.DELETE);
    }

    protected void grantAdministrationPermissionToRoleAdmin(final ObjectIdentity oid) {
        grantPermission(oid, ROLE_ADMIN_SID, BasePermission.ADMINISTRATION);
        grantPermission(oid, ROLE_ADMIN_SID, BasePermission.READ);
    }

    protected void revokeWritePermissionFromRoleAdmin(final ObjectIdentity oid) {
        revokePermission(oid, ROLE_ADMIN_SID, BasePermission.WRITE);
    }
}
