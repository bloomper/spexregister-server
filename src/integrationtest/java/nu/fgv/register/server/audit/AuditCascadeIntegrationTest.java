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
import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.settings.TypeRepository;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.address.Address;
import nu.fgv.register.server.spexare.address.AddressRepository;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.util.AbstractAuditable;
import nu.fgv.register.server.util.randomizer.CountryCodeRandomizer;
import nu.fgv.register.server.util.randomizer.LabelsRandomizer;
import nu.fgv.register.server.util.randomizer.SocialSecurityNumberRandomizer;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.HalEmbeddedResponse;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.EmailRandomizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.hibernate.envers.RevisionType;
import org.jspecify.annotations.NonNull;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
class AuditCascadeIntegrationTest extends AbstractIntegrationTest {

    private final EasyRandom random;
    private final SpexareRepository spexareRepository;
    private final AddressRepository addressRepository;
    private final TypeRepository typeRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public AuditCascadeIntegrationTest(final JdbcClient jdbcClient,
                                       final AclCache aclCache,
                                       final Keycloak keycloakAdminClient,
                                       final String keycloakClientId,
                                       final PermissionService permissionService,
                                       final SpexareRepository spexareRepository,
                                       final AddressRepository addressRepository,
                                       final TypeRepository typeRepository,
                                       final ObjectMapper objectMapper) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.spexareRepository = spexareRepository;
        this.addressRepository = addressRepository;
        this.typeRepository = typeRepository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

        parameters
                .randomize(named("labels"), new LabelsRandomizer())
                .randomize(named("emailAddress"), new EmailRandomizer())
                .randomize(named("country"), new CountryCodeRandomizer())
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
                .baseUrl("http://localhost:%s/api/revisions".formatted(localPort))
                .apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
                .build();

        JdbcTestUtils.deleteFromTables(jdbcClient, "address", "type", "spexare",
                "address_audit", "type_audit", "spexare_audit", "revchanges", "revinfo");
    }

    @Nested
    @DisplayName("Related revisions")
    class RelatedRevisionTests {

        @Test
        void should_return_the_history_of_every_address_including_removed_ones() {
            final var type = persistType();
            final var spexare = persistSpexare();
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));

            final var kept = persistAddress(type, spexare);
            final var removed = persistAddress(type, spexare);

            kept.setCity("Göteborg");
            addressRepository.save(kept);
            addressRepository.delete(removed);

            final List<RevisionDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri("/{type}/{id}/related/{relatedType}", "SPEXARE", spexare.getId(), "ADDRESS")
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

            assertThat(result).extracting(RevisionDto::entityId).contains(kept.getId(), removed.getId());
            assertThat(result).extracting(RevisionDto::type).containsOnly(AuditedType.ADDRESS);
            assertThat(result)
                    .filteredOn(revision -> revision.revisionType() == RevisionType.DEL)
                    .extracting(RevisionDto::entityId)
                    .containsExactly(removed.getId());
            assertThat(result)
                    .extracting(RevisionDto::revision)
                    .isSortedAccordingTo(Comparator.reverseOrder());
        }

        @Test
        void should_name_the_entity_each_revision_belongs_to() {
            final var type = persistType();
            final var spexare = persistSpexare();
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));

            final var first = persistAddress(type, spexare);

            first.setStreetAddress("Kungsgatan 1");
            addressRepository.save(first);

            final var second = persistAddress(type, spexare);

            second.setStreetAddress("Drottninggatan 2");
            addressRepository.save(second);

            final List<RevisionDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri("/{type}/{id}/related/{relatedType}", "SPEXARE", spexare.getId(), "ADDRESS")
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

            assertThat(result).extracting(RevisionDto::entityLabel).contains("Kungsgatan 1", "Drottninggatan 2");
        }

        @Test
        void should_reach_entities_nested_below_an_activity() {
            final var spexare = persistSpexare();
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));

            final List<RevisionDto> result = Objects.requireNonNull(
                            restTestClient
                                    .get()
                                    .uri("/{type}/{id}/related/{relatedType}", "SPEXARE", spexare.getId(), "ACTOR")
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

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Restoring a removed entity")
    class RemovedEntityTests {

        @Test
        void should_not_be_possible_to_restore_an_entity_that_no_longer_exists() {
            final var type = persistType();
            final var spexare = persistSpexare();
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));

            final var removed = persistAddress(type, spexare);
            final long creation = latestRevision();

            addressRepository.delete(removed);

            restTestClient
                    .post()
                    .uri("/{type}/{id}/{revision}/restore", "ADDRESS", removed.getId(), creation)
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    @DisplayName("Cascade restore")
    class CascadeRestoreTests {

        @Test
        void should_recreate_removed_child_and_delete_added_child() {
            final var type = persistType();
            final var spexare = persistSpexare();
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));

            final var kept = persistAddress(type, spexare);
            final var removed = persistAddress(type, spexare);

            final long target = latestRevision();

            addressRepository.delete(removed);
            final var added = persistAddress(type, spexare);

            final RestorePreviewDto preview = Objects.requireNonNull(
                    restTestClient
                            .get()
                            .uri("/{type}/{id}/{revision}/preview?cascade=true", "SPEXARE", spexare.getId(), target)
                            .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .apiVersion("1.0")
                            .exchange()
                            .expectStatus().isOk()
                            .expectBody(RestorePreviewDto.class)
                            .returnResult()
                            .getResponseBody());

            assertThat(preview.entries())
                    .filteredOn(e -> e.action() == RestoreAction.CREATE)
                    .extracting(RestorePreviewEntryDto::id)
                    .containsExactly(removed.getId());
            assertThat(preview.entries())
                    .filteredOn(e -> e.action() == RestoreAction.DELETE)
                    .extracting(RestorePreviewEntryDto::id)
                    .containsExactly(added.getId());

            final RestoreResultDto result = Objects.requireNonNull(
                    restTestClient
                            .post()
                            .uri("/{type}/{id}/{revision}/restore?cascade=true", "SPEXARE", spexare.getId(), target)
                            .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .apiVersion("1.0")
                            .exchange()
                            .expectStatus().isOk()
                            .expectBody(RestoreResultDto.class)
                            .returnResult()
                            .getResponseBody());

            assertThat(result.created()).isEqualTo(1);
            assertThat(result.deleted()).isEqualTo(1);
            assertThat(result.warnings())
                    .extracting(RestoreWarningDto::code, RestoreWarningDto::previousId)
                    .contains(org.assertj.core.api.Assertions.tuple(RestoreWarningDto.ID_REASSIGNED, removed.getId()));

            final var remaining = addressRepository.findAll();

            assertThat(remaining).hasSize(2);
            assertThat(remaining).extracting(Address::getId).contains(kept.getId());
            assertThat(remaining).extracting(Address::getId).doesNotContain(added.getId(), removed.getId());
        }
    }

    private long latestRevision() {
        return Objects.requireNonNull(jdbcClient.sql("SELECT MAX(id) FROM revinfo").query(Long.class).single());
    }

    private Type persistType() {
        final var type = random.nextObject(Type.class);

        type.setType(TypeType.ADDRESS);

        return typeRepository.save(type);
    }

    private Spexare persistSpexare() {
        final var spexare = random.nextObject(Spexare.class);

        spexare.setId(null);

        return spexareRepository.save(spexare);
    }

    private Address persistAddress(final Type type, final Spexare spexare) {
        final var address = random.nextObject(Address.class);

        address.setId(null);
        address.setType(type);
        address.setSpexare(spexare);

        return addressRepository.save(address);
    }
}