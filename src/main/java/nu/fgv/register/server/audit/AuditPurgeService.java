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

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.image.Image;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.activity.Activity;
import nu.fgv.register.server.spexare.activity.spex.SpexActivity;
import nu.fgv.register.server.spexare.activity.task.TaskActivity;
import nu.fgv.register.server.spexare.activity.task.actor.Actor;
import nu.fgv.register.server.spexare.address.Address;
import nu.fgv.register.server.spexare.consent.Consent;
import nu.fgv.register.server.spexare.membership.Membership;
import nu.fgv.register.server.spexare.toggle.Toggle;
import nu.fgv.register.server.util.security.RequiresAdmin;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AuditPurgeService {

    private static final Map<Class<?>, List<String>> AUDIT_TABLES = Map.of(
            Spexare.class, List.of("spexare_audit", "tagging_audit"),
            Image.class, List.of("image_audit"),
            Address.class, List.of("address_audit"),
            Consent.class, List.of("consent_audit"),
            Membership.class, List.of("membership_audit"),
            Toggle.class, List.of("toggle_audit"),
            Activity.class, List.of("activity_audit"),
            SpexActivity.class, List.of("spex_activity_audit"),
            TaskActivity.class, List.of("task_activity_audit"),
            Actor.class, List.of("actor_audit")
    );

    private final JdbcClient jdbcClient;

    @RequiresAdmin
    @Transactional
    public void purgeSpexare(final Long id) {
        final Set<Long> revisions = new HashSet<>();
        final List<Long> imageIds = ids("SELECT DISTINCT image_id FROM spexare_audit WHERE id IN (:ids) AND image_id IS NOT NULL", List.of(id));
        final List<Long> activityIds = ids("SELECT DISTINCT id FROM activity_audit WHERE spexare_id IN (:ids)", List.of(id));
        final List<Long> taskActivityIds = ids("SELECT DISTINCT id FROM task_activity_audit WHERE activity_id IN (:ids)", activityIds);

        purge("actor_audit", "task_activity_id", taskActivityIds, revisions);
        purge("task_activity_audit", "id", taskActivityIds, revisions);
        purge("spex_activity_audit", "activity_id", activityIds, revisions);
        purge("activity_audit", "id", activityIds, revisions);
        purge("address_audit", "spexare_id", List.of(id), revisions);
        purge("consent_audit", "spexare_id", List.of(id), revisions);
        purge("membership_audit", "spexare_id", List.of(id), revisions);
        purge("toggle_audit", "spexare_id", List.of(id), revisions);
        purge("tagging_audit", "spexare_id", List.of(id), revisions);
        purge("image_audit", "id", imageIds, revisions);
        purge("spexare_audit", "id", List.of(id), revisions);

        jdbcClient.sql("UPDATE spexare_audit SET partner_id = NULL WHERE partner_id = :id").param("id", id).update();
        jdbcClient.sql("UPDATE user_audit SET spexare_id = NULL WHERE spexare_id = :id").param("id", id).update();

        final int removedRevisions = removeEmptyRevisions(revisions);

        log.info("Purged audit history of spexare {}, {} revisions left empty were removed", id, removedRevisions);
    }

    private List<Long> ids(final String sql, final Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbcClient.sql(sql).param("ids", ids).query(Long.class).list();
    }

    private void purge(final String table, final String column, final Collection<Long> ids, final Set<Long> revisions) {
        if (ids.isEmpty()) {
            return;
        }

        jdbcClient.sql("SELECT rev, revend FROM %s WHERE %s IN (:ids)".formatted(table, column))
                .param("ids", ids)
                .query((rs, _) -> {
                    revisions.add(rs.getLong("rev"));
                    final long revend = rs.getLong("revend");
                    if (!rs.wasNull()) {
                        revisions.add(revend);
                    }
                    return null;
                })
                .list();

        jdbcClient.sql("DELETE FROM %s WHERE %s IN (:ids)".formatted(table, column))
                .param("ids", ids)
                .update();
    }

    private int removeEmptyRevisions(final Set<Long> revisions) {
        if (revisions.isEmpty()) {
            return 0;
        }

        AUDIT_TABLES.forEach((entityClass, tables) -> {
            final String noRemainingRows = tables.stream()
                    .map("NOT EXISTS (SELECT 1 FROM %s a WHERE a.rev = revchanges.rev)"::formatted)
                    .reduce((a, b) -> a + " AND " + b)
                    .orElseThrow();

            jdbcClient.sql("DELETE FROM revchanges WHERE rev IN (:revisions) AND entityname = :entityName AND " + noRemainingRows)
                    .param("revisions", revisions)
                    .param("entityName", entityClass.getName())
                    .update();
        });

        return jdbcClient.sql("DELETE FROM revinfo WHERE id IN (:revisions) AND NOT EXISTS (SELECT 1 FROM revchanges c WHERE c.rev = revinfo.id)")
                .param("revisions", revisions)
                .update();
    }
}
