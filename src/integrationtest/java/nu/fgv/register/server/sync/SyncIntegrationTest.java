package nu.fgv.register.server.sync;

import nu.fgv.register.server.spexare.activity.task.actor.ActorCharacterService;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.BaseDataFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spexregister's copy of base data, written only by the sync of Spexipedia's change feed, and the SPEXIPEDIA role that
 * Spexipedia reads people with (spexregister-spec, docs/base-data-and-memberships.md, sections 2, 3, 4 and 6).
 */
class SyncIntegrationTest extends AbstractIntegrationTest {

    private final SyncService sync;
    private final ActorCharacterService actorCharacterService;
    private BaseDataFixtures fixtures;
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;
    @Value("${spring.graphql.http.path}")
    private String graphqlPath;

    @Autowired
    SyncIntegrationTest(final JdbcClient jdbcClient, final Keycloak keycloakAdminClient, final String keycloakClientId,
                        final ObjectMapper objectMapper, final SyncService sync, final ActorCharacterService actorCharacterService) {
        super(jdbcClient, keycloakAdminClient, keycloakClientId, objectMapper);
        this.sync = sync;
        this.actorCharacterService = actorCharacterService;
    }

    @BeforeEach
    void setUp() {
        for (final String table : List.of("actor", "task_activity", "spex_activity", "activity", "address", "spexare",
                "spex_character", "task", "task_category")) {
            jdbcClient.sql("DELETE FROM " + table).update();
        }
        jdbcClient.sql("UPDATE spex SET parent_id = NULL").update();
        jdbcClient.sql("DELETE FROM permission_grant").update();
        jdbcClient.sql("DELETE FROM spex").update();
        jdbcClient.sql("DELETE FROM spex_details").update();
        jdbcClient.sql("DELETE FROM spex_category").update();
        fixtures = new BaseDataFixtures(sync);
    }

