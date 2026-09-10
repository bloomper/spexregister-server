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

package nu.fgv.register.server.audit;

import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.tag.TagRepository;
import nu.fgv.register.server.util.AbstractAuditable;
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.inClass;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class AuditGraphqlApiIntegrationTest extends AbstractGraphqlIntegrationTest {

    private final EasyRandom random;
    private final TagRepository repository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public AuditGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
                                          final AclCache aclCache,
                                          final Keycloak keycloakAdminClient,
                                          final String keycloakClientId,
                                          final PermissionService permissionService,
                                          final TagRepository repository,
                                          final ObjectMapper objectMapper) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.repository = repository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

        parameters
                .excludeField(named("version").and(ofType(Long.class)).and(inClass(AbstractAuditable.class)));
        random = new EasyRandom(parameters);
    }

    @BeforeEach
    void setUp() {
        httpGraphQlTester = HttpGraphQlTester.create(
                WebTestClient.bindToServer()
                        .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath))
                        .build()
        );

        JdbcTestUtils.deleteFromTables(jdbcClient, "tag", "tag_audit", "revchanges", "revinfo");
    }

    @Nested
    @DisplayName("Retrieve revisions")
    class RetrieveRevisionsTests {

        @Test
        void should_return_add_and_mod() {
            final var tag = persistTag();
            grantReadPermissionToRoleUser(toObjectIdentity(Tag.class, tag.getId()));

            tag.setName("renamed");
            repository.save(tag);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("audit/revisions")
                    .variable("type", AuditedType.TAG)
                    .variable("id", tag.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("revisions").entityList(Object.class).hasSize(2)
                    .path("revisions[0].revisionType").entity(String.class).isEqualTo("MOD")
                    .path("revisions[0].changes[0].field").entity(String.class).isEqualTo("name")
                    .path("revisions[0].changes[0].newValue").entity(String.class).isEqualTo("renamed")
                    .path("revisions[1].revisionType").entity(String.class).isEqualTo("ADD");
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted() {
            final var tag = persistTag();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("audit/revisions")
                    .variable("type", AuditedType.TAG)
                    .variable("id", tag.getId())
                    .execute()
                    .errors()
                    .satisfy(errors -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    );
        }
    }

    @Nested
    @DisplayName("Restore")
    class RestoreTests {

        @Test
        void should_restore_as_admin() {
            final var tag = persistTag();
            final var originalName = tag.getName();
            grantReadPermissionToRoleAdmin(toObjectIdentity(Tag.class, tag.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Tag.class, tag.getId()));

            tag.setName("renamed");
            repository.save(tag);

            final Long firstRevision = firstRevisionOf(tag);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("audit/restore")
                    .variable("type", AuditedType.TAG)
                    .variable("id", tag.getId())
                    .variable("revision", firstRevision)
                    .variable("cascade", false)
                    .execute()
                    .errors()
                    .verify()
                    .path("restore.updated").entity(Integer.class).isEqualTo(1);

            assertThat(repository.findById(tag.getId()).orElseThrow().getName()).isEqualTo(originalName);
        }

        @Test
        void should_return_FORBIDDEN_when_editor() {
            final var tag = persistTag();
            grantReadPermissionToRoleEditor(toObjectIdentity(Tag.class, tag.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainEditorAccessToken()))
                    .build()
                    .documentName("audit/restore")
                    .variable("type", AuditedType.TAG)
                    .variable("id", tag.getId())
                    .variable("revision", 1L)
                    .variable("cascade", false)
                    .execute()
                    .errors()
                    .satisfy(errors -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    );
        }
    }

    @Nested
    @DisplayName("Retrieve feed")
    class RetrieveFeedTests {

        @Test
        void should_return_entries_for_admin() {
            persistTag();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("audit/revisionFeedPaged")
                    .variable("first", 10)
                    .variable("type", AuditedType.TAG)
                    .execute()
                    .errors()
                    .verify()
                    .path("revisionFeedPaged.totalCount").entity(Integer.class).isEqualTo(1)
                    .path("revisionFeedPaged.edges[0].node.types").entityList(String.class).containsExactly("TAG");
        }

        @Test
        void should_return_FORBIDDEN_when_not_admin() {
            persistTag();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainEditorAccessToken()))
                    .build()
                    .documentName("audit/revisionFeedPaged")
                    .variable("first", 10)
                    .execute()
                    .errors()
                    .satisfy(errors -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    );
        }
    }

    private Long firstRevisionOf(final Tag tag) {
        final List<Long> revisions = jdbcClient
                .sql("SELECT rev FROM tag_audit WHERE id = :id ORDER BY rev ASC")
                .param("id", tag.getId())
                .query(Long.class)
                .list();

        return revisions.getFirst();
    }

    private Tag persistTag() {
        final Tag tag = random.nextObject(Tag.class);

        tag.setId(null);

        return repository.save(tag);
    }
}