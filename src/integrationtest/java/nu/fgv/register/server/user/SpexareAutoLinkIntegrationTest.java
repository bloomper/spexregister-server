package nu.fgv.register.server.user;

import nu.fgv.register.server.util.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Users are linked to their spexare by verified email address, when exactly one spexare without a user has it, and not
 * again after an admin has removed a link by hand.
 */
class SpexareAutoLinkIntegrationTest extends AbstractIntegrationTest {

    private final UserService service;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private nu.fgv.register.server.spexare.SpexareRepository spexareRepository;
    @Autowired
    private nu.fgv.register.server.user.state.StateRepository stateRepository;

    @Autowired
    SpexareAutoLinkIntegrationTest(final JdbcClient jdbcClient, final Keycloak keycloakAdminClient, final String keycloakClientId,
                                   final ObjectMapper objectMapper, final UserService service) {
        super(jdbcClient, keycloakAdminClient, keycloakClientId, objectMapper);
        this.service = service;
    }

    @BeforeEach
    void setUp() {
        for (final String table : List.of("permission_grant", "user", "address", "spexare")) {
            jdbcClient.sql("DELETE FROM " + table).update();
        }
    }

    private UUID user() {
        final User user = new User();

        user.setExternalId(UUID.randomUUID().toString());
        user.setState(stateRepository.findById("ACTIVE").orElseThrow());
        return userRepository.save(user).getUuid();
    }

    private long spexare(final String email) {
        final nu.fgv.register.server.spexare.Spexare entity = new nu.fgv.register.server.spexare.Spexare();

        entity.setFirstName("A");
        entity.setLastName("B");
        entity.setDeceased(false);
        entity.setPublished(true);

        final long spexare = spexareRepository.save(entity).getId();

        jdbcClient.sql("INSERT INTO address (uuid, email_address, type_id, spexare_id, created_by, created_at, version) VALUES (UUID(), :e, 'HOME', :s, 'test', NOW(), 0)")
                .param("e", email).param("s", spexare).update();
        return spexare;
    }

    private static UserRepresentation account(final String email, final boolean verified) {
        final UserRepresentation account = new UserRepresentation();

        account.setEmail(email);
        account.setEmailVerified(verified);
        return account;
    }

    private Long linked(final UUID user) {
        return jdbcClient.sql("SELECT spexare_id FROM user WHERE uuid = :u").param("u", user.toString()).query(Long.class).optional().orElse(null);
    }

    @Test
    void should_link_to_the_one_spexare_with_the_verified_email() {
        final UUID user = user();
        final long spexare = spexare("Basen@Example.org ");

        assertThat(service.autoLink(user, account("basen@example.org", false))).isFalse();
        assertThat(service.autoLink(user, account("basen@example.org", true))).isTrue();
        assertThat(linked(user)).isEqualTo(spexare);
    }

    @Test
    void should_not_link_when_several_spexare_share_the_address_or_one_is_taken() {
        final UUID user = user();

        spexare("delad@example.org");
        spexare("delad@example.org");
        assertThat(service.autoLink(user, account("delad@example.org", true))).isFalse();

        final UUID other = user();
        final long taken = spexare("tagen@example.org");

        jdbcClient.sql("UPDATE user SET spexare_id = :s WHERE uuid = :u").param("s", taken).param("u", other.toString()).update();
        assertThat(service.autoLink(user, account("tagen@example.org", true))).isFalse();
        assertThat(linked(user)).isNull();
    }

    @Test
    void should_not_link_again_after_an_admin_removed_the_link() {
        final UUID user = user();
        final long spexare = spexare("basen@example.org");

        assertThat(service.autoLink(user, account("basen@example.org", true))).isTrue();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin", "x", "ROLE_fgv-spexregister_ADMIN"));
        try {
            service.removeSpexare(user);
        } finally {
            SecurityContextHolder.clearContext();
        }

        assertThat(linked(user)).isNull();
        assertThat(service.autoLink(user, account("basen@example.org", true))).isFalse();
        assertThat(spexare).isPositive();
    }
}
