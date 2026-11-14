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

package nu.fgv.register.server.spexipedia;

import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The {@code SPEXIPEDIA} role (spexregister-spec, docs/base-data-and-memberships.md, section 6): Spexipedia shows who
 * took part in a spex, by nickname to visitors and by name to its members. Only published spexare are ever answered,
 * and names only when asked for, so that Spexipedia's pages for visitors never receive them. Nothing else about a
 * spexare is answered.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Controller
@PreAuthorize("hasRole('spexregister_SPEXIPEDIA')")
public class SpexipediaGraphqlApi {

    static final int MAX_IDS = 2_000;
    static final int MAX_SEARCH = 50;

    private final JdbcClient jdbc;

    public SpexipediaGraphqlApi(final JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static @Nullable String name(final @Nullable String first, final @Nullable String last) {
        final String name = ((first == null ? "" : first) + " " + (last == null ? "" : last)).strip();

        return name.isEmpty() ? null : name;
    }

    /**
     * The published spexare who took part in the spex (each a spex or a revival), with their tasks and characters there.
     */
    @QueryMapping
    @Transactional(readOnly = true)
    public List<Participant> spexipediaParticipants(@Argument final List<UUID> spexIds, @Argument final boolean names) {
        if (spexIds.isEmpty()) {
            return List.of();
        }

        final Map<String, Participant> participants = new LinkedHashMap<>();

        jdbc.sql("""
                        SELECT s.uuid AS spexare, s.first_name, s.last_name, s.nick_name, sp.uuid AS spex, t.uuid AS task, ch.uuid AS chr
                        FROM spexare s
                        JOIN activity a ON a.spexare_id = s.id
                        JOIN spex_activity sa ON sa.activity_id = a.id
                        JOIN spex sp ON sp.id = sa.spex_id
                        LEFT JOIN task_activity ta ON ta.activity_id = a.id
                        LEFT JOIN task t ON t.id = ta.task_id
                        LEFT JOIN actor ac ON ac.task_activity_id = ta.id
                        LEFT JOIN spex_character ch ON ch.id = ac.character_id
                        WHERE s.published = 1 AND sp.uuid IN (:ids)
                        ORDER BY s.last_name, s.first_name""")
                .param("ids", spexIds.stream().limit(MAX_IDS).map(UUID::toString).toList())
                .query((rs, _) -> {
                    final String key = rs.getString("spexare") + "|" + rs.getString("spex");
                    final Participant participant = participants.computeIfAbsent(key, _ -> {
                        try {
                            return new Participant(UUID.fromString(rs.getString("spexare")), rs.getString("nick_name"),
                                    names ? name(rs.getString("first_name"), rs.getString("last_name")) : null,
                                    UUID.fromString(rs.getString("spex")), new LinkedHashSet<>(), new LinkedHashSet<>());
                        } catch (final java.sql.SQLException e) {
                            throw new IllegalStateException(e);
                        }
                    });

                    if (rs.getString("task") != null) {
                        participant.taskIds().add(UUID.fromString(rs.getString("task")));
                    }
                    if (rs.getString("chr") != null) {
                        participant.characterIds().add(UUID.fromString(rs.getString("chr")));
                    }
                    return participant;
                })
                .list();
        return new ArrayList<>(participants.values());
    }

    /**
     * Published spexare among the ids.
     */
    @QueryMapping
    @Transactional(readOnly = true)
    public List<Spexare> spexipediaSpexare(@Argument final List<UUID> ids, @Argument final boolean names) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("SELECT uuid, first_name, last_name, nick_name FROM spexare WHERE published = 1 AND uuid IN (:ids)")
                .param("ids", ids.stream().limit(MAX_IDS).map(UUID::toString).toList())
                .query((rs, _) -> new Spexare(UUID.fromString(rs.getString(1)), rs.getString(4),
                        names ? name(rs.getString(2), rs.getString(3)) : null))
                .list();
    }

    /**
     * Published spexare whose name or nickname contains every word, for picking credits in Spexipedia.
     */
    @QueryMapping
    @Transactional(readOnly = true)
    public List<Spexare> spexipediaSpexareSearch(@Argument final String q, @Argument final @Nullable Integer first) {
        final List<String> words = List.of(q.toLowerCase(Locale.ROOT).strip().split("\\s+")).stream()
                .filter(word -> word.length() >= 2).limit(5).toList();

        if (words.isEmpty()) {
            return List.of();
        }

        final StringBuilder sql = new StringBuilder("SELECT uuid, first_name, last_name, nick_name FROM spexare WHERE published = 1");
        final Map<String, Object> params = new LinkedHashMap<>();

        for (int i = 0; i < words.size(); i++) {
            sql.append(" AND LOWER(CONCAT_WS(' ', first_name, last_name, nick_name)) LIKE :w").append(i);
            params.put("w" + i, "%" + words.get(i).replace("%", "\\%").replace("_", "\\_") + "%");
        }
        sql.append(" ORDER BY last_name, first_name LIMIT ").append(Math.clamp(first == null ? 25 : first, 1, MAX_SEARCH));

        return jdbc.sql(sql.toString()).params(params)
                .query((rs, _) -> new Spexare(UUID.fromString(rs.getString(1)), rs.getString(4), name(rs.getString(2), rs.getString(3))))
                .list();
    }

    public record Participant(UUID spexareId, @Nullable String nickname, @Nullable String name, UUID spexId,
                              Set<UUID> taskIds, Set<UUID> characterIds) {
    }

    public record Spexare(UUID id, @Nullable String nickname, @Nullable String name) {
    }
}
