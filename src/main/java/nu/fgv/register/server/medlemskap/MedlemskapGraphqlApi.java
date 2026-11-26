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

package nu.fgv.register.server.medlemskap;

import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.user.UserRepository;
import nu.fgv.register.server.util.error.ResourceAlreadyExistsException;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The {@code MEMBERSHIP} role (spexregister-spec, docs/base-data-and-memberships.md, section 6): Medlemskap's service
 * account reads spexare by id or name, finds and sets the spexare a user is linked to, and reads FGV memberships. It
 * cannot read the personal identity number, other addresses, consents or tags, and cannot change or delete anything but
 * the user's link. Creating spexare for accepted applications is added with applications (Medlemskap's M5).
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Controller
@PreAuthorize("hasRole('fgv-spexregister_MEMBERSHIP')")
public class MedlemskapGraphqlApi {

    static final int MAX_IDS = 2_000;
    static final int MAX_SEARCH = 50;
    static final int MAX_MEMBERSHIPS = 1_000;

    private static final String SPEXARE_COLUMNS = """
            s.id AS pk, s.uuid, s.first_name, s.last_name, s.nick_name, s.deceased,
            (SELECT a.email_address FROM address a WHERE a.spexare_id = s.id AND a.email_address IS NOT NULL AND a.email_address <> ''
              ORDER BY (a.type_id = 'HOME') DESC, a.id LIMIT 1) AS email""";

    private final JdbcClient jdbc;
    private final UserRepository userRepository;
    private final SpexareRepository spexareRepository;

    public MedlemskapGraphqlApi(final JdbcClient jdbc, final UserRepository userRepository, final SpexareRepository spexareRepository) {
        this.jdbc = jdbc;
        this.userRepository = userRepository;
        this.spexareRepository = spexareRepository;
    }

    @QueryMapping
    @Transactional(readOnly = true)
    public List<Spexare> medlemskapSpexareByIds(@Argument final List<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        final Map<String, Spexare> found = new LinkedHashMap<>();

        jdbc.sql("SELECT " + SPEXARE_COLUMNS + " FROM spexare s WHERE s.uuid IN (:ids)")
                .param("ids", ids.stream().limit(MAX_IDS).map(UUID::toString).toList())
                .query((rs, _) -> found.put(rs.getString("uuid"), spexare(rs)))
                .list();
        return ids.stream().limit(MAX_IDS).map(id -> found.get(id.toString())).filter(java.util.Objects::nonNull).toList();
    }

    @QueryMapping
    @Transactional(readOnly = true)
    public List<Spexare> medlemskapSpexareSearch(@Argument final String q, @Argument final @Nullable Integer first) {
        final List<String> words = List.of(q.toLowerCase(Locale.ROOT).strip().split("\\s+")).stream()
                .filter(word -> word.length() >= 2).limit(5).toList();

        if (words.isEmpty()) {
            return List.of();
        }

        final StringBuilder sql = new StringBuilder("SELECT " + SPEXARE_COLUMNS + " FROM spexare s WHERE 1 = 1");
        final Map<String, Object> params = new LinkedHashMap<>();

        for (int i = 0; i < words.size(); i++) {
            sql.append(" AND LOWER(CONCAT_WS(' ', s.first_name, s.last_name, s.nick_name)) LIKE :w").append(i);
            params.put("w" + i, "%" + words.get(i).replace("%", "\\%").replace("_", "\\_") + "%");
        }
        sql.append(" ORDER BY s.last_name, s.first_name LIMIT ").append(Math.clamp(first == null ? 25 : first, 1, MAX_SEARCH));
        return jdbc.sql(sql.toString()).params(params).query((rs, _) -> spexare(rs)).list();
    }

    @QueryMapping
    @Transactional(readOnly = true)
    public @Nullable Spexare medlemskapSpexareForUser(@Argument final String userId) {
        return jdbc.sql("SELECT " + SPEXARE_COLUMNS + " FROM spexare s JOIN user u ON u.spexare_id = s.id WHERE u.external_id = :user")
                .param("user", userId).query((rs, _) -> spexare(rs)).optional().orElse(null);
    }

    @MutationMapping
    @Transactional
    public Spexare medlemskapLinkUser(@Argument final String userId, @Argument final UUID spexareId) {
        final var user = userRepository.findByExternalId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(nu.fgv.register.server.user.User.class, userId));
        final var spexare = spexareRepository.findByUuid0(spexareId)
                .orElseThrow(() -> new ResourceNotFoundException(nu.fgv.register.server.spexare.Spexare.class, spexareId));

        if (spexare.getUser() != null && !spexare.getUser().equals(user)) {
            throw new ResourceAlreadyExistsException(nu.fgv.register.server.spexare.Spexare.class, spexareId);
        }
        user.setSpexare(spexare);
        userRepository.save(user);
        return medlemskapSpexareByIds(List.of(spexareId)).getFirst();
    }

    @QueryMapping
    @Transactional(readOnly = true)
    public List<Membership> medlemskapFgvMemberships(@Argument final @Nullable String after, @Argument final @Nullable Integer first) {
        return jdbc.sql("""
                        SELECT m.uuid, s.uuid AS spexare, m.type_id, m.year, m.lifetime FROM membership m
                        JOIN spexare s ON s.id = m.spexare_id
                        WHERE m.type_id = 'FGV' AND m.uuid > :after ORDER BY m.uuid LIMIT :max""")
                .param("after", after == null ? "" : after).param("max", Math.clamp(first == null ? 500 : first, 1, MAX_MEMBERSHIPS))
                .query((rs, _) -> new Membership(UUID.fromString(rs.getString("uuid")), UUID.fromString(rs.getString("spexare")),
                        rs.getString("type_id"), Integer.parseInt(rs.getString("year")), rs.getBoolean("lifetime")))
                .list();
    }

    private Spexare spexare(final java.sql.ResultSet rs) throws java.sql.SQLException {
        final long pk = rs.getLong("pk");
        final PostalAddress address = jdbc.sql("""
                        SELECT street_address, postal_code, city, country FROM address
                        WHERE spexare_id = :pk AND (street_address IS NOT NULL OR city IS NOT NULL)
                        ORDER BY (type_id = 'HOME') DESC, id LIMIT 1""")
                .param("pk", pk)
                .query((a, _) -> new PostalAddress(a.getString(1), a.getString(2), a.getString(3), a.getString(4)))
                .optional().orElse(null);

        return new Spexare(UUID.fromString(rs.getString("uuid")), rs.getString("first_name"), rs.getString("last_name"),
                rs.getString("nick_name"), rs.getString("email"), address, rs.getBoolean("deceased"));
    }

    public record Spexare(UUID id, String firstName, String lastName, @Nullable String nickname, @Nullable String email,
                          @Nullable PostalAddress postalAddress, boolean deceased) {
    }

    public record PostalAddress(@Nullable String streetAddress, @Nullable String postalCode, @Nullable String city,
                                @Nullable String country) {
    }

    public record Membership(UUID id, UUID spexareId, String type, int year, boolean lifetime) {
    }
}