    private HttpGraphQlTester graphQl(final String authorization) {
        return HttpGraphQlTester.create(WebTestClient.bindToServer()
                .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath))
                .defaultHeader(HttpHeaders.AUTHORIZATION, authorization)
                .build());
    }

    private long key(final String table, final UUID id) {
        return jdbcClient.sql("SELECT id FROM " + table + " WHERE uuid = :u").param("u", id.toString()).query(Long.class).single();
    }

    private long spexare(final String first, final String nickname, final boolean published) {
        jdbcClient.sql("""
                        INSERT INTO spexare (uuid, first_name, last_name, nick_name, deceased, published, created_by, created_at, version)
                        VALUES (UUID(), :f, 'Testsson', :n, 0, :p, 'test', NOW(), 0)""")
                .param("f", first).param("n", nickname).param("p", published).update();
        return jdbcClient.sql("SELECT id FROM spexare WHERE first_name = :f").param("f", first).query(Long.class).single();
    }

    private long activity(final long spexare, final UUID spex) {
        jdbcClient.sql("INSERT INTO activity (uuid, spexare_id, created_by, created_at, version) VALUES (UUID(), :s, 'test', NOW(), 0)")
                .param("s", spexare).update();

        final long activity = jdbcClient.sql("SELECT MAX(id) FROM activity").query(Long.class).single();

        jdbcClient.sql("INSERT INTO spex_activity (uuid, activity_id, spex_id, created_by, created_at, version) VALUES (UUID(), :a, :s, 'test', NOW(), 0)")
                .param("a", activity).param("s", key("spex", spex)).update();
        return activity;
    }

    private long actor(final long activity, final UUID task, final String role) {
        jdbcClient.sql("INSERT INTO task_activity (uuid, activity_id, task_id, created_by, created_at, version) VALUES (UUID(), :a, :t, 'test', NOW(), 0)")
                .param("a", activity).param("t", key("task", task)).update();

        final long taskActivity = jdbcClient.sql("SELECT MAX(id) FROM task_activity").query(Long.class).single();

        jdbcClient.sql("INSERT INTO actor (uuid, task_activity_id, vocal_id, role, created_by, created_at, version) VALUES (UUID(), :t, 'UNKNOWN', :r, 'test', NOW(), 0)")
                .param("t", taskActivity).param("r", role).update();
        return jdbcClient.sql("SELECT MAX(id) FROM actor").query(Long.class).single();
    }

    @Test
    void should_keep_spexipedias_ids_and_share_an_originals_title_with_its_revivals() {
        final UUID category = fixtures.spexCategory("Chalmersspexet", "1948");
        final UUID original = fixtures.spex("1984", "Lasse-Maja", category);
        final UUID revival = fixtures.revival(original, "2013", "Lasse-Maja");

        sync.applyAll(List.of(new FeedChange("100", "SPEX", original, "UPSERT", null, null,
                new FeedChange.Spex(original, "1984", "Lasse-Maja!", category, null, "https://spexipedia.test/media/p.jpg", false), null, null, null)));

        graphQl(obtainUserAccessToken())
                .document("query($id: ID!) { spex(id: $id) { title year revival posterUrl retired category { name } revivals { id year } } }")
                .variable("id", original).execute().errors().verify()
                .path("spex.title").entity(String.class).isEqualTo("Lasse-Maja!")
                .path("spex.posterUrl").entity(String.class).isEqualTo("https://spexipedia.test/media/p.jpg")
                .path("spex.category.name").entity(String.class).isEqualTo("Chalmersspexet")
                .path("spex.revivals[0].id").entity(String.class).isEqualTo(revival.toString())
                .path("spex.revivals[0].year").entity(String.class).isEqualTo("2013");
        graphQl(obtainUserAccessToken()).document("query($id: ID!) { spex(id: $id) { title revival } }").variable("id", revival).execute()
                .path("spex.title").entity(String.class).isEqualTo("Lasse-Maja!")
                .path("spex.revival").entity(Boolean.class).isEqualTo(true);
    }

    @Test
    void should_mark_a_retired_record_and_keep_it() {
        final UUID spex = fixtures.spex("1999", "Felregistrerat", null);

        sync.applyAll(List.of(new FeedChange("100", "SPEX", spex, "RETIRE", null, null,
                new FeedChange.Spex(spex, "1999", "Felregistrerat", null, null, null, true), null, null, null)));

        graphQl(obtainUserAccessToken()).document("query($id: ID!) { spex(id: $id) { retired } }").variable("id", spex).execute()
                .path("spex.retired").entity(Boolean.class).isEqualTo(true);
    }

    @Test
    void should_move_activities_to_the_record_a_spex_was_merged_into() {
        final UUID kept = fixtures.spex("1984", "Lasse-Maja", null);
        final UUID duplicate = fixtures.spex("1985", "Lasse Maja", null);
        final long activity = activity(spexare("Doris", null, true), duplicate);

        sync.applyAll(List.of(new FeedChange("100", "SPEX", duplicate, "MERGE", kept, null, null, null, null, null)));

        assertThat(jdbcClient.sql("SELECT spex_id FROM spex_activity WHERE activity_id = :a").param("a", activity).query(Long.class).single())
                .isEqualTo(key("spex", kept));
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM spex WHERE uuid = :u").param("u", duplicate.toString()).query(Long.class).single()).isZero();
    }

    @Test
    void should_link_actors_to_the_character_named_by_their_role() {
        final UUID original = fixtures.spex("1984", "Lasse-Maja", null);
        final UUID revival = fixtures.revival(original, "2013", "Lasse-Maja");
        final UUID ensemble = fixtures.taskCategory("Ensemble", true);
        final UUID actorTask = fixtures.task("Skådespelare", ensemble);
        final UUID character = fixtures.character(original, "Lasse-Maja", 1);
        final long inRevival = actor(activity(spexare("Erik", null, true), revival), actorTask, " lasse  MAJA ");
        final long unknown = actor(activity(spexare("Fia", null, true), original), actorTask, "Turisten");

        assertThat(actorCharacterService.linkByRole()).isEqualTo(1);
        assertThat(jdbcClient.sql("SELECT character_id FROM actor WHERE id = :a").param("a", inRevival).query(Long.class).single())
                .isEqualTo(key("spex_character", character));
        assertThat(jdbcClient.sql("SELECT character_id FROM actor WHERE id = :a").param("a", unknown).query(Long.class).optional()).isEmpty();
    }

    @Test
    void should_give_spexipedia_published_spexare_only_and_names_only_when_asked() {
        final UUID spex = fixtures.spex("1984", "Lasse-Maja", null);
        final UUID ensemble = fixtures.taskCategory("Ensemble", true);
        final UUID actorTask = fixtures.task("Skådespelare", ensemble);
        final UUID character = fixtures.character(spex, "Lasse-Maja", 1);

        actor(activity(spexare("Bertil", "Basen", true), spex), actorTask, "Lasse-Maja");
        activity(spexare("Greta", null, false), spex);
        actorCharacterService.linkByRole();

        final String token = spexipediaToken();
        final String query = """
                query($ids: [ID!]!, $names: Boolean!) {
                    spexipediaParticipants(spexIds: $ids, names: $names) { nickname name spexId taskIds characterIds }
                }""";

        graphQl(token).document(query).variable("ids", List.of(spex)).variable("names", false).execute().errors().verify()
                .path("spexipediaParticipants[*].nickname").entityList(String.class).containsExactly("Basen")
                .path("spexipediaParticipants[0].name").valueIsNull()
                .path("spexipediaParticipants[0].taskIds[0]").entity(String.class).isEqualTo(actorTask.toString())
                .path("spexipediaParticipants[0].characterIds[0]").entity(String.class).isEqualTo(character.toString());
        graphQl(token).document(query).variable("ids", List.of(spex)).variable("names", true).execute().errors().verify()
                .path("spexipediaParticipants[0].name").entity(String.class).isEqualTo("Bertil Testsson");
        graphQl(token).document("{ spexipediaSpexareSearch(q: \"greta\") { id } }").execute().errors().verify()
                .path("spexipediaSpexareSearch").entityList(Object.class).hasSize(0);
        graphQl(obtainAdminAccessToken()).document(query).variable("ids", List.of(spex)).variable("names", true).execute()
                .errors().satisfy(errors -> assertThat(errors).isNotEmpty());
    }

    /**
     * fgv-spexipedia-service's token, by client credentials; its secret is in the imported test realm.
     */
    private String spexipediaToken() {
        final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add("grant_type", "client_credentials");
        form.add("client_id", "fgv-spexipedia-service");
        form.add("client_secret", secret("fgv-spexipedia-service"));

        @SuppressWarnings("unchecked") final Map<String, Object> response = WebClient.builder().build()
                .post()
                .uri(issuerUri + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return "Bearer " + response.get("access_token");
    }

    private String secret(final String clientId) {
        try {
            final JsonNode realm = objectMapper.readTree(new ClassPathResource("keycloak/fgv.json").getContentAsString(StandardCharsets.UTF_8));

            for (final JsonNode client : realm.get("clients")) {
                if (clientId.equals(client.get("clientId").asString())) {
                    return client.get("secret").asString();
                }
            }
            throw new IllegalStateException("No client " + clientId);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
