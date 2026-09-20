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
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.function.UnaryOperator;

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

    private Long firstRevisionOf(final Tag tag) {
        final List<Long> revisions = jdbcClient
                .sql("SELECT rev FROM tag_audit WHERE id = :id ORDER BY rev ASC")
                .param("id", tag.getId())
                .query(Long.class)
                .list();

        return revisions.getFirst();
    }

    /**
     * Envers stamps the time itself, so an old revision has to be made old afterwards.
     */
    private void backdateRevision(final Long revision, final LocalDate when) {
        jdbcClient
                .sql("UPDATE revinfo SET modified_at = :modifiedAt WHERE id = :id")
                .param("modifiedAt", when.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .param("id", revision)
                .update();
    }

    private Tag persistTag() {
        final Tag tag = random.nextObject(Tag.class);

        tag.setId(null);

        return repository.save(tag);
    }

    private GraphQlTester.Traversable feedAsAdmin(final UnaryOperator<GraphQlTester.Request<?>> narrow) {
        return narrow.apply(httpGraphQlTester
                        .mutate()
                        .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                        .build()
                        .documentName("audit/revisionFeedPaged")
                        .variable("first", 10))
                .execute()
                .errors()
                .verify();
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
        void should_filter_by_author() {
            persistTag();

            feedAsAdmin(tester -> tester.variable("modifiedBy", List.of("system")))
                    .path("revisionFeedPaged.totalCount").entity(Integer.class).isEqualTo(1);

            feedAsAdmin(tester -> tester.variable("modifiedBy", List.of("nobody@example.com")))
                    .path("revisionFeedPaged.totalCount").entity(Integer.class).isEqualTo(0);
        }

        @Test
        void should_filter_by_source() {
            persistTag();

            feedAsAdmin(tester -> tester.variable("sources", List.of(AuditSource.SYSTEM)))
                    .path("revisionFeedPaged.totalCount").entity(Integer.class).isEqualTo(1);

            feedAsAdmin(tester -> tester.variable("sources", List.of(AuditSource.IMPORT)))
                    .path("revisionFeedPaged.totalCount").entity(Integer.class).isEqualTo(0);
        }

        @Test
        void should_filter_by_date_range() {
            persistTag();

            feedAsAdmin(tester -> tester
                    .variable("from", LocalDate.now().toString())
                    .variable("to", LocalDate.now().toString()))
                    .path("revisionFeedPaged.totalCount").entity(Integer.class).isEqualTo(1);

            feedAsAdmin(tester -> tester.variable("to", LocalDate.now().minusDays(1).toString()))
                    .path("revisionFeedPaged.totalCount").entity(Integer.class).isEqualTo(0);
        }

        @Test
        void should_count_the_default_window_back_from_an_end_date_rather_than_from_today() {
            final var tag = persistTag();
            backdateRevision(firstRevisionOf(tag), LocalDate.now().minusDays(200));

            // Outside the 90 days before today, so the default window alone does not reach it.
            feedAsAdmin(tester -> tester)
                    .path("revisionFeedPaged.totalCount").entity(Integer.class).isEqualTo(0);

            // Naming an end date moves that window rather than leaving an empty intersection.
            feedAsAdmin(tester -> tester.variable("to", LocalDate.now().minusDays(195).toString()))
                    .path("revisionFeedPaged.totalCount").entity(Integer.class).isEqualTo(1);
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

    @Nested
    @DisplayName("Retrieve revision detail")
    class RetrieveDetailTests {

        @Test
        void should_return_what_changed_and_where_to_look_at_it() {
            final var tag = persistTag();
            final Long revision = firstRevisionOf(tag);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("audit/revisionDetail")
                    .variable("revision", revision)
                    .execute()
                    .errors()
                    .verify()
                    .path("revisionDetail.revision").entity(Long.class).isEqualTo(revision)
                    .path("revisionDetail.source").entity(String.class).isEqualTo("SYSTEM")
                    .path("revisionDetail.entities").entityList(Object.class).hasSize(1)
                    .path("revisionDetail.entities[0].type").entity(String.class).isEqualTo("TAG")
                    .path("revisionDetail.entities[0].entityId").entity(Long.class).isEqualTo(tag.getId())
                    .path("revisionDetail.entities[0].revisionType").entity(String.class).isEqualTo("ADD")
                    .path("revisionDetail.entities[0].changes[0].field").entity(String.class).isEqualTo("name")
                    .path("revisionDetail.entities[0].target.type").entity(String.class).isEqualTo("TAG")
                    .path("revisionDetail.entities[0].target.id").entity(Long.class).isEqualTo(tag.getId());
        }

        @Test
        void should_return_FORBIDDEN_when_not_admin() {
            final var tag = persistTag();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainEditorAccessToken()))
                    .build()
                    .documentName("audit/revisionDetail")
                    .variable("revision", firstRevisionOf(tag))
                    .execute()
                    .errors()
                    .satisfy(errors -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    );
        }
    }

    @Nested
    @DisplayName("Retrieve authors")
    class RetrieveAuthorsTests {

        @Test
        void should_return_everyone_who_has_written_a_revision() {
            persistTag();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("audit/revisionAuthors")
                    .execute()
                    .errors()
                    .verify()
                    .path("revisionAuthors").entityList(String.class).contains("system");
        }
    }

    @Nested
    @DisplayName("Revision origin")
    class RevisionOriginTests {

        @Test
        void should_record_a_restore_as_its_own_source() {
            final var tag = persistTag();
            grantReadPermissionToRoleAdmin(toObjectIdentity(Tag.class, tag.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Tag.class, tag.getId()));

            tag.setName("renamed");
            repository.save(tag);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("audit/restore")
                    .variable("type", AuditedType.TAG)
                    .variable("id", tag.getId())
                    .variable("revision", firstRevisionOf(tag))
                    .variable("cascade", false)
                    .execute()
                    .errors()
                    .verify();

            feedAsAdmin(tester -> tester.variable("sources", List.of(AuditSource.RESTORE)))
                    .path("revisionFeedPaged.totalCount").entity(Integer.class).isEqualTo(1)
                    .path("revisionFeedPaged.edges[0].node.operation").entity(String.class).isEqualTo("restore")
                    .path("revisionFeedPaged.edges[0].node.comment").entity(String.class)
                    .matches(comment -> comment.startsWith("Restored from revision"));
        }

        @Test
        void should_record_the_graphql_operation_and_supplied_reason_behind_an_ordinary_edit() {
            final var tag = persistTag();
            grantReadPermissionToRoleAdmin(toObjectIdentity(Tag.class, tag.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Tag.class, tag.getId()));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> {
                        headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken());
                        headers.set("X-Audit-Reason", "Rättade stavfel");
                    })
                    .build()
                    .documentName("tag/tagUpdate")
                    .variable("id", tag.getId())
                    .variable("name", "renamed")
                    .execute()
                    .errors()
                    .verify();

            feedAsAdmin(tester -> tester.variable("sources", List.of(AuditSource.WEB)))
                    .path("revisionFeedPaged.totalCount").entity(Integer.class).isEqualTo(1)
                    .path("revisionFeedPaged.edges[0].node.operation").entity(String.class).isEqualTo("tagUpdate")
                    .path("revisionFeedPaged.edges[0].node.comment").entity(String.class).isEqualTo("Rättade stavfel");
        }
    }
}