package nu.fgv.register.server.imports;

import nu.fgv.register.server.imports.ImportEngine.ItemResult;
import nu.fgv.register.server.imports.ImportEngine.Outcome;
import nu.fgv.register.server.spexare.SpexareService;
import nu.fgv.register.server.spexare.SpexareUpdateDto;
import nu.fgv.register.server.sync.SyncService;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.BaseDataFixtures;
import nu.fgv.register.server.util.security.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The import API that spexregister-migration builds the first database through (spexregister-spec, docs/migration.md,
 * 3): records by source key, again without duplicates, left alone once changed, refused for FGV memberships, and only
 * for IMPORT.
 */
class ImportIntegrationTest extends AbstractIntegrationTest {

    private static final String PREFIX = "test:";

    private final SpexregisterImport importer;
    private final SpexareService spexareService;
    private final BaseDataFixtures baseData;
    @Value("${spring.graphql.http.path}")
    private String graphqlPath;
    private UUID spex;
    private UUID task;

    @Autowired
    ImportIntegrationTest(final JdbcClient jdbcClient, final Keycloak keycloakAdminClient, final String keycloakClientId,
                          final ObjectMapper objectMapper, final SpexregisterImport importer, final SpexareService spexareService,
                          final SyncService sync) {
        super(jdbcClient, keycloakAdminClient, keycloakClientId, objectMapper);
        this.importer = importer;
        this.spexareService = spexareService;
        this.baseData = new BaseDataFixtures(sync);
    }

    @BeforeEach
    void setUp() {
        for (final String table : List.of("import_key", "tagging", "actor", "task_activity", "spex_activity", "activity", "address",
                "membership", "medlemskap_notification", "consent", "toggle", "news", "tag")) {
            jdbcClient.sql("DELETE FROM " + table).update();
        }
        jdbcClient.sql("UPDATE spexare SET partner_id = NULL").update();
        jdbcClient.sql("UPDATE user SET spexare_id = NULL").update();
        jdbcClient.sql("DELETE FROM permission_grant").update();
        jdbcClient.sql("DELETE FROM spexare").update();

        final String suffix = UUID.randomUUID().toString().substring(0, 8);

        spex = baseData.spex("1987", "Montgomery " + suffix, baseData.spexCategory("Chalmersspexet " + suffix, "1948"));
        task = baseData.task("Trumpet " + suffix, baseData.taskCategory("Orkester " + suffix, false));
    }

    private static ImportRecords.Spexare spexare(final String key, final String firstName, final List<ImportRecords.Membership> memberships) {
        return new ImportRecords.Spexare(key, firstName, "Garbo", "Greta", "19811228-9874", false, true, "E81", null, null,
                List.of(new ImportRecords.Address("HOME", "Storgatan 1", "41101", "Göteborg", "SE", null, null, "greta@example.org")),
                List.of(new ImportRecords.TypeValue("CIRCULARS", true)),
                List.of(new ImportRecords.TypeValue("CHALMERS_STUDENT", false)),
                List.of(PREFIX + "tag:1"),
                memberships);
    }

    private ImportRecords.Batch batch(final ImportRecords.Spexare... spexare) {
        return new ImportRecords.Batch(
                List.of(new ImportRecords.Tag(PREFIX + "tag:1", "Styrelse")),
                List.of(spexare),
                null,
                List.of(new ImportRecords.Activity(PREFIX + "activity:1", spexare[0].sourceKey(), spex, task,
                        List.of(new ImportRecords.Actor("B1", "Kungen", null)))),
                List.of(new ImportRecords.News(PREFIX + "news:1", "Välkommen", "Till det nya registret", null, null)),
                null);
    }

    private static List<Outcome> outcomes(final List<ItemResult> results) {
        return results.stream().map(ItemResult::outcome).toList();
    }

    private long count(final String table) {
        return jdbcClient.sql("SELECT COUNT(*) FROM " + table).query(Long.class).single();
    }

    @Test
    void should_create_then_leave_unchanged_records_alone() {
        final List<ItemResult> first = importer.run(batch(spexare(PREFIX + "spexare:1", "Greta", List.of(new ImportRecords.Membership("CING", "2020")))), false);

        assertThat(outcomes(first)).containsOnly(Outcome.CREATED);
        assertThat(count("spexare")).isEqualTo(1);
        assertThat(count("address")).isEqualTo(1);
        assertThat(count("tagging")).isEqualTo(1);
        assertThat(count("membership")).isEqualTo(1);
        assertThat(count("task_activity")).isEqualTo(1);
        assertThat(count("actor")).isEqualTo(1);
        assertThat(jdbcClient.sql("SELECT social_security_number FROM spexare").query(String.class).single())
                .as("encrypted by the register").isNotEqualTo("19811228-9874");

        final List<ItemResult> again = importer.run(batch(spexare(PREFIX + "spexare:1", "Greta", List.of(new ImportRecords.Membership("CING", "2020")))), false);

        assertThat(outcomes(again)).containsOnly(Outcome.UNCHANGED);
        assertThat(count("spexare")).isEqualTo(1);
        assertThat(count("task_activity")).isEqualTo(1);
    }

