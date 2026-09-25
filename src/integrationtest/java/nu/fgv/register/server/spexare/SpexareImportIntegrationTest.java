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

package nu.fgv.register.server.spexare;

import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.impex.model.ImpexAction;
import nu.fgv.register.server.impex.model.ImportResultDto;
import nu.fgv.register.server.spexare.address.AddressImpexDto;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static nu.fgv.register.server.util.security.SecurityUtil.runAsSystem;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class SpexareImportIntegrationTest extends AbstractIntegrationTest {

    private final SpexareImportService importService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public SpexareImportIntegrationTest(final JdbcClient jdbcClient,
                                        final AclCache aclCache,
                                        final Keycloak keycloakAdminClient,
                                        final String keycloakClientId,
                                        final PermissionService permissionService,
                                        final ObjectMapper objectMapper,
                                        final SpexareImportService importService) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.importService = importService;
    }

    @BeforeEach
    void setUp() {
        jdbcClient.sql("UPDATE spexare SET partner_id = NULL").update();
        JdbcTestUtils.deleteFromTables(jdbcClient, "address", "address_audit", "spexare", "spexare_audit");
    }

    @Test
    void should_roll_back_a_spexare_with_a_failing_row_and_keep_the_others() {
        final AtomicReference<ImportResultDto> result = new AtomicReference<>();

        runAsSystem(() -> result.set(importService.processImport(Map.of(
                SpexareImpexDto.class, List.of(spexare(-1L, "Ada", 2), spexare(-2L, "Bo", 3)),
                AddressImpexDto.class, List.of(address(-1L, 2), address(-1L, 3), address(-2L, 4))
        ), Locale.ENGLISH)));

        assertThat(result.get().isSuccess()).isFalse();
        assertThat(result.get().getErrors()).singleElement().asString()
                .contains("row 3")
                .contains("no changes were saved for this spexare");
        assertThat(jdbcClient.sql("SELECT first_name FROM spexare").query(String.class).list()).containsExactly("Bo");
        assertThat(JdbcTestUtils.countRowsInTable(jdbcClient, "address")).isEqualTo(1);
    }

    private static SpexareImpexDto spexare(final Long id, final String firstName, final int rowNumber) {
        final SpexareImpexDto dto = new SpexareImpexDto();

        dto.setAction(ImpexAction.CREATE);
        dto.setRowNumber(rowNumber);
        dto.setId(id);
        dto.setFirstName(firstName);
        dto.setLastName("Importsson");
        dto.setDeceased(false);
        dto.setPublished(true);
        return dto;
    }

    private static AddressImpexDto address(final Long spexareId, final int rowNumber) {
        final AddressImpexDto dto = new AddressImpexDto();

        dto.setAction(ImpexAction.CREATE);
        dto.setRowNumber(rowNumber);
        dto.setSpexareId(spexareId);
        dto.setTypeId("HOME");
        dto.setStreetAddress("Gatan %d".formatted(rowNumber));
        return dto;
    }
}
