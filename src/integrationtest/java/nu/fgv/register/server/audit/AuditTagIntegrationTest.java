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

import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.tag.TagRepository;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.util.AbstractAuditable;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.HalEmbeddedResponse;
import nu.fgv.register.server.util.randomizer.RandomizerSupport;
import nu.fgv.register.server.util.randomizer.SocialSecurityNumberRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.jeasy.random.FieldPredicates.inClass;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;

/**
 * A spexare's tags in their history, the change log and a restore.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
class AuditTagIntegrationTest extends AbstractIntegrationTest {

    private final EasyRandom random;
    private final SpexareRepository spexareRepository;
    private final TagRepository tagRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public AuditTagIntegrationTest(final JdbcClient jdbcClient,
                                   final Keycloak keycloakAdminClient,
                                   final String keycloakClientId,
                                   final SpexareRepository spexareRepository,
                                   final TagRepository tagRepository,
                                   final ObjectMapper objectMapper) {
        super(jdbcClient, keycloakAdminClient, keycloakClientId, objectMapper);
        this.spexareRepository = spexareRepository;
        this.tagRepository = tagRepository;

        final EasyRandomParameters parameters = RandomizerSupport.parameters();

        parameters
                .randomize(named("socialSecurityNumber"), new SocialSecurityNumberRandomizer())
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

    @BeforeEach
    void setUp() {
        restTestClient = RestTestClient
                .bindToServer()
                .baseUrl("http://localhost:%s/api".formatted(localPort))
                .apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
                .build();

        jdbcClient.sql("UPDATE spexare SET partner_id = NULL").update();
        JdbcTestUtils.deleteFromTables(jdbcClient, "tagging", "tag", "spexare",
                "tagging_audit", "tag_audit", "spexare_audit", "revchanges", "revinfo");
    }

    private long latestRevision() {
        return Objects.requireNonNull(jdbcClient.sql("SELECT MAX(id) FROM revinfo").query(Long.class).single());
    }

    private Spexare persistSpexare() {
        final var spexare = random.nextObject(Spexare.class);

        spexare.setId(null);

        return spexareRepository.save(spexare);
    }

    private Tag persistTag(final String name) {
        final var tag = random.nextObject(Tag.class);

        tag.setId(null);
        tag.setName(name);

        return tagRepository.save(tag);
    }

    private void tag(final Spexare spexare, final Tag tag) {
        restTestClient
                .post()
                .uri("/spexare/{spexareId}/taggings/{tagId}", spexare.getUuid(), tag.getUuid())
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .apiVersion("1.0")
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    private void untag(final Spexare spexare, final Tag tag) {
        restTestClient
                .delete()
                .uri("/spexare/{spexareId}/taggings/{tagId}", spexare.getUuid(), tag.getUuid())
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .apiVersion("1.0")
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    private List<String> tagNamesOf(final Spexare spexare) {
        return jdbcClient.sql("SELECT t.name FROM tagging g JOIN tag t ON t.id = g.tag_id WHERE g.spexare_id = :id ORDER BY t.name")
                .param("id", spexare.getId())
                .query(String.class)
                .list();
    }

    private List<RevisionDto> revisionsOf(final Spexare spexare) {
        return Objects.requireNonNull(
                        restTestClient
                                .get()
                                .uri("/revisions/{type}/{id}", "SPEXARE", spexare.getUuid())
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
    }

    private RestoreResultDto restore(final Spexare spexare, final long revision) {
        return Objects.requireNonNull(
                restTestClient
                        .post()
                        .uri("/revisions/{type}/{id}/{revision}/restore", "SPEXARE", spexare.getUuid(), revision)
                        .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .apiVersion("1.0")
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(RestoreResultDto.class)
                        .returnResult()
                        .getResponseBody());
    }

    @Test
    void should_show_tag_changes_in_the_history() {
        final var spexare = persistSpexare();
        final var styrelse = persistTag("Styrelse");
        final var kassor = persistTag("Kassör");

        tag(spexare, styrelse);
        tag(spexare, kassor);
        untag(spexare, styrelse);

        final List<FieldChangeDto> tagChanges = revisionsOf(spexare).stream()
                .flatMap(revision -> revision.changes().stream())
                .filter(change -> "tags".equals(change.field()))
                .toList();

        assertThat(tagChanges)
                .extracting(FieldChangeDto::oldValue, FieldChangeDto::newValue)
                .containsExactly(
                        tuple("Kassör, Styrelse", "Kassör"),
                        tuple("Styrelse", "Kassör, Styrelse"),
                        tuple(null, "Styrelse"));
    }

    @Test
    void should_show_tag_changes_in_the_revision_detail() {
        final var spexare = persistSpexare();
        final var styrelse = persistTag("Styrelse");

        tag(spexare, styrelse);

        final RevisionDetailDto detail = Objects.requireNonNull(
                restTestClient
                        .get()
                        .uri("/revisions/detail/{revision}", latestRevision())
                        .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                        .apiVersion("1.0")
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(RevisionDetailDto.class)
                        .returnResult()
                        .getResponseBody());

        assertThat(detail.entities())
                .filteredOn(entity -> entity.type() == AuditedType.SPEXARE)
                .flatExtracting(RevisionEntityChangeDto::changes)
                .extracting(FieldChangeDto::field, FieldChangeDto::newValue)
                .contains(tuple("tags", "Styrelse"));
    }

    @Test
    void should_preview_and_restore_the_tags_of_a_revision() {
        final var spexare = persistSpexare();
        final var styrelse = persistTag("Styrelse");
        final var kassor = persistTag("Kassör");
        final var revisor = persistTag("Revisor");

        tag(spexare, styrelse);
        tag(spexare, kassor);

        final long target = latestRevision();

        untag(spexare, styrelse);
        tag(spexare, revisor);

        final RestorePreviewDto preview = Objects.requireNonNull(
                restTestClient
                        .get()
                        .uri("/revisions/{type}/{id}/{revision}/preview", "SPEXARE", spexare.getUuid(), target)
                        .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                        .apiVersion("1.0")
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(RestorePreviewDto.class)
                        .returnResult()
                        .getResponseBody());

        assertThat(preview.entries())
                .flatExtracting(RestorePreviewEntryDto::changes)
                .extracting(FieldChangeDto::field, FieldChangeDto::oldValue, FieldChangeDto::newValue)
                .containsExactly(tuple("tags", "Kassör, Revisor", "Kassör, Styrelse"));

        final RestoreResultDto result = restore(spexare, target);

        assertThat(result.warnings()).isEmpty();
        assertThat(tagNamesOf(spexare)).containsExactly("Kassör", "Styrelse");
        assertThat(revisionsOf(spexare).getFirst().changes())
                .extracting(FieldChangeDto::field, FieldChangeDto::oldValue, FieldChangeDto::newValue)
                .containsExactly(tuple("tags", "Kassör, Revisor", "Kassör, Styrelse"));
    }

    @Test
    void should_leave_out_a_tag_that_no_longer_exists() {
        final var spexare = persistSpexare();
        final var styrelse = persistTag("Styrelse");
        final var gone = persistTag("Borta");

        tag(spexare, styrelse);
        tag(spexare, gone);

        final long target = latestRevision();

        untag(spexare, styrelse);
        untag(spexare, gone);
        tagRepository.delete(gone);

        final RestoreResultDto result = restore(spexare, target);

        assertThat(result.warnings()).extracting(RestoreWarningDto::code).containsExactly("REFERENCE_MISSING");
        assertThat(tagNamesOf(spexare)).containsExactly("Styrelse");
    }
}
