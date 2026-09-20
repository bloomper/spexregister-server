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

package nu.fgv.register.server.util.data;

import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import nu.fgv.register.server.audit.AuditSource;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Component
public class AuditDataSeeder {

    private static final int HISTORY_SPAN_IN_DAYS = 540;
    private static final int PERCENTAGE_OF_ROWS_WITH_HISTORY = 30;
    private static final int PERCENTAGE_OF_CHANGES_FROM_IMPORT = 20;
    private static final int PERCENTAGE_OF_CHANGES_FROM_RESTORE = 5;
    private static final int MAX_SINGLE_EDITS_PER_PASS = 40;
    private static final int NUMBER_OF_EDITORS = 6;
    private static final int NUMBER_OF_DELETED_ADDRESSES = 10;
    private static final String SYSTEM_USER = "system";

    private static final List<AuditedTable> TABLES = List.of(
            AuditedTable.of("nu.fgv.register.server.settings.Type", "type", "id, type, labels"),
            AuditedTable.of("nu.fgv.register.server.user.state.State", "state", "id, labels, initial, enabled"),
            AuditedTable.of("nu.fgv.register.server.user.User", "user", "id, external_id, state_id, spexare_id"),
            AuditedTable.of("nu.fgv.register.server.tag.Tag", "tag", "id, name")
                    .withHistory("name = LEFT(CONCAT('Preliminär ', name), 255)", "name = LEFT(CONCAT(name, ' (utkast)'), 255)"),
            AuditedTable.of("nu.fgv.register.server.news.News", "news", "id, visible_from, visible_to, subject, text, published")
                    .withHistory("subject = LEFT(CONCAT('Utkast: ', subject), 255)", "published = 1 - COALESCE(published, 0)"),
            AuditedTable.of("nu.fgv.register.server.spex.category.SpexCategory", "spex_category", "id, name, first_year, logo, logo_content_type")
                    .withHistory("name = LEFT(CONCAT(name, ' (arbetsnamn)'), 255)"),
            AuditedTable.of("nu.fgv.register.server.spex.SpexDetails", "spex_details", "id, title, poster, poster_content_type, category_id")
                    .withHistory("title = LEFT(CONCAT(title, ' (arbetstitel)'), 255)"),
            AuditedTable.of("nu.fgv.register.server.spex.Spex", "spex", "id, year, parent_id, details_id")
                    .withHistory("year = CAST(CAST(year AS UNSIGNED) - 1 AS CHAR)"),
            AuditedTable.of("nu.fgv.register.server.task.category.TaskCategory", "task_category", "id, name, actor_present")
                    .withHistory("name = LEFT(CONCAT(name, ' (preliminär)'), 255)"),
            AuditedTable.of("nu.fgv.register.server.task.Task", "task", "id, name, category_id")
                    .withHistory("name = LEFT(CONCAT(name, ' (preliminär)'), 255)"),
            AuditedTable.of("nu.fgv.register.server.spexare.Spexare", "spexare",
                            "id, first_name, last_name, nick_name, social_security_number, deceased, published, graduation, comment, image, image_content_type, partner_id")
                    .withHistory("comment = CONCAT('(tidigare) ', COALESCE(comment, ''))", "graduation = LEFT(CONCAT('(tidigare) ', COALESCE(graduation, '')), 255)", "nick_name = LEFT(CONCAT('(tidigare) ', COALESCE(nick_name, '')), 255)"),
            AuditedTable.of("nu.fgv.register.server.spexare.toggle.Toggle", "toggle", "id, value, type_id, spexare_id")
                    .withHistory("value = 1 - COALESCE(value, 0)"),
            new AuditedTable("nu.fgv.register.server.spexare.tagging.Tagging", "tagging", "tagging_audit",
                    List.of("tag_id", "spexare_id"), List.of()),
            AuditedTable.of("nu.fgv.register.server.spexare.membership.Membership", "membership", "id, year, type_id, spexare_id")
                    .withHistory("year = CAST(CAST(year AS UNSIGNED) - 1 AS CHAR)"),
            AuditedTable.of("nu.fgv.register.server.spexare.consent.Consent", "consent", "id, value, type_id, spexare_id")
                    .withHistory("value = 1 - COALESCE(value, 0)"),
            AuditedTable.of("nu.fgv.register.server.spexare.address.Address", "address",
                            "id, street_address, postal_code, city, country, phone, phone_mobile, email_address, type_id, spexare_id")
                    .withHistory("city = LEFT(CONCAT(COALESCE(city, 'Okänd'), 'holm'), 255)", "email_address = LEFT(CONCAT('gammal.', COALESCE(email_address, 'adress@example.com')), 255)", "phone_mobile = LEFT(CONCAT('0', COALESCE(phone_mobile, '700000000')), 255)"),
            AuditedTable.of("nu.fgv.register.server.spexare.activity.Activity", "activity", "id, spexare_id"),
            AuditedTable.of("nu.fgv.register.server.spexare.activity.spex.SpexActivity", "spex_activity", "id, activity_id, spex_id"),
            AuditedTable.of("nu.fgv.register.server.spexare.activity.task.TaskActivity", "task_activity", "id, activity_id, task_id"),
            AuditedTable.of("nu.fgv.register.server.spexare.activity.task.actor.Actor", "actor", "id, role, vocal_id, task_activity_id")
                    .withHistory("role = LEFT(CONCAT(COALESCE(role, ''), ' (ersättare)'), 255)")
    );

