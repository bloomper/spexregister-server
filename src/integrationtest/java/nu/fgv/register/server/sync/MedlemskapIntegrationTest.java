package nu.fgv.register.server.sync;

import nu.fgv.register.server.spexare.SpexareCreateDto;
import nu.fgv.register.server.spexare.SpexareService;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.security.SecurityUtil;
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
 * FGV memberships are Medlemskap's: a read-only copy here, written only by the sync of Medlemskap's feed, plus the
 * MEMBERSHIP role that Medlemskap reads spexare with and the telling of Medlemskap about deleted spexare
 * (spexregister-spec, docs/base-data-and-memberships.md, sections 5 and 6).
 */
class MedlemskapIntegrationTest extends AbstractIntegrationTest {

    private final MembershipSyncService sync;
    private final SpexareService spexareService;
    private final MedlemskapNotifier notifier;
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;
    @Value("${spring.graphql.http.path}")
    private String graphqlPath;

    @Autowired
    MedlemskapIntegrationTest(final JdbcClient jdbcClient, final Keycloak keycloakAdminClient, final String keycloakClientId,
                              final ObjectMapper objectMapper, final MembershipSyncService sync, final SpexareService spexareService,
                              final MedlemskapNotifier notifier) {
        super(jdbcClient, keycloakAdminClient, keycloakClientId, objectMapper);
        this.sync = sync;
        this.spexareService = spexareService;
        this.notifier = notifier;
    }

    @BeforeEach
    void setUp() {
        for (final String table : List.of("tagging", "actor", "task_activity", "spex_activity", "activity", "address", "membership",
                "medlemskap_notification", "consent", "toggle")) {
            jdbcClient.sql("DELETE FROM " + table).update();
        }
        jdbcClient.sql("UPDATE user SET spexare_id = NULL").update();
        jdbcClient.sql("DELETE FROM permission_grant").update();
        jdbcClient.sql("DELETE FROM spexare").update();
        for (final String[] type : List.of(new String[]{"FGV", "MEMBERSHIP"}, new String[]{"CING", "MEMBERSHIP"}, new String[]{"HOME", "ADDRESS"})) {
            jdbcClient.sql("INSERT IGNORE INTO type (id, type, labels, created_by, created_at, version) VALUES (:id, :type, '{\"en\":\"x\",\"sv\":\"x\"}', 'test', NOW(), 0)")
                    .param("id", type[0]).param("type", type[1]).update();
        }
    }

