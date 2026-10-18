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

package nu.fgv.register.server.impex;

import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.tag.TagRepository;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.ApiVersionInserter;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class ExportJobIntegrationTest extends AbstractIntegrationTest {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() {
    };

    private final TagRepository tagRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public ExportJobIntegrationTest(final JdbcClient jdbcClient,
                                    final AclCache aclCache,
                                    final Keycloak keycloakAdminClient,
                                    final String keycloakClientId,
                                    final PermissionService permissionService,
                                    final ObjectMapper objectMapper,
                                    final TagRepository tagRepository) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.tagRepository = tagRepository;
    }

    @BeforeEach
    void setUp() {
        restTestClient = RestTestClient
                .bindToServer()
                .baseUrl("http://localhost:%s/api".formatted(localPort))
                .apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
                .build();

        JdbcTestUtils.deleteFromTables(jdbcClient, "tag", "tag_audit");
    }

    @Test
    void should_export_to_a_downloadable_workbook() throws Exception {
        persistTag("Founder");
        persistTag("Honorary member");

        final String token = obtainAdminAccessToken();
        final Long jobId = startTagExport(token);

        assertThat(awaitJob(token, jobId)).isEqualTo("COMPLETED");

        final byte[] file = restTestClient
                .get()
                .uri("/jobs/{id}/results", jobId)
                .header(HttpHeaders.AUTHORIZATION, token)
                .apiVersion("1.0")
                .exchange()
                .expectStatus().isOk()
                .expectBody(byte[].class)
                .returnResult()
                .getResponseBody();

        try (final XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(Objects.requireNonNull(file)))) {
            assertThat(cellsInColumn(workbook.getSheetAt(0), 2)).containsExactly("Founder", "Honorary member");
        }
    }

    @Test
    void should_not_expose_a_job_to_anyone_but_its_requestor() throws Exception {
        persistTag("Founder");

        final String adminToken = obtainAdminAccessToken();
        final Long jobId = startTagExport(adminToken);

        awaitJob(adminToken, jobId);

        restTestClient
                .get()
                .uri("/jobs/{id}/results", jobId)
                .header(HttpHeaders.AUTHORIZATION, obtainEditorAccessToken())
                .apiVersion("1.0")
                .exchange()
                .expectStatus().isForbidden();
    }

    private void persistTag(final String name) {
        final Tag tag = new Tag();

        tag.setName(name);

        final Tag saved = tagRepository.save(tag);

        grantReadPermissionToRoleAdmin(toObjectIdentity(Tag.class, saved.getId()));
        grantReadPermissionToRoleEditor(toObjectIdentity(Tag.class, saved.getId()));
    }

    private Long startTagExport(final String token) {
        dropTestAuthenticationFromReactorContext();

        final Map<String, Object> reference = restTestClient
                .get()
                .uri("/tags?type=excel")
                .header(HttpHeaders.AUTHORIZATION, token)
                .apiVersion("1.0")
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(JSON)
                .returnResult()
                .getResponseBody();

        return ((Number) Objects.requireNonNull(reference).get("id")).longValue();
    }

    private String awaitJob(final String token, final Long jobId) throws InterruptedException {
        final Instant deadline = Instant.now().plus(Duration.ofSeconds(30));

        while (true) {
            final Map<String, Object> body = restTestClient
                    .get()
                    .uri("/jobs/{id}/status", jobId)
                    .header(HttpHeaders.AUTHORIZATION, token)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(JSON)
                    .returnResult()
                    .getResponseBody();
            final String status = (String) Objects.requireNonNull(body).get("status");

            if (List.of("COMPLETED", "FAILED", "STOPPED", "ABANDONED").contains(status)) {
                return status;
            }
            assertThat(Instant.now()).as("job %d did not finish in time", jobId).isBefore(deadline);
            Thread.sleep(200);
        }
    }

    private static void dropTestAuthenticationFromReactorContext() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    private static List<String> cellsInColumn(final Sheet sheet, final int column) {
        final List<String> values = new ArrayList<>();

        for (final Row row : sheet) {
            if (row.getRowNum() > 0) {
                values.add(row.getCell(column).getStringCellValue());
            }
        }
        return values;
    }
}
