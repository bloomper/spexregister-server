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

import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.SpexDetails;
import nu.fgv.register.server.spex.SpexDetailsRepository;
import nu.fgv.register.server.spex.SpexRepository;
import nu.fgv.register.server.spex.category.SpexCategory;
import nu.fgv.register.server.spex.category.SpexCategoryRepository;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.activity.Activity;
import nu.fgv.register.server.spexare.activity.ActivityRepository;
import nu.fgv.register.server.spexare.activity.task.TaskActivity;
import nu.fgv.register.server.spexare.activity.task.TaskActivityRepository;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.tag.TagRepository;
import nu.fgv.register.server.task.Task;
import nu.fgv.register.server.task.TaskRepository;
import nu.fgv.register.server.task.category.TaskCategory;
import nu.fgv.register.server.task.category.TaskCategoryRepository;
import nu.fgv.register.server.util.AbstractAuditable;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.HalEmbeddedResponse;
import nu.fgv.register.server.util.randomizer.RandomizerSupport;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.ApiVersionInserter;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.inClass;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class AuditApiIntegrationTest extends AbstractIntegrationTest {

    private final EasyRandom random;
    private final TagRepository repository;
    private final SpexRepository spexRepository;
    private final SpexDetailsRepository spexDetailsRepository;
    private final SpexareRepository spexareRepository;
    private final SpexCategoryRepository spexCategoryRepository;
    private final TaskCategoryRepository taskCategoryRepository;
    private final TaskRepository taskRepository;
    private final ActivityRepository activityRepository;
    private final TaskActivityRepository taskActivityRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public AuditApiIntegrationTest(final JdbcClient jdbcClient,
                                   final Keycloak keycloakAdminClient,
                                   final String keycloakClientId,
                                   final TagRepository repository,
                                   final SpexRepository spexRepository,
                                   final SpexDetailsRepository spexDetailsRepository,
                                   final SpexCategoryRepository spexCategoryRepository,
                                   final SpexareRepository spexareRepository,
                                   final TaskCategoryRepository taskCategoryRepository,
                                   final TaskRepository taskRepository,
                                   final ActivityRepository activityRepository,
                                   final TaskActivityRepository taskActivityRepository,
                                   final ObjectMapper objectMapper) {
        super(jdbcClient, keycloakAdminClient, keycloakClientId, objectMapper);
        this.repository = repository;
        this.spexRepository = spexRepository;
        this.spexDetailsRepository = spexDetailsRepository;
        this.spexCategoryRepository = spexCategoryRepository;
        this.spexareRepository = spexareRepository;
        this.taskCategoryRepository = taskCategoryRepository;
        this.taskRepository = taskRepository;
        this.activityRepository = activityRepository;
        this.taskActivityRepository = taskActivityRepository;

        final EasyRandomParameters parameters = RandomizerSupport.parameters();

        parameters
                .excludeField(named("version").and(ofType(Long.class)).and(inClass(AbstractAuditable.class)));
        random = new EasyRandom(parameters);
    }

    @BeforeEach
    void setUp() {
        restTestClient = RestTestClient
                .bindToServer()
                .baseUrl("http://localhost:%s/api/revisions".formatted(localPort))
                .apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
                .build();

        JdbcTestUtils.deleteFromTables(jdbcClient, "task_activity", "activity", "task", "task_category", "tag", "spex",
                "spex_details", "spex_category", "spexare", "task_activity_audit", "activity_audit", "task_audit",
                "task_category_audit", "spexare_audit", "tag_audit", "spex_audit", "spex_details_audit", "spex_category_audit",
                "revchanges", "revinfo");
    }

    private List<RevisionDto> retrieveRevisions(final UUID id, final String token) {
        return retrieveRevisions(AuditedType.TAG, id, token);
    }

    private List<RevisionDto> retrieveRevisions(final AuditedType type, final UUID id, final String token) {
        return Objects.requireNonNull(
                        restTestClient
                                .get()
                                .uri("/{type}/{id}", type.name(), id)
                                .header(HttpHeaders.AUTHORIZATION, token)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .apiVersion("1.0")
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<RevisionDto>>() {
                                })
                                .returnResult()
                                .getResponseBody())
                .getList("revisions");
    }

    private SpexCategory persistSpexCategory(final String name) {
        final var category = random.nextObject(SpexCategory.class);

        category.setId(null);
        category.setName(name);
        category.setFirstYear("1948");
        category.setLogoUrl(null);

        return spexCategoryRepository.save(category);
    }

    private SpexDetails persistSpexDetails(final SpexCategory category, final String title) {
        final var details = random.nextObject(SpexDetails.class);

        details.setId(null);
        details.setTitle(title);
        details.setCategory(category);

        return spexDetailsRepository.save(details);
    }

    private Spex persistSpex(final SpexDetails details, final String year) {
        final var spex = random.nextObject(Spex.class);

        spex.setId(null);
        spex.setParent(null);
        spex.setDetails(details);
        spex.setYear(year);

        return spexRepository.save(spex);
    }

    /**
     * An unpublished spexare, which a user may not read.
     */
    private Spexare persistUnpublishedSpexare() {
        final Spexare spexare = new Spexare();

        spexare.setFirstName("Olle");
        spexare.setLastName("Opublicerad");
        spexare.setDeceased(false);
        spexare.setPublished(false);

        return spexareRepository.save(spexare);
    }

    private Task persistTask(final String name) {
        final TaskCategory category = new TaskCategory();

        category.setName("Ensemble");
        category.setActorPresent(true);

        final Task task = new Task();

        task.setName(name);
        task.setCategory(taskCategoryRepository.save(category));

        return taskRepository.save(task);
    }

    /**
     * A task activity, owned by an unpublished spexare: its history is the spexare's, and its task is an association.
     */
    private TaskActivity persistTaskActivity(final Task task) {
        final Activity activity = new Activity();

        activity.setSpexare(persistUnpublishedSpexare());

        final TaskActivity taskActivity = new TaskActivity();

        taskActivity.setActivity(activityRepository.save(activity));
        taskActivity.setTask(task);

        return taskActivityRepository.save(taskActivity);
    }

    private Tag persistTag() {
        final Tag tag = random.nextObject(Tag.class);

        tag.setId(null);

        return repository.save(tag);
    }

    @Nested
    @DisplayName("Retrieve revisions")
    class RetrieveRevisionsTests {

        @Test
        void should_return_add_and_mod() {
            final var tag = persistTag();

            tag.setName("renamed");
            repository.save(tag);

            final List<RevisionDto> result = retrieveRevisions(tag.getUuid(), obtainAdminAccessToken());

            assertThat(result).hasSize(2);
            assertThat(result.getFirst().revisionType().name()).isEqualTo("MOD");
            assertThat(result.getLast().revisionType().name()).isEqualTo("ADD");
            assertThat(result.getFirst().changes())
                    .extracting(FieldChangeDto::field, FieldChangeDto::newValue)
                    .containsExactly(org.assertj.core.api.Assertions.tuple("name", "renamed"));
        }

        @Test
        void should_return_403_when_not_permitted() {
            final var spexare = persistUnpublishedSpexare();

            restTestClient
                    .get()
                    .uri("/{type}/{id}", "SPEXARE", spexare.getUuid())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden();
        }
    }

    @Nested
    @DisplayName("Association rendering")
    class AssociationTests {

        @Test
        void should_render_the_label_of_an_association_instead_of_its_id() {
            final var taskActivity = persistTaskActivity(persistTask("Scenmästare"));

            final List<RevisionDto> result = retrieveRevisions(AuditedType.TASK_ACTIVITY, taskActivity.getUuid(), obtainAdminAccessToken());

            assertThat(result).isNotEmpty();
            assertThat(result.getFirst().changes())
                    .filteredOn(change -> "task".equals(change.field()))
                    .singleElement()
                    .satisfies(change -> assertThat(change.newValue()).isEqualTo("Scenmästare"));
        }

        @Test
        void should_attribute_each_change_to_its_own_entity() {
            final var taskActivity = persistTaskActivity(persistTask("Scenmästare"));
            final var activity = taskActivity.getActivity();

            final List<RevisionDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri("/{type}/{id}/related/{relatedType}", "SPEXARE", activity.getSpexare().getUuid(), "ACTIVITY")
                                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .apiVersion("1.0")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(new ParameterizedTypeReference<@NonNull HalEmbeddedResponse<RevisionDto>>() {
                                    })
                                    .returnResult()
                                    .getResponseBody())
                    .getList("revisions");

            assertThat(result)
                    .flatExtracting(RevisionDto::changes)
                    .filteredOn(change -> "task".equals(change.field()))
                    .singleElement()
                    .satisfies(change -> {
                        assertThat(change.type()).isEqualTo(AuditedType.TASK_ACTIVITY);
                        assertThat(change.entityId()).isEqualTo(taskActivity.getUuid().toString());
                    });

            assertThat(result)
                    .flatExtracting(RevisionDto::changes)
                    .filteredOn(change -> "spexare".equals(change.field()))
                    .singleElement()
                    .satisfies(change -> {
                        assertThat(change.type()).isEqualTo(AuditedType.ACTIVITY);
                        assertThat(change.entityId()).isEqualTo(activity.getUuid().toString());
                    });
        }

        @Test
        void should_refuse_to_serve_a_property_that_is_not_binary() {
            final var spexare = persistUnpublishedSpexare();
            final var creation = retrieveRevisions(AuditedType.SPEXARE, spexare.getUuid(), obtainAdminAccessToken()).getLast();

            restTestClient
                    .get()
                    .uri("/{type}/{id}/{revision}/binary/{field}", "SPEXARE", spexare.getUuid(), creation.revision(), "firstName")
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        void should_still_report_a_change_when_two_targets_share_a_label() {
            final var second = persistTask("Sångare");
            final var taskActivity = persistTaskActivity(persistTask("Sångare"));

            taskActivity.setTask(second);
            taskActivityRepository.save(taskActivity);

            final List<RevisionDto> result = retrieveRevisions(AuditedType.TASK_ACTIVITY, taskActivity.getUuid(), obtainAdminAccessToken());

            assertThat(result.getFirst().changes())
                    .extracting(FieldChangeDto::field)
                    .contains("task");
        }
    }

    @Nested
    @DisplayName("Base data")
    class BaseDataTests {

        @Test
        void should_refuse_the_history_of_base_data_since_spexipedia_keeps_it() {
            final var category = persistSpexCategory("Chalmersspexet");
            final var details = persistSpexDetails(category, "Nobel");
            final var spex = persistSpex(details, "2024");

            List.of("SPEX", "SPEX_DETAILS", "SPEX_CATEGORY").forEach(type -> restTestClient
                    .get()
                    .uri("/{type}/{id}", type, "SPEX".equals(type) ? spex.getUuid() : "SPEX_DETAILS".equals(type) ? details.getUuid() : category.getUuid())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isBadRequest());
        }

        @Test
        void should_refuse_to_restore_base_data() {
            final var category = persistSpexCategory("Chalmersspexet");
            final var details = persistSpexDetails(category, "Nobel");
            final var spex = persistSpex(details, "2024");
            final Long originalRevision = jdbcClient
                    .sql("SELECT MIN(rev) FROM spex_audit WHERE id = :id")
                    .param("id", spex.getId())
                    .query(Long.class)
                    .single();

            spex.setYear("2025");
            spexRepository.save(spex);

            restTestClient
                    .post()
                    .uri("/{type}/{id}/{revision}/restore", "SPEX", spex.getUuid(), originalRevision)
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isBadRequest();

            assertThat(spexRepository.findById(spex.getId()).orElseThrow().getYear()).isEqualTo("2025");
        }
    }

    @Nested
    @DisplayName("Restore")
    class RestoreTests {

        @Test
        void should_restore_and_append_a_new_revision() {
            final var tag = persistTag();
            final var originalName = tag.getName();
            final var originalCreatedBy = tag.getCreatedBy();

            tag.setName("renamed");
            repository.save(tag);

            final var firstRevision = retrieveRevisions(tag.getUuid(), obtainAdminAccessToken()).getLast().revision();

            final RestoreResultDto result = Objects.requireNonNull(
                    restTestClient
                            .post()
                            .uri("/{type}/{id}/{revision}/restore", "TAG", tag.getUuid(), firstRevision)
                            .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .apiVersion("1.0")
                            .exchange()
                            .expectStatus().isOk()
                            .expectBody(RestoreResultDto.class)
                            .returnResult()
                            .getResponseBody());

            assertThat(result.updated()).isEqualTo(1);

            final var restored = repository.findById(tag.getId()).orElseThrow();

            assertThat(restored.getName()).isEqualTo(originalName);
            assertThat(restored.getCreatedBy()).isEqualTo(originalCreatedBy);
            assertThat(retrieveRevisions(tag.getUuid(), obtainAdminAccessToken())).hasSize(3);
        }

        @Test
        void should_return_403_when_editor() {
            final var tag = persistTag();

            restTestClient
                    .post()
                    .uri("/{type}/{id}/{revision}/restore", "TAG", tag.getUuid(), 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainEditorAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden();
        }
    }
}