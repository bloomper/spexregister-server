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
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.SpexDetails;
import nu.fgv.register.server.spex.SpexDetailsRepository;
import nu.fgv.register.server.spex.SpexRepository;
import nu.fgv.register.server.spex.category.SpexCategory;
import nu.fgv.register.server.spex.category.SpexCategoryRepository;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.tag.TagRepository;
import nu.fgv.register.server.util.AbstractAuditable;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.HalEmbeddedResponse;
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
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.ApiVersionInserter;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;

import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
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
    private final SpexCategoryRepository spexCategoryRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public AuditApiIntegrationTest(final JdbcClient jdbcClient,
                                   final AclCache aclCache,
                                   final Keycloak keycloakAdminClient,
                                   final String keycloakClientId,
                                   final PermissionService permissionService,
                                   final TagRepository repository,
                                   final SpexRepository spexRepository,
                                   final SpexDetailsRepository spexDetailsRepository,
                                   final SpexCategoryRepository spexCategoryRepository,
                                   final ObjectMapper objectMapper) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.repository = repository;
        this.spexRepository = spexRepository;
        this.spexDetailsRepository = spexDetailsRepository;
        this.spexCategoryRepository = spexCategoryRepository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

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

        JdbcTestUtils.deleteFromTables(jdbcClient, "tag", "spex", "spex_details", "spex_category",
                "tag_audit", "spex_audit", "spex_details_audit", "spex_category_audit", "revchanges", "revinfo");
    }

    @Nested
    @DisplayName("Retrieve revisions")
    class RetrieveRevisionsTests {

        @Test
        void should_return_add_and_mod() {
            final var tag = persistTag();
            grantReadPermissionToRoleAdmin(toObjectIdentity(Tag.class, tag.getId()));

            tag.setName("renamed");
            repository.save(tag);

            final List<RevisionDto> result = retrieveRevisions(tag.getId(), obtainAdminAccessToken());

            assertThat(result).hasSize(2);
            assertThat(result.getFirst().revisionType().name()).isEqualTo("MOD");
            assertThat(result.getLast().revisionType().name()).isEqualTo("ADD");
            assertThat(result.getFirst().changes())
                    .extracting(FieldChangeDto::field, FieldChangeDto::newValue)
                    .containsExactly(org.assertj.core.api.Assertions.tuple("name", "renamed"));
        }

        @Test
        void should_return_403_when_not_permitted() {
            final var tag = persistTag();

            restTestClient
                    .get()
                    .uri("/{type}/{id}", "TAG", tag.getId())
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
            final var category = persistSpexCategory("Chalmersspexet");
            final var details = persistSpexDetails(category, "Nobel");
            final var spex = persistSpex(details, "2024");
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            final List<RevisionDto> result = retrieveRevisions(AuditedType.SPEX, spex.getId(), obtainAdminAccessToken());

            assertThat(result).isNotEmpty();
            assertThat(result.getFirst().changes())
                    .filteredOn(change -> "details".equals(change.field()))
                    .singleElement()
                    .satisfies(change -> assertThat(change.newValue()).isEqualTo("Nobel"));
        }

        @Test
        void should_include_the_details_of_a_spex_in_its_own_timeline() {
            final var category = persistSpexCategory("Chalmersspexet");
            final var details = persistSpexDetails(category, "Nobel");
            final var spex = persistSpex(details, "2024");
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            final List<RevisionDto> result = retrieveRevisions(AuditedType.SPEX, spex.getId(), obtainAdminAccessToken());

            assertThat(result)
                    .extracting(RevisionDto::type)
                    .contains(AuditedType.SPEX, AuditedType.SPEX_DETAILS);

            assertThat(result)
                    .filteredOn(revision -> revision.type() == AuditedType.SPEX_DETAILS)
                    .flatExtracting(RevisionDto::changes)
                    .extracting(FieldChangeDto::field)
                    .contains("title", "poster", "category");
        }

        @Test
        void should_restore_the_details_along_with_the_spex() {
            final var category = persistSpexCategory("Chalmersspexet");
            final var details = persistSpexDetails(category, "Nobel");
            final var spex = persistSpex(details, "2024");
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(SpexDetails.class, details.getId()));

            final var originalRevision = retrieveRevisions(AuditedType.SPEX, spex.getId(), obtainAdminAccessToken()).stream()
                    .filter(revision -> revision.type() == AuditedType.SPEX)
                    .toList()
                    .getLast()
                    .revision();

            details.setTitle("Nobel 2");
            spexDetailsRepository.save(details);
            spex.setYear("2025");
            spexRepository.save(spex);

            restTestClient
                    .post()
                    .uri("/{type}/{id}/{revision}/restore", "SPEX", spex.getId(), originalRevision)
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk();

            assertThat(spexRepository.findById(spex.getId()).orElseThrow().getYear()).isEqualTo("2024");
            assertThat(spexDetailsRepository.findById(details.getId()).orElseThrow().getTitle()).isEqualTo("Nobel");
        }

        @Test
        void should_report_a_poster_change_on_the_details_of_a_spex() {
            final var category = persistSpexCategory("Chalmersspexet");
            final var details = persistSpexDetails(category, "Nobel");
            final var spex = persistSpex(details, "2024");
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            details.setPoster(new byte[]{4, 5, 6});
            spexDetailsRepository.save(details);

            final List<RevisionDto> result = retrieveRevisions(AuditedType.SPEX, spex.getId(), obtainAdminAccessToken());

            assertThat(result.getFirst().type()).isEqualTo(AuditedType.SPEX_DETAILS);
            assertThat(result.getFirst().changes())
                    .filteredOn(change -> "poster".equals(change.field()))
                    .singleElement()
                    .satisfies(change -> assertThat(change.binary()).isTrue());
        }

        @Test
        void should_attribute_each_change_in_a_merged_revision_to_its_own_entity() {
            final var category = persistSpexCategory("Chalmersspexet");
            final var details = persistSpexDetails(category, "Nobel");
            final var spex = persistSpex(details, "2024");
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            final List<RevisionDto> result = retrieveRevisions(AuditedType.SPEX, spex.getId(), obtainAdminAccessToken());

            assertThat(result)
                    .flatExtracting(RevisionDto::changes)
                    .filteredOn(change -> "poster".equals(change.field()))
                    .singleElement()
                    .satisfies(change -> {
                        assertThat(change.type()).isEqualTo(AuditedType.SPEX_DETAILS);
                        assertThat(change.entityId()).isEqualTo(details.getId());
                    });

            assertThat(result)
                    .flatExtracting(RevisionDto::changes)
                    .filteredOn(change -> "year".equals(change.field()))
                    .singleElement()
                    .satisfies(change -> {
                        assertThat(change.type()).isEqualTo(AuditedType.SPEX);
                        assertThat(change.entityId()).isEqualTo(spex.getId());
                    });
        }

        @Test
        void should_serve_the_poster_as_it_was_at_a_revision() {
            final var category = persistSpexCategory("Chalmersspexet");
            final var details = persistSpexDetails(category, "Nobel");
            final var spex = persistSpex(details, "2024");
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            final var creation = retrieveRevisions(AuditedType.SPEX, spex.getId(), obtainAdminAccessToken()).stream()
                    .filter(revision -> revision.type() == AuditedType.SPEX_DETAILS)
                    .toList()
                    .getLast();

            assertThat(creation.changes())
                    .filteredOn(change -> "poster".equals(change.field()))
                    .singleElement()
                    .satisfies(change -> {
                        assertThat(change.binary()).isTrue();
                        assertThat(change.oldValue()).isNull();
                        assertThat(change.newValue()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
                    });

            final byte[] poster = restTestClient
                    .get()
                    .uri("/{type}/{id}/{revision}/binary/{field}", "SPEX", spex.getId(), creation.revision(), "poster")
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.IMAGE_PNG)
                    .expectBody(byte[].class)
                    .returnResult()
                    .getResponseBody();

            assertThat(poster).containsExactly(1, 2, 3);
        }

        @Test
        void should_serve_the_poster_to_someone_permitted_only_on_the_spex() {
            final var category = persistSpexCategory("Chalmersspexet");
            final var details = persistSpexDetails(category, "Nobel");
            final var spex = persistSpex(details, "2024");
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            final var creation = retrieveRevisions(AuditedType.SPEX, spex.getId(), obtainAdminAccessToken()).stream()
                    .filter(revision -> revision.type() == AuditedType.SPEX_DETAILS)
                    .toList()
                    .getLast();

            restTestClient
                    .get()
                    .uri("/{type}/{id}/{revision}/binary/{field}", "SPEX", spex.getId(), creation.revision(), "poster")
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        void should_refuse_to_serve_a_property_that_is_not_binary() {
            final var category = persistSpexCategory("Chalmersspexet");
            final var details = persistSpexDetails(category, "Nobel");
            final var spex = persistSpex(details, "2024");
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            final var creation = retrieveRevisions(AuditedType.SPEX, spex.getId(), obtainAdminAccessToken()).stream()
                    .filter(revision -> revision.type() == AuditedType.SPEX_DETAILS)
                    .toList()
                    .getLast();

            restTestClient
                    .get()
                    .uri("/{type}/{id}/{revision}/binary/{field}", "SPEX", spex.getId(), creation.revision(), "title")
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        void should_still_report_a_change_when_two_targets_share_a_label() {
            final var category = persistSpexCategory("Chalmersspexet");
            final var first = persistSpexDetails(category, "Nobel");
            final var second = persistSpexDetails(category, "Nobel");
            final var spex = persistSpex(first, "2024");
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            spex.setDetails(second);
            spexRepository.save(spex);

            final List<RevisionDto> result = retrieveRevisions(AuditedType.SPEX, spex.getId(), obtainAdminAccessToken());

            assertThat(result.getFirst().changes())
                    .extracting(FieldChangeDto::field)
                    .contains("details");
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
            grantReadPermissionToRoleAdmin(toObjectIdentity(Tag.class, tag.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Tag.class, tag.getId()));

            tag.setName("renamed");
            repository.save(tag);

            final var firstRevision = retrieveRevisions(tag.getId(), obtainAdminAccessToken()).getLast().revision();

            final RestoreResultDto result = Objects.requireNonNull(
                    restTestClient
                            .post()
                            .uri("/{type}/{id}/{revision}/restore", "TAG", tag.getId(), firstRevision)
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
            assertThat(retrieveRevisions(tag.getId(), obtainAdminAccessToken())).hasSize(3);
        }

        @Test
        void should_return_403_when_editor() {
            final var tag = persistTag();
            grantReadPermissionToRoleEditor(toObjectIdentity(Tag.class, tag.getId()));

            restTestClient
                    .post()
                    .uri("/{type}/{id}/{revision}/restore", "TAG", tag.getId(), 1)
                    .header(HttpHeaders.AUTHORIZATION, obtainEditorAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden();
        }
    }

    private List<RevisionDto> retrieveRevisions(final Long id, final String token) {
        return retrieveRevisions(AuditedType.TAG, id, token);
    }

    private List<RevisionDto> retrieveRevisions(final AuditedType type, final Long id, final String token) {
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
        category.setLogo(null);

        return spexCategoryRepository.save(category);
    }

    private SpexDetails persistSpexDetails(final SpexCategory category, final String title) {
        final var details = random.nextObject(SpexDetails.class);

        details.setId(null);
        details.setTitle(title);
        details.setCategory(category);
        details.setPoster(new byte[]{1, 2, 3});
        details.setPosterContentType(MediaType.IMAGE_PNG_VALUE);

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

    private Tag persistTag() {
        final Tag tag = random.nextObject(Tag.class);

        tag.setId(null);

        return repository.save(tag);
    }
}