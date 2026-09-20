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

package nu.fgv.register.server.spexare.bulk;

import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.tag.TagRepository;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.randomizer.SocialSecurityNumberRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.ApiVersionInserter;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.inClass;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class SpexareBulkApiIntegrationTest extends AbstractIntegrationTest {

    private final EasyRandom random;
    private final SpexareRepository spexareRepository;
    private final TagRepository tagRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public SpexareBulkApiIntegrationTest(final JdbcClient jdbcClient,
                                         final AclCache aclCache,
                                         final Keycloak keycloakAdminClient,
                                         final String keycloakClientId,
                                         final PermissionService permissionService,
                                         final SpexareRepository spexareRepository,
                                         final TagRepository tagRepository,
                                         final ObjectMapper objectMapper) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.spexareRepository = spexareRepository;
        this.tagRepository = tagRepository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

        parameters
                .randomize(
                        named("socialSecurityNumber"), new SocialSecurityNumberRandomizer()
                )
                .excludeField(named("partner").and(ofType(Spexare.class)).and(inClass(Spexare.class)))
                .excludeField(named("user").and(ofType(User.class)).and(inClass(Spexare.class)))
                .excludeField(named("activities").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("tags").and(ofType(Set.class)).and(inClass(Spexare.class)))
                .excludeField(named("addresses").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("memberships").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("consents").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("toggles").and(ofType(List.class)).and(inClass(Spexare.class)))
                .randomizationDepth(1);
        random = new EasyRandom(parameters);
    }

    @BeforeEach
    void setUp() {
        restTestClient = RestTestClient
                .bindToServer()
                .baseUrl("http://localhost:%s/api/spexare/bulk".formatted(localPort))
                .apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
                .build();

        JdbcTestUtils.deleteFromTables(jdbcClient, "tagging", "tag", "spexare", "tagging_audit", "tag_audit", "spexare_audit");
    }

    private Spexare persistWritableSpexare() {
        final var spexare = random.nextObject(Spexare.class);

        spexare.setId(null);
        spexare.setPublished(true);

        final var saved = spexareRepository.save(spexare);

        grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, saved.getId()));
        grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, saved.getId()));

        return saved;
    }

    private Tag persistTag() {
        final var tag = random.nextObject(Tag.class);

        tag.setId(null);

        return tagRepository.save(tag);
    }

    private Map<String, Object> tagAdd(final List<Long> ids, final Long tagId) {
        return Map.of(
                "target", Map.of("ids", ids),
                "operation", "TAG_ADD",
                "tags", List.of(tagId));
    }

    private long countTaggings() {
        return jdbcClient.sql("SELECT COUNT(*) FROM tagging").query(Long.class).single();
    }

    @Nested
    @DisplayName("Preview")
    class PreviewTests {

        @Test
        void should_return_200_and_write_nothing() {
            final var spexare = persistWritableSpexare();
            final var tag = persistTag();

            restTestClient
                    .post()
                    .uri("/preview")
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(tagAdd(List.of(spexare.getId()), tag.getId()))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("applied").isEqualTo(1)
                    .jsonPath("requested").isEqualTo(1)
                    .jsonPath("entries[0].outcome").isEqualTo("APPLIED");

            assertThat(countTaggings()).isZero();
        }
    }

    @Nested
    @DisplayName("Apply")
    class ApplyTests {

        @Test
        void should_return_200_and_write() {
            final var spexare = persistWritableSpexare();
            final var tag = persistTag();

            restTestClient
                    .post()
                    .uri(builder -> builder.build())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(tagAdd(List.of(spexare.getId()), tag.getId()))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("applied").isEqualTo(1);

            assertThat(countTaggings()).isEqualTo(1);
        }

        @Test
        void should_record_the_audit_reason_on_the_revision() {
            final var spexare = persistWritableSpexare();
            final var tag = persistTag();
            final var reason = "Massåtgärd: Lägg till taggar (1 poster)";

            restTestClient
                    .post()
                    .uri(builder -> builder.build())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("X-Audit-Reason", reason)
                    .apiVersion("1.0")
                    .body(tagAdd(List.of(spexare.getId()), tag.getId()))
                    .exchange()
                    .expectStatus().isOk();

            final var revision = jdbcClient
                    .sql("SELECT source, comment FROM revinfo WHERE id = (SELECT MAX(rev) FROM spexare_audit)")
                    .query()
                    .singleRow();

            assertThat(revision.get("source")).isEqualTo("WEB");
            assertThat(revision.get("comment")).isEqualTo(reason);
        }
    }

    @Nested
    @DisplayName("Rejected input")
    class RejectedInputTests {

        @Test
        void should_return_400_when_both_ids_and_filter_are_given() {
            final var spexare = persistWritableSpexare();
            final var tag = persistTag();

            restTestClient
                    .post()
                    .uri(builder -> builder.build())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(Map.of(
                            "target", Map.of("ids", List.of(spexare.getId()), "filter", "published:TRUE"),
                            "operation", "TAG_ADD",
                            "tags", List.of(tag.getId())))
                    .exchange()
                    .expectStatus().isBadRequest();

            assertThat(countTaggings()).isZero();
        }

        @Test
        void should_return_404_when_a_tag_does_not_exist() {
            final var spexare = persistWritableSpexare();
            final var tag = persistTag();

            restTestClient
                    .post()
                    .uri(builder -> builder.build())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .body(Map.of(
                            "target", Map.of("ids", List.of(spexare.getId())),
                            "operation", "TAG_ADD",
                            "tags", List.of(tag.getId(), tag.getId() + 1000)))
                    .exchange()
                    .expectStatus().isNotFound();

            assertThat(countTaggings()).isZero();
        }
    }
}
