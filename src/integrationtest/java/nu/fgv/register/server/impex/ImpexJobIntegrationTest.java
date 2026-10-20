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

import nu.fgv.register.server.impex.exporting.excel.ExcelWriter;
import nu.fgv.register.server.impex.model.ImpexAction;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.tag.TagImpexDto;
import nu.fgv.register.server.tag.TagRepository;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.ApiVersionInserter;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class ImpexJobIntegrationTest extends AbstractIntegrationTest {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() {
    };

    private static final Locale LOCALE = Locale.forLanguageTag("sv");

    private final TagRepository tagRepository;
    private final MessageSource messageSource;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public ImpexJobIntegrationTest(final JdbcClient jdbcClient,
                                   final Keycloak keycloakAdminClient,
                                   final String keycloakClientId,
                                   final ObjectMapper objectMapper,
                                   final TagRepository tagRepository,
                                   final MessageSource messageSource) {
        super(jdbcClient, keycloakAdminClient, keycloakClientId, objectMapper);
        this.tagRepository = tagRepository;
        this.messageSource = messageSource;
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

    @Test
    void should_import_a_workbook_as_one_import_revision() throws Exception {
        final Tag existing = persistTag("Founder");

        final String token = obtainAdminAccessToken();
        final Long jobId = startTagImport(token, workbook(
                tag(ImpexAction.UPDATE, existing.getId(), "Founder and pillar"),
                tag(ImpexAction.CREATE, null, "Honorary member")
        ));

        assertThat(awaitJob(token, jobId)).isEqualTo("COMPLETED");

        final Map<String, Object> job = job(token, jobId);

        assertThat(job.get("exitStatus")).isEqualTo("COMPLETED");
        assertThat(job.get("importResult")).asInstanceOf(MAP).containsEntry("success", true);
        assertThat(jdbcClient.sql("SELECT name FROM tag ORDER BY name").query(String.class).list())
                .containsExactly("Founder and pillar", "Honorary member");
        assertThat(jdbcClient.sql("""
                        SELECT DISTINCT CONCAT(r.source, ' ', r.comment)
                        FROM revinfo r JOIN tag_audit a ON a.rev = r.id
                        WHERE a.name IN ('Founder and pillar', 'Honorary member')
                        """).query(String.class).list())
                .containsExactly("IMPORT Import job %d (EXCEL)".formatted(jobId));
    }

    @Test
    void should_reject_an_invalid_workbook_without_changing_anything() throws Exception {
        persistTag("Founder");

        final String token = obtainAdminAccessToken();
        final Long jobId = startTagImport(token, workbook(
                tag(ImpexAction.CREATE, null, "Honorary member"),
                tag(ImpexAction.CREATE, null, "")
        ));

        awaitJob(token, jobId);

        final Map<String, Object> job = job(token, jobId);

        assertThat(job.get("exitStatus")).isEqualTo("FAILED");
        assertThat(job.get("importResult")).asInstanceOf(MAP)
                .containsEntry("success", false)
                .extractingByKey("messages").asInstanceOf(LIST).isNotEmpty();
        assertThat(jdbcClient.sql("SELECT name FROM tag").query(String.class).list()).containsExactly("Founder");
    }

    private Tag persistTag(final String name) {
        final Tag tag = new Tag();

        tag.setName(name);

        final Tag saved = tagRepository.save(tag);

        return saved;
    }

    private Long startTagImport(final String token, final byte[] file) {
        dropTestAuthenticationFromReactorContext();

        final Map<String, Object> reference = restTestClient
                .post()
                .uri("/tags?type=excel")
                .header(HttpHeaders.AUTHORIZATION, token)
                .header(HttpHeaders.ACCEPT_LANGUAGE, LOCALE.toLanguageTag())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .apiVersion("1.0")
                .body(file)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(JSON)
                .returnResult()
                .getResponseBody();

        return ((Number) Objects.requireNonNull(reference).get("id")).longValue();
    }

    private Map<String, Object> job(final String token, final Long jobId) {
        return Objects.requireNonNull(restTestClient
                .get()
                .uri("/jobs/{id}", jobId)
                .header(HttpHeaders.AUTHORIZATION, token)
                .apiVersion("1.0")
                .exchange()
                .expectStatus().isOk()
                .expectBody(JSON)
                .returnResult()
                .getResponseBody());
    }

    private byte[] workbook(final TagImpexDto... tags) throws IOException {
        try (final XSSFWorkbook workbook = new XSSFWorkbook();
             final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            new ExcelWriter().createSheet(messageSource, LOCALE, workbook, List.of(tags), null, TagImpexDto.class, false);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static TagImpexDto tag(final ImpexAction action, final Long id, final String name) {
        final TagImpexDto dto = new TagImpexDto();

        dto.setAction(action);
        dto.setId(id);
        dto.setName(name);
        return dto;
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
