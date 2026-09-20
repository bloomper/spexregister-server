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
import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.settings.TypeRepository;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.SpexDetails;
import nu.fgv.register.server.spex.SpexDetailsRepository;
import nu.fgv.register.server.spex.SpexRepository;
import nu.fgv.register.server.spex.category.SpexCategory;
import nu.fgv.register.server.spex.category.SpexCategoryRepository;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.activity.ActivityRepository;
import nu.fgv.register.server.spexare.activity.task.TaskActivityRepository;
import nu.fgv.register.server.spexare.consent.ConsentRepository;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.tag.TagRepository;
import nu.fgv.register.server.task.Task;
import nu.fgv.register.server.task.TaskRepository;
import nu.fgv.register.server.task.category.TaskCategory;
import nu.fgv.register.server.task.category.TaskCategoryRepository;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.util.AbstractAuditable;
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import nu.fgv.register.server.util.randomizer.SocialSecurityNumberRandomizer;
import nu.fgv.register.server.util.randomizer.YearRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.ResponseError;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.inClass;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class SpexareBulkGraphqlApiIntegrationTest extends AbstractGraphqlIntegrationTest {

    private final EasyRandom random;
    private final SpexareRepository spexareRepository;
    private final TagRepository tagRepository;
    private final TypeRepository typeRepository;
    private final ConsentRepository consentRepository;
    private final SpexRepository spexRepository;
    private final SpexDetailsRepository spexDetailsRepository;
    private final SpexCategoryRepository spexCategoryRepository;
    private final TaskRepository taskRepository;
    private final TaskCategoryRepository taskCategoryRepository;
    private final ActivityRepository activityRepository;
    private final TaskActivityRepository taskActivityRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public SpexareBulkGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
                                                final AclCache aclCache,
                                                final Keycloak keycloakAdminClient,
                                                final String keycloakClientId,
                                                final PermissionService permissionService,
                                                final SpexareRepository spexareRepository,
                                                final TagRepository tagRepository,
                                                final TypeRepository typeRepository,
                                                final ConsentRepository consentRepository,
                                                final SpexRepository spexRepository,
                                                final SpexDetailsRepository spexDetailsRepository,
                                                final SpexCategoryRepository spexCategoryRepository,
                                                final TaskRepository taskRepository,
                                                final TaskCategoryRepository taskCategoryRepository,
                                                final ActivityRepository activityRepository,
                                                final TaskActivityRepository taskActivityRepository,
                                                final ObjectMapper objectMapper) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.spexareRepository = spexareRepository;
        this.tagRepository = tagRepository;
        this.typeRepository = typeRepository;
        this.consentRepository = consentRepository;
        this.spexRepository = spexRepository;
        this.spexDetailsRepository = spexDetailsRepository;
        this.spexCategoryRepository = spexCategoryRepository;
        this.taskRepository = taskRepository;
        this.taskCategoryRepository = taskCategoryRepository;
        this.activityRepository = activityRepository;
        this.taskActivityRepository = taskActivityRepository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

        parameters
                .randomize(
                        named("year"), new YearRandomizer()
                )
                .randomize(
                        named("firstYear"), new YearRandomizer()
                )
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
                .excludeField(named("version").and(ofType(Long.class)).and(inClass(AbstractAuditable.class)))
                .randomizationDepth(1);
        random = new EasyRandom(parameters);
    }

    private static boolean isClassification(final ResponseError error, final ErrorType type) {
        return type.toString().equals(String.valueOf(error.getExtensions().get("classification")));
    }

    @BeforeEach
    void setUp() {
        httpGraphQlTester = HttpGraphQlTester.create(
                WebTestClient.bindToServer()
                        .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath))
                        .build()
        );

        // revinfo is left alone: every *_audit row references it, so clearing it breaks the FK.
        JdbcTestUtils.deleteFromTables(jdbcClient,
                "task_activity", "spex_activity", "activity",
                "tagging", "consent", "tag", "type", "task", "task_category", "spex", "spex_details", "spex_category", "spexare",
                "task_activity_audit", "spex_activity_audit", "activity_audit",
                "tagging_audit", "consent_audit", "tag_audit", "type_audit",
                "task_audit", "task_category_audit", "spex_audit", "spex_details_audit", "spex_category_audit", "spexare_audit");
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

    private Spexare persistReadOnlySpexare() {
        final var spexare = random.nextObject(Spexare.class);

        spexare.setId(null);
        spexare.setPublished(true);

        final var saved = spexareRepository.save(spexare);

        grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, saved.getId()));

        return saved;
    }

    private Tag persistTag() {
        final var tag = random.nextObject(Tag.class);

        tag.setId(null);

        return tagRepository.save(tag);
    }

    private Type persistType(final TypeType typeType) {
        final var type = random.nextObject(Type.class);

        type.setType(typeType);

        return typeRepository.save(type);
    }

    private Spex persistSpex() {
        final var category = random.nextObject(SpexCategory.class);

        category.setId(null);

        final var details = random.nextObject(SpexDetails.class);

        details.setId(null);
        details.setCategory(spexCategoryRepository.save(category));

        final var spex = random.nextObject(Spex.class);

        spex.setId(null);
        spex.setParent(null);
        spex.setDetails(spexDetailsRepository.save(details));

        return spexRepository.save(spex);
    }

    private Task persistTask() {
        final var category = random.nextObject(TaskCategory.class);

        category.setId(null);

        final var task = random.nextObject(Task.class);

        task.setId(null);
        task.setCategory(taskCategoryRepository.save(category));

        return taskRepository.save(task);
    }

    private HttpGraphQlTester asAdmin() {
        return httpGraphQlTester
                .mutate()
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                .build();
    }

    private long countTaggings() {
        return jdbcClient.sql("SELECT COUNT(*) FROM tagging").query(Long.class).single();
    }

    private long countActivities() {
        return jdbcClient.sql("SELECT COUNT(*) FROM activity").query(Long.class).single();
    }

    private long countTaskActivities() {
        return jdbcClient.sql("SELECT COUNT(*) FROM task_activity").query(Long.class).single();
    }

    private long latestSpexareRevision() {
        return jdbcClient.sql("SELECT COALESCE(MAX(rev), 0) FROM spexare_audit").query(Long.class).single();
    }

    private long countSpexareRevisionsSince(final long revision) {
        return jdbcClient
                .sql("SELECT COUNT(DISTINCT rev) FROM spexare_audit WHERE rev > :revision")
                .param("revision", revision)
                .query(Long.class)
                .single();
    }

    @Nested
    @DisplayName("Preview")
    class PreviewTests {

        @Test
        void should_report_outcomes_without_writing_anything() {
            final var untagged = persistWritableSpexare();
            final var tag = persistTag();

            asAdmin()
                    .documentName("spexare/bulk/spexareBulkPreview")
                    .variable("input", Map.of(
                            "target", Map.of("ids", List.of(untagged.getId())),
                            "operation", "TAG_ADD",
                            "tags", List.of(tag.getId())))
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareBulkPreview.applied").entity(Integer.class).isEqualTo(1)
                    .path("spexareBulkPreview.requested").entity(Integer.class).isEqualTo(1)
                    .path("spexareBulkPreview.entries[0].outcome").entity(String.class).isEqualTo("APPLIED");

            assertThat(countTaggings()).isZero();
        }

        @Test
        void should_report_a_record_the_caller_may_not_write_as_not_permitted() {
            final var writable = persistWritableSpexare();
            final var readOnly = persistReadOnlySpexare();
            final var tag = persistTag();

            asAdmin()
                    .documentName("spexare/bulk/spexareBulkPreview")
                    .variable("input", Map.of(
                            "target", Map.of("ids", List.of(writable.getId(), readOnly.getId())),
                            "operation", "TAG_ADD",
                            "tags", List.of(tag.getId())))
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareBulkPreview.applied").entity(Integer.class).isEqualTo(1)
                    .path("spexareBulkPreview.blocked").entity(Integer.class).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Apply")
    class ApplyTests {

        @Test
        void should_tag_every_target_in_a_single_revision() {
            final var ids = IntStream.range(0, 5)
                    .mapToObj(_ -> persistWritableSpexare().getId())
                    .toList();
            final var tag = persistTag();
            final var before = latestSpexareRevision();

            asAdmin()
                    .documentName("spexare/bulk/spexareBulkApply")
                    .variable("input", Map.of(
                            "target", Map.of("ids", ids),
                            "operation", "TAG_ADD",
                            "tags", List.of(tag.getId())))
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareBulkApply.applied").entity(Integer.class).isEqualTo(5);

            assertThat(countTaggings()).isEqualTo(5);
            // The point of doing this server-side: one transaction, so the change log gets one row.
            assertThat(countSpexareRevisionsSince(before)).isEqualTo(1);
        }

        @Test
        void should_skip_a_record_that_already_has_the_tag() {
            final var spexare = persistWritableSpexare();
            final var tag = persistTag();
            final var input = Map.of(
                    "target", Map.of("ids", List.of(spexare.getId())),
                    "operation", "TAG_ADD",
                    "tags", List.of(tag.getId()));

            asAdmin().documentName("spexare/bulk/spexareBulkApply").variable("input", input).execute().errors().verify();

            asAdmin()
                    .documentName("spexare/bulk/spexareBulkApply")
                    .variable("input", input)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareBulkApply.applied").entity(Integer.class).isEqualTo(0)
                    .path("spexareBulkApply.unchanged").entity(Integer.class).isEqualTo(1);

            assertThat(countTaggings()).isEqualTo(1);
        }

        @Test
        void should_leave_a_record_the_caller_may_not_write_alone() {
            final var writable = persistWritableSpexare();
            final var readOnly = persistReadOnlySpexare();
            final var tag = persistTag();

            asAdmin()
                    .documentName("spexare/bulk/spexareBulkApply")
                    .variable("input", Map.of(
                            "target", Map.of("ids", List.of(writable.getId(), readOnly.getId())),
                            "operation", "TAG_ADD",
                            "tags", List.of(tag.getId())))
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareBulkApply.applied").entity(Integer.class).isEqualTo(1)
                    .path("spexareBulkApply.blocked").entity(Integer.class).isEqualTo(1);

            assertThat(countTaggings()).isEqualTo(1);
        }

        @Test
        void should_remove_tags() {
            final var spexare = persistWritableSpexare();
            final var tag = persistTag();

            asAdmin()
                    .documentName("spexare/bulk/spexareBulkApply")
                    .variable("input", Map.of(
                            "target", Map.of("ids", List.of(spexare.getId())),
                            "operation", "TAG_ADD",
                            "tags", List.of(tag.getId())))
                    .execute()
                    .errors()
                    .verify();

            asAdmin()
                    .documentName("spexare/bulk/spexareBulkApply")
                    .variable("input", Map.of(
                            "target", Map.of("ids", List.of(spexare.getId())),
                            "operation", "TAG_REMOVE",
                            "tags", List.of(tag.getId())))
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareBulkApply.applied").entity(Integer.class).isEqualTo(1);

            assertThat(countTaggings()).isZero();
        }

        @Test
        void should_target_by_filter() {
            final var published = persistWritableSpexare();
            final var unpublished = persistWritableSpexare();

            unpublished.setPublished(false);
            spexareRepository.save(unpublished);

            final var tag = persistTag();

            asAdmin()
                    .documentName("spexare/bulk/spexareBulkApply")
                    .variable("input", Map.of(
                            "target", Map.of("filter", "published:TRUE"),
                            "operation", "TAG_ADD",
                            "tags", List.of(tag.getId())))
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareBulkApply.applied").entity(Integer.class).isEqualTo(1)
                    .path("spexareBulkApply.entries[0].id").entity(String.class)
                    .isEqualTo(String.valueOf(published.getId()));

            assertThat(countTaggings()).isEqualTo(1);
        }

        @Test
        void should_upsert_consents() {
            final var spexare = persistWritableSpexare();
            final var type = persistType(TypeType.CONSENT);
            final var toTrue = Map.of(
                    "target", Map.of("ids", List.of(spexare.getId())),
                    "operation", "CONSENT_SET",
                    "values", List.of(Map.of("typeId", type.getId(), "value", true)));

            asAdmin()
                    .documentName("spexare/bulk/spexareBulkApply")
                    .variable("input", toTrue)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareBulkApply.applied").entity(Integer.class).isEqualTo(1);

            assertThat(consentRepository.count()).isEqualTo(1);

            // Setting the same value again is a skip, not a second row and not a failure.
            asAdmin()
                    .documentName("spexare/bulk/spexareBulkApply")
                    .variable("input", toTrue)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareBulkApply.unchanged").entity(Integer.class).isEqualTo(1);

            assertThat(consentRepository.count()).isEqualTo(1);

            asAdmin()
                    .documentName("spexare/bulk/spexareBulkApply")
                    .variable("input", Map.of(
                            "target", Map.of("ids", List.of(spexare.getId())),
                            "operation", "CONSENT_SET",
                            "values", List.of(Map.of("typeId", type.getId(), "value", false))))
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareBulkApply.applied").entity(Integer.class).isEqualTo(1);

            assertThat(consentRepository.count()).isEqualTo(1);
            assertThat(consentRepository.findAll().getFirst().getValue()).isFalse();
        }

        @Test
        void should_add_the_tasks_to_the_activity_for_the_spex_creating_it_when_missing() {
            final var spexare = persistWritableSpexare();
            final var spex = persistSpex();
            final var task = persistTask();

            asAdmin()
                    .documentName("spexare/bulk/spexareBulkApply")
                    .variable("input", Map.of(
                            "target", Map.of("ids", List.of(spexare.getId())),
                            "operation", "TASK_ADD",
                            "spexId", spex.getId(),
                            "tasks", List.of(task.getId())))
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareBulkApply.applied").entity(Integer.class).isEqualTo(1);

            assertThat(countActivities()).isEqualTo(1);
            assertThat(countTaskActivities()).isEqualTo(1);
        }

        @Test
        void should_reuse_the_activity_a_preceding_spex_add_created_rather_than_making_a_second_one() {
            final var ids = IntStream.range(0, 3)
                    .mapToObj(_ -> persistWritableSpexare().getId())
                    .toList();
            final var spex = persistSpex();
            final var task = persistTask();

            asAdmin()
                    .documentName("spexare/bulk/spexareBulkApply")
                    .variable("input", Map.of(
                            "target", Map.of("ids", ids),
                            "operation", "SPEX_ADD",
                            "spex", List.of(spex.getId())))
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareBulkApply.applied").entity(Integer.class).isEqualTo(3);

            // One activity per spexare, each holding that spexare's own spex activity.
            assertThat(countActivities()).isEqualTo(3);

            asAdmin()
                    .documentName("spexare/bulk/spexareBulkApply")
                    .variable("input", Map.of(
                            "target", Map.of("ids", ids),
                            "operation", "TASK_ADD",
                            "spexId", spex.getId(),
                            "tasks", List.of(task.getId())))
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareBulkApply.applied").entity(Integer.class).isEqualTo(3);

            assertThat(countActivities()).isEqualTo(3);
            assertThat(countTaskActivities()).isEqualTo(3);

            final var orphans = jdbcClient
                    .sql("SELECT COUNT(*) FROM activity a WHERE NOT EXISTS (SELECT 1 FROM spex_activity sa WHERE sa.activity_id = a.id)")
                    .query(Long.class)
                    .single();

            assertThat(orphans).isZero();
        }

        @Test
        void should_skip_a_task_the_activity_already_has() {
            final var spexare = persistWritableSpexare();
            final var spex = persistSpex();
            final var task = persistTask();
            final var input = Map.of(
                    "target", Map.of("ids", List.of(spexare.getId())),
                    "operation", "TASK_ADD",
                    "spexId", spex.getId(),
                    "tasks", List.of(task.getId()));

            asAdmin().documentName("spexare/bulk/spexareBulkApply").variable("input", input).execute().errors().verify();

            asAdmin()
                    .documentName("spexare/bulk/spexareBulkApply")
                    .variable("input", input)
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareBulkApply.applied").entity(Integer.class).isEqualTo(0)
                    .path("spexareBulkApply.unchanged").entity(Integer.class).isEqualTo(1);

            assertThat(countActivities()).isEqualTo(1);
            assertThat(countTaskActivities()).isEqualTo(1);
        }

        @Test
        void should_set_fields() {
            final var spexare = persistWritableSpexare();

            asAdmin()
                    .documentName("spexare/bulk/spexareBulkApply")
                    .variable("input", Map.of(
                            "target", Map.of("ids", List.of(spexare.getId())),
                            "operation", "FIELDS_SET",
                            "fields", Map.of("published", false)))
                    .execute()
                    .errors()
                    .verify()
                    .path("spexareBulkApply.applied").entity(Integer.class).isEqualTo(1);

            assertThat(spexareRepository.findById(spexare.getId()).orElseThrow().getPublished()).isFalse();
        }
    }

    @Nested
    @DisplayName("Rejected input")
    class RejectedInputTests {

        @Test
        void should_return_BAD_REQUEST_when_both_ids_and_filter_are_given() {
            final var spexare = persistWritableSpexare();
            final var tag = persistTag();

            asAdmin()
                    .documentName("spexare/bulk/spexareBulkApply")
                    .variable("input", Map.of(
                            "target", Map.of("ids", List.of(spexare.getId()), "filter", "published:TRUE"),
                            "operation", "TAG_ADD",
                            "tags", List.of(tag.getId())))
                    .execute()
                    .errors()
                    .satisfy(errors -> assertThat(errors).anyMatch(error -> isClassification(error, ErrorType.BAD_REQUEST)));

            assertThat(countTaggings()).isZero();
        }

        @Test
        void should_return_BAD_REQUEST_when_the_target_is_empty() {
            final var tag = persistTag();

            asAdmin()
                    .documentName("spexare/bulk/spexareBulkApply")
                    .variable("input", Map.of(
                            "target", Map.of(),
                            "operation", "TAG_ADD",
                            "tags", List.of(tag.getId())))
                    .execute()
                    .errors()
                    .satisfy(errors -> assertThat(errors).anyMatch(error -> isClassification(error, ErrorType.BAD_REQUEST)));
        }

        @Test
        void should_return_BAD_REQUEST_when_TASK_ADD_has_no_spexId() {
            final var spexare = persistWritableSpexare();

            asAdmin()
                    .documentName("spexare/bulk/spexareBulkApply")
                    .variable("input", Map.of(
                            "target", Map.of("ids", List.of(spexare.getId())),
                            "operation", "TASK_ADD",
                            "tasks", List.of("1")))
                    .execute()
                    .errors()
                    .satisfy(errors -> assertThat(errors).anyMatch(error -> isClassification(error, ErrorType.BAD_REQUEST)));
        }

        @Test
        void should_return_NOT_FOUND_and_write_nothing_when_a_tag_does_not_exist() {
            final var spexare = persistWritableSpexare();
            final var tag = persistTag();

            asAdmin()
                    .documentName("spexare/bulk/spexareBulkApply")
                    .variable("input", Map.of(
                            "target", Map.of("ids", List.of(spexare.getId())),
                            "operation", "TAG_ADD",
                            "tags", List.of(tag.getId(), tag.getId() + 1000)))
                    .execute()
                    .errors()
                    .satisfy(errors -> assertThat(errors).anyMatch(error -> isClassification(error, ErrorType.NOT_FOUND)));

            // The payload is resolved before the first write, so a bad id costs nothing.
            assertThat(countTaggings()).isZero();
        }

        @Test
        void should_return_BAD_REQUEST_when_a_consent_type_is_not_a_consent_type() {
            final var spexare = persistWritableSpexare();
            final var type = persistType(TypeType.TOGGLE);

            asAdmin()
                    .documentName("spexare/bulk/spexareBulkApply")
                    .variable("input", Map.of(
                            "target", Map.of("ids", List.of(spexare.getId())),
                            "operation", "CONSENT_SET",
                            "values", List.of(Map.of("typeId", type.getId(), "value", true))))
                    .execute()
                    .errors()
                    .satisfy(errors -> assertThat(errors).anyMatch(error -> isClassification(error, ErrorType.BAD_REQUEST)));

            assertThat(consentRepository.count()).isZero();
        }
    }
}
