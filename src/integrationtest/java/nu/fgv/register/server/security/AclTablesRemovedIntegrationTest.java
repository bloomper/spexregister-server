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

package nu.fgv.register.server.security;

import nu.fgv.register.server.util.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The per-record access lists are gone once the database is migrated; access is decided by the policies.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
class AclTablesRemovedIntegrationTest extends AbstractIntegrationTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public AclTablesRemovedIntegrationTest(final JdbcClient jdbcClient,
                                           final Keycloak keycloakAdminClient,
                                           final String keycloakClientId,
                                           final ObjectMapper objectMapper) {
        super(jdbcClient, keycloakAdminClient, keycloakClientId, objectMapper);
    }

    @Test
    void should_have_no_access_list_tables() {
        final var tables = jdbcClient
                .sql("SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name LIKE 'acl\\\\_%'")
                .query(String.class)
                .list();

        assertThat(tables).isEmpty();
    }

    @Test
    void should_have_the_permission_grant_table() {
        final var tables = jdbcClient
                .sql("SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name IN ('permission_grant', 'permission_grant_audit')")
                .query(String.class)
                .list();

        assertThat(tables).containsExactlyInAnyOrder("permission_grant", "permission_grant_audit");
    }
}