    private final Random rnd = new SecureRandom();
    private final Faker faker = new Faker(Locale.of("sv", "SE"));

    private static String placeholders(final List<Long> ids) {
        return String.join(", ", ids.stream().map(_ -> "?").toList());
    }

    public void seedBaseline(final JdbcClient jdbcClient) {
        purge(jdbcClient);

        final long now = Instant.now().toEpochMilli();
        final Revision initial = new Revision(
                insertRevision(jdbcClient, SYSTEM_USER, now, AuditSource.SYSTEM, "seed", null), now);

        TABLES.forEach(table -> seedInitialRevision(jdbcClient, table, initial));

        log.info("Seeded baseline audit data across {} tables", TABLES.size());
    }

    public void seedSampleHistory(final JdbcClient jdbcClient, final List<String> editors) {
        purge(jdbcClient);

        final Instant oldest = Instant.now().minus(HISTORY_SPAN_IN_DAYS, ChronoUnit.DAYS);
        final Revision baseline = new Revision(
                insertRevision(jdbcClient, SYSTEM_USER, oldest.toEpochMilli(), AuditSource.SYSTEM, "seed", null),
                oldest.toEpochMilli());

        TABLES.forEach(table -> seedInitialRevision(jdbcClient, table, baseline));

        final List<Edit> plan = new ArrayList<>(planEdits(jdbcClient, baseline));

        Collections.shuffle(plan, rnd);

        final List<Revision> revisions = createRevisions(jdbcClient, resolveEditors(editors), plan, oldest);

        IntStream.range(0, plan.size()).forEach(i ->
                pushRevision(jdbcClient, plan.get(i).table(), revisions.get(i), plan.get(i).mutation(), plan.get(i).ids()));

        seedDeletedAddresses(jdbcClient, revisions);

        log.info("Seeded sample audit history: {} revisions across {} tables", revisions.size() + 1, TABLES.size());
    }

    private List<Edit> planEdits(final JdbcClient jdbcClient, final Revision baseline) {
        final List<Edit> plan = new ArrayList<>();
        final AtomicInteger job = new AtomicInteger(1);

        TABLES.stream().filter(AuditedTable::hasHistory).forEach(table -> {
            final List<Long> allIds = jdbcClient
                    .sql("SELECT id FROM %s".formatted(table.table()))
                    .query(Long.class)
                    .list();

            if (allIds.isEmpty()) {
                return;
            }

            table.mutations().forEach(mutation -> {
                final List<Long> ids = sample(allIds);
                final int individually = Math.min(
                        ids.size() - ids.size() * PERCENTAGE_OF_CHANGES_FROM_IMPORT / 100,
                        MAX_SINGLE_EDITS_PER_PASS);
                final List<Long> imported = ids.subList(individually, ids.size());

                if (imported.size() > 1) {
                    plan.add(new Edit(table, mutation, List.copyOf(imported), AuditSource.IMPORT, "import",
                            "Import job %d (%s)".formatted(job.getAndIncrement(), table.table().toUpperCase(Locale.ROOT))));
                }

                ids.subList(0, individually).forEach(id -> {
                    final boolean restored = rnd.nextInt(100) < PERCENTAGE_OF_CHANGES_FROM_RESTORE;

                    plan.add(restored
                            ? new Edit(table, mutation, List.of(id), AuditSource.RESTORE, "restore",
                            "Återställd från version %d".formatted(baseline.id()))
                            : new Edit(table, mutation, List.of(id), AuditSource.WEB, updateOperationOf(table), null));
                });
            });
        });

        return plan;
    }

