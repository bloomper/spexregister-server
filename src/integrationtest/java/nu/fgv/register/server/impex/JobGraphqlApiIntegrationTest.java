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

import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.tag.TagRepository;
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.ApiVersionInserter;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The job queries and mutation over GraphQL, which find the caller through Reactor's context.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
class JobGraphqlApiIntegrationTest extends AbstractGraphqlIntegrationTest {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() {
    };

    private final TagRepository tagRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public JobGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
                                        final Keycloak keycloakAdminClient,
                                        final String keycloakClientId,
                                        final ObjectMapper objectMapper,
                                        final TagRepository tagRepository) {
        super(jdbcClient, keycloakAdminClient, keycloakClientId, objectMapper);
        this.tagRepository = tagRepository;
    }

    @BeforeEach
    void setUp() {
        httpGraphQlTester = HttpGraphQlTester.create(
                WebTestClient.bindToServer()
                        .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath))
                        .build()
        );
        restTestClient = RestTestClient
                .bindToServer()
                .baseUrl("http://localhost:%s/api".formatted(localPort))
                .apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
                .build();

        JdbcTestUtils.deleteFromTables(jdbcClient, "tag", "tag_audit");

        final Tag tag = new Tag();

        tag.setName("Founder");
        tagRepository.save(tag);
    }

    private GraphQlTester as(final String token) {
        return httpGraphQlTester
                .mutate()
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, token))
                .build();
    }

    private Long startTagExport(final String token) {
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
            final String status = as(token)
                    .documentName("job/jobStatus")
                    .variable("id", jobId)
                    .execute()
                    .errors()
                    .verify()
                    .path("jobStatus.status")
                    .entity(String.class)
                    .get();

            if (List.of("COMPLETED", "FAILED", "STOPPED", "ABANDONED").contains(status)) {
                return status;
            }
            assertThat(Instant.now()).as("job %d did not finish in time", jobId).isBefore(deadline);
            Thread.sleep(200);
        }
    }

    private void expectError(final GraphQlTester.Response response, final ErrorType type) {
        response
                .errors()
                .satisfy(errors -> assertThat(errors)
                        .anyMatch(error -> String.valueOf(error.getExtensions().get("classification")).equals(type.toString())));
    }

    @Test
    void should_give_the_requestor_their_job() throws Exception {
        final String admin = obtainAdminAccessToken();
        final Long jobId = startTagExport(admin);

        assertThat(awaitJob(admin, jobId)).isEqualTo("COMPLETED");

        as(admin)
                .documentName("job/job")
                .variable("id", jobId)
                .execute()
                .errors()
                .verify()
                .path("job.id").entity(String.class).isEqualTo(String.valueOf(jobId))
                .path("job.exitStatus").entity(String.class).isEqualTo("COMPLETED")
                .path("job.hasDownload").entity(Boolean.class).isEqualTo(true);

        as(admin)
                .documentName("job/jobs")
                .execute()
                .errors()
                .verify()
                .path("jobs[*].id").entityList(String.class).contains(String.valueOf(jobId));
    }

    @Test
    void should_delete_a_finished_job() throws Exception {
        final String admin = obtainAdminAccessToken();
        final Long jobId = startTagExport(admin);

        awaitJob(admin, jobId);

        as(admin)
                .documentName("job/jobDelete")
                .variable("id", jobId)
                .execute()
                .errors()
                .verify();

        expectError(as(admin).documentName("job/job").variable("id", jobId).execute(), ErrorType.NOT_FOUND);
        as(admin)
                .documentName("job/jobs")
                .execute()
                .errors()
                .verify()
                .path("jobs[*].id").entityList(String.class).doesNotContain(String.valueOf(jobId));
    }

    @Test
    void should_never_give_anyone_else_the_job() throws Exception {
        final String admin = obtainAdminAccessToken();
        final String editor = obtainEditorAccessToken();
        final Long jobId = startTagExport(admin);

        awaitJob(admin, jobId);

        expectError(as(editor).documentName("job/jobStatus").variable("id", jobId).execute(), ErrorType.FORBIDDEN);
        expectError(as(editor).documentName("job/job").variable("id", jobId).execute(), ErrorType.FORBIDDEN);
        expectError(as(editor).documentName("job/jobDelete").variable("id", jobId).execute(), ErrorType.FORBIDDEN);
        as(editor)
                .documentName("job/jobs")
                .execute()
                .errors()
                .verify()
                .path("jobs[*].id").entityList(String.class).doesNotContain(String.valueOf(jobId));

        assertThat(awaitJob(admin, jobId)).isEqualTo("COMPLETED");
    }
}