    private HttpGraphQlTester graphQl(final String authorization) {
        return HttpGraphQlTester.create(WebTestClient.bindToServer()
                .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath))
                .defaultHeader(HttpHeaders.AUTHORIZATION, authorization)
                .build());
    }

    private UUID spexare(final String first, final String last, final String email) {
        final UUID[] created = new UUID[1];

        // Through the service, so that the audit history is there too
        SecurityUtil.runAsSystem(() -> created[0] = spexareService.create(SpexareCreateDto.builder()
                .firstName(first).lastName(last).nickName("Nick").deceased(false).published(true).build()).getId());

        final long id = jdbcClient.sql("SELECT id FROM spexare WHERE uuid = :u").param("u", created[0].toString()).query(Long.class).single();

        if (email != null) {
            jdbcClient.sql("""
                            INSERT INTO address (uuid, street_address, postal_code, city, country, email_address, type_id, spexare_id, created_by, created_at, version)
                            VALUES (UUID(), 'Storgatan 1', '41101', 'Göteborg', 'SE', :e, 'HOME', :s, 'test', NOW(), 0)""")
                    .param("e", email).param("s", id).update();
        }
        return created[0];
    }

    private static MembershipFeedChange change(final String kind, final UUID spexare, final int year, final boolean lifetime) {
        return new MembershipFeedChange("1", UUID.randomUUID(), kind, spexare, "FGV", year, lifetime);
    }

    @Test
    void should_copy_what_the_feed_says_and_show_it_as_internal() {
        final UUID spexare = spexare("Greta", "Garbo", null);

        sync.applyAll(List.of(change("UPSERT", spexare, 2024, false), change("UPSERT", spexare, 2019, true)));

        graphQl(obtainAdminAccessToken())
                .document("query($id: ID!) { spexare(id: $id) { memberships { year lifetime type { id membershipKind } } } }")
                .variable("id", spexare).execute().errors().verify()
                .path("spexare.memberships").entityList(Object.class).hasSize(2)
                .path("spexare.memberships[?(@.year == '2019')].lifetime").entityList(Boolean.class).containsExactly(true)
                .path("spexare.memberships[0].type.membershipKind").entity(String.class).isEqualTo("INTERNAL");

        sync.applyAll(List.of(change("DELETE", spexare, 2024, false)));

        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM membership").query(Long.class).single()).isEqualTo(1);
    }

    @Test
    void should_refuse_changing_fgv_memberships_here_but_keep_external_ones_as_before() {
        final UUID spexare = spexare("Greta", "Garbo", null);
        final String create = "mutation($id: ID!, $type: ID!) { membershipCreate(spexareId: $id, typeId: $type, input: { year: \"2020\" }) { id } }";

        graphQl(obtainAdminAccessToken()).document(create).variable("id", spexare).variable("type", "FGV").execute()
                .errors().satisfy(errors -> assertThat(errors).hasSize(1));
        graphQl(obtainAdminAccessToken()).document(create).variable("id", spexare).variable("type", "CING").execute()
                .errors().verify();

        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM membership").query(Long.class).single()).isEqualTo(1);
    }

    @Test
    void should_let_medlemskap_read_spexare_but_nobody_else() {
        final UUID greta = spexare("Greta", "Garbo", "greta@example.org");
        final UUID ingrid = spexare("Ingrid", "Bergman", null);
        final String token = membershipToken();

        graphQl(token)
                .document("query($ids: [ID!]!) { medlemskapSpexareByIds(ids: $ids) { id firstName lastName email deceased postalAddress { city } } }")
                .variable("ids", List.of(ingrid, greta, UUID.randomUUID())).execute().errors().verify()
                .path("medlemskapSpexareByIds").entityList(Object.class).hasSize(2)
                .path("medlemskapSpexareByIds[0].firstName").entity(String.class).isEqualTo("Ingrid")
                .path("medlemskapSpexareByIds[1].email").entity(String.class).isEqualTo("greta@example.org")
                .path("medlemskapSpexareByIds[1].postalAddress.city").entity(String.class).isEqualTo("Göteborg");
        graphQl(token).document("{ medlemskapSpexareSearch(q: \"garbo greta\") { id } }").execute().errors().verify()
                .path("medlemskapSpexareSearch").entityList(Object.class).hasSize(1);

        graphQl(obtainUserAccessToken()).document("{ medlemskapSpexareSearch(q: \"garbo\") { id } }").execute()
                .errors().satisfy(errors -> assertThat(errors).isNotEmpty());
        graphQl(token).document("{ spexarePaged(first: 5) { totalCount } }").execute()
                .errors().satisfy(errors -> assertThat(errors).isNotEmpty());
    }

    @Test
    void should_list_fgv_memberships_for_medlemskap_to_compare_with() {
        final UUID greta = spexare("Greta", "Garbo", null);

        sync.applyAll(List.of(change("UPSERT", greta, 2024, false)));

        graphQl(membershipToken()).document("{ medlemskapFgvMemberships { spexareId type year lifetime } }").execute().errors().verify()
                .path("medlemskapFgvMemberships[0].spexareId").entity(String.class).isEqualTo(greta.toString())
                .path("medlemskapFgvMemberships[0].year").entity(Integer.class).isEqualTo(2024);
    }

    @Test
    void should_keep_a_deleted_spexare_to_tell_medlemskap_about_until_it_is_sent() {
        final UUID spexare = spexare("Greta", "Garbo", null);

        SecurityUtil.runAsSystem(() -> spexareService.deleteById(spexare));

        // The test profile does not talk to Medlemskap (spexregister.medlemskap.enabled=false), so nothing is kept or sent
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM spexare WHERE uuid = :u").param("u", spexare.toString()).query(Long.class).single()).isZero();
        assertThat(notifier.sendPending()).isZero();
    }

    private String membershipToken() {
        final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add("grant_type", "client_credentials");
        form.add("client_id", "fgv-membership-service");
        form.add("client_secret", secret("fgv-membership-service"));

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