    private void purge(final JdbcClient jdbcClient) {
        TABLES.reversed().forEach(table ->
                jdbcClient
                        .sql("TRUNCATE TABLE %s".formatted(table.auditTable()))
                        .update()
        );

        jdbcClient.sql("TRUNCATE TABLE revchanges").update();
        jdbcClient.sql("TRUNCATE TABLE revinfo").update();
    }

    private List<String> resolveEditors(final List<String> candidates) {
        final List<String> editors = new ArrayList<>(candidates.stream().limit(NUMBER_OF_EDITORS).toList());

        while (editors.size() < NUMBER_OF_EDITORS) {
            editors.add(faker.internet().emailAddress());
        }

        return List.copyOf(editors);
    }

    private List<Revision> createRevisions(final JdbcClient jdbcClient,
                                           final List<String> editors,
                                           final List<Edit> plan,
                                           final Instant oldest) {
        final long spanInMillis = Instant.now().toEpochMilli() - oldest.toEpochMilli();
        final List<Long> timestamps = plan.stream()
                .map(_ -> oldest.toEpochMilli() + 1 + (long) (rnd.nextDouble() * (spanInMillis - 1)))
                .sorted(Comparator.naturalOrder())
                .toList();

        return IntStream.range(0, plan.size())
                .mapToObj(i -> {
                    final Edit edit = plan.get(i);
                    final String modifiedBy = edit.source() == AuditSource.IMPORT
                            ? SYSTEM_USER
                            : editors.get(rnd.nextInt(editors.size()));

                    return new Revision(
                            insertRevision(jdbcClient, modifiedBy, timestamps.get(i), edit.source(), edit.operation(), edit.comment()),
                            timestamps.get(i));
                })
                .toList();
    }

    private String updateOperationOf(final AuditedTable table) {
        final String entity = table.entityName().substring(table.entityName().lastIndexOf('.') + 1);

        return "%s%sUpdate".formatted(entity.substring(0, 1).toLowerCase(Locale.ROOT), entity.substring(1));
    }