    @Test
    void should_update_what_it_made_but_keep_what_someone_changed() {
        importer.run(batch(spexare(PREFIX + "spexare:1", "Greta", List.of())), false);

        final List<ItemResult> updated = importer.run(batch(spexare(PREFIX + "spexare:1", "Gretha", List.of())), false);

        assertThat(updated.stream().filter(r -> r.kind().equals("SPEXARE")).map(ItemResult::outcome)).as(updated.toString()).containsExactly(Outcome.UPDATED);
        assertThat(count("address")).as("replaced, not added").isEqualTo(1);

        final UUID id = updated.stream().filter(r -> r.kind().equals("SPEXARE")).findFirst().orElseThrow().id();

        SecurityUtil.runAsSystem(() -> spexareService.partialUpdate(SpexareUpdateDto.builder().id(id).firstName("Greta Lovisa").build()));

        final List<ItemResult> kept = importer.run(batch(spexare(PREFIX + "spexare:1", "Gretha", List.of())), false);

        assertThat(kept.stream().filter(r -> r.kind().equals("SPEXARE")).map(ItemResult::outcome)).containsExactly(Outcome.KEPT);
        assertThat(jdbcClient.sql("SELECT first_name FROM spexare").query(String.class).single()).isEqualTo("Greta Lovisa");
    }

    @Test
    void should_write_nothing_in_a_dry_run() {
        final List<ItemResult> results = importer.run(batch(spexare(PREFIX + "spexare:1", "Greta", List.of())), true);

        assertThat(outcomes(results)).containsOnly(Outcome.CREATED);
        assertThat(count("spexare")).isZero();
        assertThat(count("tag")).isZero();
        assertThat(count("import_key")).isZero();
    }

    @Test
    void should_refuse_fgv_memberships_which_are_medlemskaps() {
        final List<ItemResult> results = importer.run(batch(spexare(PREFIX + "spexare:1", "Greta", List.of(new ImportRecords.Membership("FGV", "2020")))), false);

        assertThat(results.stream().filter(r -> r.kind().equals("SPEXARE")).map(ItemResult::outcome)).containsExactly(Outcome.FAILED);
        assertThat(count("spexare")).isZero();
        assertThat(count("membership")).isZero();
    }

    @Test
    void should_share_the_activity_of_a_spex_between_sources() {
        importer.run(batch(spexare(PREFIX + "spexare:1", "Greta", List.of())), false);

        // A row of the archive's Poster i Spexet: the same spexare, spex and task, without actors
        final List<ItemResult> archive = importer.run(new ImportRecords.Batch(null, null, null,
                List.of(new ImportRecords.Activity(PREFIX + "archive:1", PREFIX + "spexare:1", spex, task, null)), null, null), false);

        assertThat(outcomes(archive)).containsExactly(Outcome.CREATED);
        assertThat(count("activity")).isEqualTo(1);
        assertThat(count("task_activity")).isEqualTo(1);
        assertThat(count("actor")).as("left alone").isEqualTo(1);
    }

    @Test
    void should_link_partners_and_forget_what_nobody_changed() {
        importer.run(new ImportRecords.Batch(List.of(new ImportRecords.Tag(PREFIX + "tag:1", "Styrelse")),
                List.of(spexare(PREFIX + "spexare:1", "Greta", List.of()), spexare(PREFIX + "spexare:2", "Ingrid", List.of())),
                List.of(new ImportRecords.Partner(PREFIX + "partner:1", PREFIX + "spexare:1", PREFIX + "spexare:2")),
                null, null, null), false);

        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM spexare WHERE partner_id IS NOT NULL").query(Long.class).single()).isEqualTo(2);

        assertThat(importer.forget(PREFIX)).as(jdbcClient.sql("SELECT CONCAT(record_kind, ' ', source_key) FROM import_key").query(String.class).list().toString()).isEqualTo(4);
        assertThat(count("spexare")).isZero();
        assertThat(count("tag")).isZero();
        assertThat(count("import_key")).isZero();
    }

    @Test
    void should_register_the_users_keycloak_account_with_its_spexare() {
        final String externalId = keycloakAdminClient.realm(keycloakRealm).users().searchByEmail(TEST_USER, true).getFirst().getId();
        final List<ItemResult> results = importer.run(new ImportRecords.Batch(List.of(new ImportRecords.Tag(PREFIX + "tag:1", "Styrelse")),
                List.of(spexare(PREFIX + "spexare:1", "Greta", List.of())), null, null, null,
                List.of(new ImportRecords.User(PREFIX + "user:1", externalId, "ACTIVE", PREFIX + "spexare:1"))), false);

        assertThat(outcomes(results)).as(results.toString()).containsOnly(Outcome.CREATED);
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM user WHERE external_id = :e").param("e", externalId).query(Long.class).single())
                .as("one user per account, also when the sync had added it").isEqualTo(1);
        assertThat(jdbcClient.sql("SELECT s.first_name FROM user u JOIN spexare s ON s.id = u.spexare_id WHERE u.external_id = :e")
                .param("e", externalId).query(String.class).single()).isEqualTo("Greta");
    }

    @Test
    void should_be_refused_without_the_import_role() {
        HttpGraphQlTester.create(WebTestClient.bindToServer()
                        .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath))
                        .defaultHeader(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                        .build())
                .document("mutation { importForget(sourcePrefix: \"x\") }")
                .execute()
                .errors().satisfy(errors -> assertThat(errors).isNotEmpty());
    }
}
