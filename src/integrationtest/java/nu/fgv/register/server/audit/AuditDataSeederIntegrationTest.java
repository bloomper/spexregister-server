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
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.SpexDetails;
import nu.fgv.register.server.spex.SpexDetailsRepository;
import nu.fgv.register.server.spex.SpexRepository;
import nu.fgv.register.server.spex.category.SpexCategory;
import nu.fgv.register.server.spex.category.SpexCategoryRepository;
import nu.fgv.register.server.spexare.address.AddressRepository;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.tag.TagRepository;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.util.AbstractAuditable;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.data.AuditDataSeeder;
import nu.fgv.register.server.util.randomizer.CountryCodeRandomizer;
import nu.fgv.register.server.util.randomizer.LabelsRandomizer;
import nu.fgv.register.server.util.randomizer.SocialSecurityNumberRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.EmailRandomizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static nu.fgv.register.server.util.security.SecurityUtil.runAsSystem;
import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.inClass;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class AuditDataSeederIntegrationTest extends AbstractIntegrationTest {

    private final EasyRandom random;
    private final AuditDataSeeder seeder;
    private final TagRepository tagRepository;
    private final SpexareRepository spexareRepository;
    private final AddressRepository addressRepository;
    private final TypeRepository typeRepository;
    private final SpexRepository spexRepository;
    private final SpexDetailsRepository spexDetailsRepository;
    private final SpexCategoryRepository spexCategoryRepository;
    private final AuditService auditService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public AuditDataSeederIntegrationTest(final JdbcClient jdbcClient,
                                          final AclCache aclCache,
                                          final Keycloak keycloakAdminClient,
                                          final String keycloakClientId,
                                          final PermissionService permissionService,
                                          final AuditDataSeeder seeder,
                                          final TagRepository tagRepository,
                                          final SpexareRepository spexareRepository,
                                          final AddressRepository addressRepository,
                                          final TypeRepository typeRepository,
                                          final SpexRepository spexRepository,
                                          final SpexDetailsRepository spexDetailsRepository,
                                          final SpexCategoryRepository spexCategoryRepository,
                                          final AuditService auditService,
                                          final ObjectMapper objectMapper) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.seeder = seeder;
        this.tagRepository = tagRepository;
        this.spexareRepository = spexareRepository;
        this.addressRepository = addressRepository;
        this.typeRepository = typeRepository;
        this.spexRepository = spexRepository;
        this.spexDetailsRepository = spexDetailsRepository;
        this.spexCategoryRepository = spexCategoryRepository;
        this.auditService = auditService;

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
        JdbcTestUtils.deleteFromTables(jdbcClient, "address", "type", "spexare", "tag", "spex", "spex_details", "spex_category",
                "address_audit", "type_audit", "spexare_audit", "tag_audit", "spex_audit", "spex_details_audit", "spex_category_audit",
                "revchanges", "revinfo");
    }

    private static final List<String> EDITORS = List.of(
            "anna@spexregister.com",
            "erik@spexregister.com",
            "karin@spexregister.com",
            "lars@spexregister.com",
            "maria@spexregister.com",
            "olof@spexregister.com"
    );

    @Nested
    @DisplayName("Seed baseline")
    class SeedBaselineTests {

        @Test
        void should_give_every_row_a_single_creating_revision() {
            final var tag = persistTag();
            grantReadPermissionToRoleAdmin(toObjectIdentity(Tag.class, tag.getId()));

            runAsSystem(() -> seeder.seedBaseline(jdbcClient));

            final List<RevisionDto> revisions = asAdmin(() -> auditService.findRevisions(AuditedType.TAG, String.valueOf(tag.getId())));

            assertThat(revisions).hasSize(1);
            assertThat(revisions.getFirst().revisionType().name()).isEqualTo("ADD");
            assertThat(revisions.getFirst().modifiedBy()).isEqualTo("system");
        }

        @Test
        void should_not_seed_any_further_history() {
            persistTag();

            runAsSystem(() -> seeder.seedBaseline(jdbcClient));

            final Integer revisions = jdbcClient.sql("SELECT COUNT(*) FROM revinfo").query(Integer.class).single();
            final Integer modifications = jdbcClient.sql("SELECT COUNT(*) FROM tag_audit WHERE revtype <> 0").query(Integer.class).single();

            assertThat(revisions).isEqualTo(1);
            assertThat(modifications).isZero();
        }
    }

    @Nested
    @DisplayName("Seed sample history")
    class SeedTests {

        @Test
        void should_produce_a_readable_multi_revision_history() {
            final var tag = persistTag();
            grantReadPermissionToRoleAdmin(toObjectIdentity(Tag.class, tag.getId()));

            runAsSystem(() -> seeder.seedSampleHistory(jdbcClient, EDITORS));

            final List<RevisionDto> revisions = asAdmin(() -> auditService.findRevisions(AuditedType.TAG, String.valueOf(tag.getId())));

            assertThat(revisions).hasSizeGreaterThan(1);
            assertThat(revisions.getLast().revisionType().name()).isEqualTo("ADD");
            assertThat(revisions.getFirst().changes()).isNotEmpty();
            assertThat(revisions).extracting(RevisionDto::modifiedBy).doesNotContainNull();
            assertThat(revisions.getFirst().modifiedBy()).isIn(EDITORS);
        }

        @Test
        void should_leave_the_live_data_untouched() {
            final var tag = persistTag();
            final var originalName = tag.getName();

            runAsSystem(() -> seeder.seedSampleHistory(jdbcClient, EDITORS));

            assertThat(tagRepository.findById(tag.getId()).orElseThrow().getName()).isEqualTo(originalName);
        }

        @Test
        void should_leave_exactly_one_open_row_per_entity() {
            final var type = persistType();

            IntStream.range(0, 30).forEach(_ -> {
                persistTag();
                final var spexare = persistSpexare();
                persistAddress(type, spexare);
                persistAddress(type, spexare);
            });

            runAsSystem(() -> seeder.seedSampleHistory(jdbcClient, EDITORS));

            List.of("tag_audit", "spexare_audit", "address_audit").forEach(table -> {
                final Integer withSeveralOpenRows = jdbcClient
                        .sql("SELECT COUNT(*) FROM (SELECT id FROM %s WHERE revend IS NULL GROUP BY id HAVING COUNT(*) > 1) duplicates".formatted(table))
                        .query(Integer.class)
                        .single();
                final Integer withoutOpenRow = jdbcClient
                        .sql("SELECT COUNT(*) FROM %s l WHERE NOT EXISTS (SELECT 1 FROM %s a WHERE a.id = l.id AND a.revend IS NULL)".formatted(table.replace("_audit", ""), table))
                        .query(Integer.class)
                        .single();
                final Integer danglingRevends = jdbcClient
                        .sql("""
                                SELECT COUNT(*) FROM %s a
                                 WHERE a.revend IS NOT NULL
                                   AND NOT EXISTS (SELECT 1 FROM %s n WHERE n.id = a.id AND n.rev = a.revend)
                                """.formatted(table, table))
                        .query(Integer.class)
                        .single();

                assertThat(withSeveralOpenRows).as("%s must have at most one open row per id", table).isZero();
                assertThat(withoutOpenRow).as("%s must have an open row for every live row", table).isZero();
                assertThat(danglingRevends).as("%s must not close a row at a revision it has no row for", table).isZero();
            });
        }

        @Test
        void should_seed_deleted_entities_that_are_absent_from_the_live_table() {
            final var type = persistType();
            final var spexare = persistSpexare();
            persistAddress(type, spexare);

            runAsSystem(() -> seeder.seedSampleHistory(jdbcClient, EDITORS));

            final List<Long> deleted = jdbcClient
                    .sql("SELECT DISTINCT a.id FROM address_audit a WHERE a.revtype = 2 AND NOT EXISTS (SELECT 1 FROM address l WHERE l.id = a.id)")
                    .query(Long.class)
                    .list();

            assertThat(deleted).isNotEmpty();

            final List<RevisionDto> revisions = asAdmin(() -> auditService.findRevisions(AuditedType.ADDRESS, String.valueOf(deleted.getFirst())));

            assertThat(revisions).hasSize(2);
            assertThat(revisions.getFirst().revisionType().name()).isEqualTo("DEL");
            assertThat(revisions.getLast().revisionType().name()).isEqualTo("ADD");
        }

        @Test
        void should_not_reuse_deleted_identifiers_for_new_rows() {
            final var type = persistType();
            final var spexare = persistSpexare();
            persistAddress(type, spexare);

            runAsSystem(() -> seeder.seedSampleHistory(jdbcClient, EDITORS));

            final Long highestSeeded = jdbcClient
                    .sql("SELECT MAX(id) FROM address_audit")
                    .query(Long.class)
                    .single();

            final var fresh = persistAddress(type, spexare);

            assertThat(fresh.getId()).isGreaterThan(highestSeeded);
        }
    }

    @Nested
    @DisplayName("Association rendering against seeded history")
    class SeededAssociationTests {

        @Test
        void should_report_one_creation_when_a_spex_and_its_details_share_a_revision() {
            final var category = persistSpexCategory();
            final var details = persistSpexDetails(category, "H. C. Andersen");
            final var spex = persistSpex(details, "2022");
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            runAsSystem(() -> seeder.seedBaseline(jdbcClient));

            final List<RevisionDto> revisions = asAdmin(() -> auditService.findRevisions(AuditedType.SPEX, String.valueOf(spex.getId())));

            assertThat(revisions)
                    .as("a spex and its details created in one revision must read as a single event")
                    .hasSize(1);
            assertThat(revisions.getFirst().revisionType().name()).isEqualTo("ADD");
            assertThat(revisions.getFirst().type()).isEqualTo(AuditedType.SPEX);
            assertThat(revisions.getFirst().changes())
                    .extracting(FieldChangeDto::field)
                    .contains("year", "details", "title", "category");
        }

        @Test
        void should_render_the_label_of_an_association_instead_of_its_id() {
            final var category = persistSpexCategory();
            final var details = persistSpexDetails(category, "Carl von Linné");
            final var spex = persistSpex(details, "2020");
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));

            runAsSystem(() -> seeder.seedSampleHistory(jdbcClient, EDITORS));

            final List<RevisionDto> revisions = asAdmin(() -> auditService.findRevisions(AuditedType.SPEX, String.valueOf(spex.getId())));

            assertThat(revisions).isNotEmpty();
            assertThat(revisions)
                    .flatExtracting(RevisionDto::changes)
                    .filteredOn(change -> "details".equals(change.field()))
                    .isNotEmpty()
                    .allSatisfy(change -> assertThat(change.newValue())
                            .as("an association must render as its label, never as a bare identifier")
                            .startsWith("Carl von Linné")
                            .doesNotMatch("\\d+"));
        }
    }

    @Nested
    @DisplayName("Restore against seeded history")
    class RestoreTests {

        @Test
        void should_restore_a_tag_to_its_seeded_previous_value() {
            final var tag = persistTag();
            final var currentName = tag.getName();
            grantReadPermissionToRoleAdmin(toObjectIdentity(Tag.class, tag.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Tag.class, tag.getId()));

            runAsSystem(() -> seeder.seedSampleHistory(jdbcClient, EDITORS));

            final List<RevisionDto> revisions = asAdmin(() -> auditService.findRevisions(AuditedType.TAG, String.valueOf(tag.getId())));
            final var oldest = revisions.getLast();

            asAdmin(() -> auditService.restore(AuditedType.TAG, String.valueOf(tag.getId()), oldest.revision(), false));

            final var restored = tagRepository.findById(tag.getId()).orElseThrow();

            assertThat(restored.getName()).isNotEqualTo(currentName);
            assertThat(restored.getCreatedBy()).isEqualTo(tag.getCreatedBy());
        }

        @Test
        void should_offer_to_recreate_a_seeded_deleted_address_when_cascading() {
            final var type = persistType();
            final var spexare = persistSpexare();
            persistAddress(type, spexare);
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));

            runAsSystem(() -> seeder.seedSampleHistory(jdbcClient, EDITORS));

            final Long aliveAt = jdbcClient
                    .sql("""
                            SELECT a.rev FROM address_audit a
                             WHERE a.revtype = 0 AND a.spexare_id = ?
                               AND NOT EXISTS (SELECT 1 FROM address l WHERE l.id = a.id)
                             ORDER BY a.rev LIMIT 1
                            """)
                    .param(spexare.getId())
                    .query(Long.class)
                    .optional()
                    .orElse(null);

            assertThat(aliveAt).as("the seeder must produce a deleted address for the spexare").isNotNull();

            final var preview = asAdmin(() -> auditService.preview(AuditedType.SPEXARE, String.valueOf(spexare.getId()), aliveAt, true));

            assertThat(preview.entries())
                    .filteredOn(e -> e.action() == RestoreAction.CREATE)
                    .isNotEmpty();
        }
    }

    private <T> T asAdmin(final Supplier<T> supplier) {
        final AtomicReference<T> result = new AtomicReference<>();

        runAsSystem(() -> result.set(supplier.get()));

        return result.get();
    }

    private SpexCategory persistSpexCategory() {
        final var category = random.nextObject(SpexCategory.class);

        category.setId(null);
        category.setFirstYear("1948");
        category.setLogo(null);

        return spexCategoryRepository.save(category);
    }

    private SpexDetails persistSpexDetails(final SpexCategory category, final String title) {
        final var details = random.nextObject(SpexDetails.class);

        details.setId(null);
        details.setTitle(title);
        details.setCategory(category);
        details.setPoster(null);

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

        return tagRepository.save(tag);
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