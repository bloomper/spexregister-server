/*
 * Copyright 2026 the original author or authors.
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

package nu.fgv.register.server.config;

import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class ActuatorIntegrationTest extends AbstractIntegrationTest {

    @LocalManagementPort
    private int localManagementPort;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public ActuatorIntegrationTest(final JdbcClient jdbcClient,
                                   final AclCache aclCache,
                                   final Keycloak keycloakAdminClient,
                                   final String keycloakClientId,
                                   final PermissionService permissionService,
                                   final ObjectMapper objectMapper) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
    }

    @BeforeEach
    void setUp() {
        restTestClient = RestTestClient
                .bindToServer()
                .baseUrl("http://localhost:%s/actuator".formatted(localManagementPort))
                .build();
    }

    @Test
    @DisplayName("Should return health without authentication")
    void shouldReturnHealthWithoutAuthentication() {
        restTestClient
                .get()
                .uri("/health")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    @DisplayName("Should require authentication for metrics")
    void shouldRequireAuthenticationForMetrics() {
        restTestClient
                .get()
                .uri("/metrics")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