    private long insertRevision(final JdbcClient jdbcClient,
                                final String modifiedBy,
                                final long modifiedAt,
                                final AuditSource source,
                                final String operation,
                                final @Nullable String comment) {
        final KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient
                .sql("INSERT INTO revinfo (modified_by, modified_at, source, operation, comment) VALUES (?, ?, ?, ?, ?)")
                .param(modifiedBy)
                .param(modifiedAt)
                .param(source.name())
                .param(operation)
                .param(comment)
                .update(keyHolder);

        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    private void seedInitialRevision(final JdbcClient jdbcClient, final AuditedTable table, final Revision revision) {
        final int rows = jdbcClient
                .sql("INSERT INTO %s (%s, rev, revend, revtype, revend_tstmp) SELECT %s, ?, NULL, 0, NULL FROM %s"
                        .formatted(table.auditTable(), table.columnList(), table.columnList(), table.table()))
                .param(revision.id())
                .update();

        if (rows > 0) {
            insertRevChanges(jdbcClient, revision.id(), table.entityName());
        }
    }

    private void pushRevision(final JdbcClient jdbcClient,
                              final AuditedTable table,
                              final Revision revision,
                              final String mutation,
                              final List<Long> ids) {
        final List<Long> closable = closableIds(jdbcClient, table, revision, ids);

        if (closable.isEmpty()) {
            return;
        }

        final String placeholders = placeholders(closable);

        var close = jdbcClient
                .sql("UPDATE %s SET revend = ?, revend_tstmp = FROM_UNIXTIME(? / 1000), %s WHERE revend IS NULL AND rev < ? AND id IN (%s)"
                        .formatted(table.auditTable(), mutation, placeholders))
                .param(revision.id())
                .param(revision.timestamp())
                .param(revision.id());
        for (final Long id : closable) {
            close = close.param(id);
        }
        close.update();

        var insert = jdbcClient
                .sql("INSERT INTO %s (%s, rev, revend, revtype, revend_tstmp) SELECT %s, ?, NULL, 1, NULL FROM %s WHERE id IN (%s)"
                        .formatted(table.auditTable(), table.columnList(), table.columnList(), table.table(), placeholders))
                .param(revision.id());
        for (final Long id : closable) {
            insert = insert.param(id);
        }
        insert.update();

        insertRevChanges(jdbcClient, revision.id(), table.entityName());
    }

    private List<Long> closableIds(final JdbcClient jdbcClient,
                                   final AuditedTable table,
                                   final Revision revision,
                                   final List<Long> ids) {
        var query = jdbcClient
                .sql("SELECT id FROM %s WHERE revend IS NULL AND rev < ? AND id IN (%s)"
                        .formatted(table.auditTable(), placeholders(ids)))
                .param(revision.id());
        for (final Long id : ids) {
            query = query.param(id);
        }

        return query.query(Long.class).list();
    }

    private void seedDeletedAddresses(final JdbcClient jdbcClient, final List<Revision> revisions) {
        final List<Long> sources = jdbcClient
                .sql("SELECT id FROM address ORDER BY RAND() LIMIT ?")
                .param(NUMBER_OF_DELETED_ADDRESSES)
                .query(Long.class)
                .list();

        if (sources.isEmpty()) {
            return;
        }

        final Long maxId = jdbcClient.sql("SELECT COALESCE(MAX(id), 0) FROM address").query(Long.class).single();
        final AuditedTable address = TABLES.stream()
                .filter(t -> "address".equals(t.table()))
                .findFirst()
                .orElseThrow();
        final String columnsWithoutId = String.join(", ", address.columns().stream().filter(c -> !"id".equals(c)).toList());

        long phantomId = Objects.requireNonNull(maxId) + 1;

        for (final Long source : sources) {
            final Revision added = revisions.get(rnd.nextInt(revisions.size() - 1));
            final Revision removed = revisions.get(revisions.indexOf(added) + 1 + rnd.nextInt(revisions.size() - revisions.indexOf(added) - 1));

            jdbcClient
                    .sql("INSERT INTO address_audit (id, %s, rev, revend, revtype, revend_tstmp) SELECT ?, %s, ?, ?, 0, FROM_UNIXTIME(? / 1000) FROM address WHERE id = ?"
                            .formatted(columnsWithoutId, columnsWithoutId))
                    .param(phantomId)
                    .param(added.id())
                    .param(removed.id())
                    .param(removed.timestamp())
                    .param(source)
                    .update();

            jdbcClient
                    .sql("INSERT INTO address_audit (id, %s, rev, revend, revtype, revend_tstmp) SELECT ?, %s, ?, NULL, 2, NULL FROM address WHERE id = ?"
                            .formatted(columnsWithoutId, columnsWithoutId))
                    .param(phantomId)
                    .param(removed.id())
                    .param(source)
                    .update();

            insertRevChanges(jdbcClient, added.id(), address.entityName());
            insertRevChanges(jdbcClient, removed.id(), address.entityName());

            phantomId++;
        }

        jdbcClient.sql("ALTER TABLE address AUTO_INCREMENT = %d".formatted(phantomId)).update();
    }

    private List<Long> sample(final List<Long> ids) {
        final List<Long> shuffled = new ArrayList<>(ids);

        Collections.shuffle(shuffled, rnd);

        return List.copyOf(shuffled.subList(0, Math.max(1, shuffled.size() * PERCENTAGE_OF_ROWS_WITH_HISTORY / 100)));
    }

    private void insertRevChanges(final JdbcClient jdbcClient, final long revision, final String entityName) {
        jdbcClient
                .sql("INSERT INTO revchanges (rev, entityname) SELECT ?, ? FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM revchanges WHERE rev = ? AND entityname = ?)")
                .param(revision)
                .param(entityName)
                .param(revision)
                .param(entityName)
                .update();
    }

    private record Revision(long id, long timestamp) {
    }

    private record Edit(AuditedTable table,
                        String mutation,
                        List<Long> ids,
                        AuditSource source,
                        String operation,
                        @Nullable String comment) {
    }
}