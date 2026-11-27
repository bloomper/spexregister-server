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

package nu.fgv.register.server.sync;

import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.SpexDetails;
import nu.fgv.register.server.spex.SpexDetailsRepository;
import nu.fgv.register.server.spex.SpexRepository;
import nu.fgv.register.server.spex.category.SpexCategory;
import nu.fgv.register.server.spex.category.SpexCategoryRepository;
import nu.fgv.register.server.spex.character.SpexCharacter;
import nu.fgv.register.server.spex.character.SpexCharacterRepository;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.task.Task;
import nu.fgv.register.server.task.TaskRepository;
import nu.fgv.register.server.task.category.TaskCategory;
import nu.fgv.register.server.task.category.TaskCategoryRepository;
import nu.fgv.register.server.util.search.IndexingService;
import nu.fgv.register.server.util.security.SecurityUtil;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Keeps Spexregister's copy of base data: pulls Spexipedia's change feed every five minutes and when Spexipedia tells it
 * to ({@code POST /api/sync/notify}), reads pages until it has caught up, and applies each page in one transaction
 * together with the cursor, so that a crash never applies a change twice or skips one. Records are written by
 * Spexipedia's ids; the internal keys, and the activities that point at them, stay. A merge moves every activity, actor
 * and revival from the merged record to the record it was merged into, then deletes it. Changes are audited as made by
 * {@value #AUDITOR} (spexregister-spec, docs/base-data-and-memberships.md, section 2).
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Service
public class SyncService {

    public static final String SOURCE = "spexipedia";
    public static final String AUDITOR = "spexipedia";
    static final Duration WARN_AFTER = Duration.ofHours(1);
    private static final int MAX_ERROR = 1000;
    private static final int MAX_PAGES = 1000;
    static final int MAX_ATTEMPTS = 5;
    private static final int MAX_SKIPPED = 50;

    private final SpexipediaFeedClient client;
    private final SyncProperties properties;
    private final JdbcClient jdbc;
    private final TransactionTemplate transactionTemplate;
    private final SpexCategoryRepository categoryRepository;
    private final SpexRepository spexRepository;
    private final SpexDetailsRepository detailsRepository;
    private final TaskCategoryRepository taskCategoryRepository;
    private final TaskRepository taskRepository;
    private final SpexCharacterRepository characterRepository;
    private final EntityManagerFactory entityManagerFactory;
    private final IndexingService indexingService;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final ReentrantLock running = new ReentrantLock();

    @SuppressWarnings("java:S107")
    public SyncService(final SpexipediaFeedClient client, final SyncProperties properties, final JdbcClient jdbc,
                       final PlatformTransactionManager transactionManager, final SpexCategoryRepository categoryRepository,
                       final SpexRepository spexRepository, final SpexDetailsRepository detailsRepository,
                       final TaskCategoryRepository taskCategoryRepository, final TaskRepository taskRepository,
                       final SpexCharacterRepository characterRepository, final EntityManagerFactory entityManagerFactory,
                       final IndexingService indexingService, final ApplicationEventPublisher events) {
        this.client = client;
        this.properties = properties;
        this.jdbc = jdbc;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.categoryRepository = categoryRepository;
        this.spexRepository = spexRepository;
        this.detailsRepository = detailsRepository;
        this.taskCategoryRepository = taskCategoryRepository;
        this.taskRepository = taskRepository;
        this.characterRepository = characterRepository;
        this.entityManagerFactory = entityManagerFactory;
        this.indexingService = indexingService;
        this.events = events;
        this.clock = Clock.systemUTC();
    }

    @Scheduled(cron = "${spexregister.jobs.sync-spexipedia.cron-expression}")
    @SchedulerLock(name = "sync-spexipedia", lockAtMostFor = "PT10M")
    void scheduled() {
        if (properties.enabled()) {
            pull(false);
        }
    }

    /**
     * Pulls in the background, e.g. when Spexipedia tells us there is something new.
     */
    public void pullSoon() {
        if (properties.enabled()) {
            Executors.newVirtualThreadPerTaskExecutor().execute(() -> pull(false));
        }
    }

    /**
     * Pulls until caught up, one run at a time; a run that finds another running returns at once.
     *
     * @param full from the start of the feed, reporting records here that Spexipedia no longer lists
     * @return the outcome, or null when another run is going on
     */
    public @Nullable Result pull(final boolean full) {
        if (!running.tryLock()) {
            return null;
        }
        try {
            final Result[] result = new Result[1];

            SecurityUtil.runAs(AUDITOR, () -> result[0] = pullLocked(full));
            return result[0];
        } finally {
            running.unlock();
        }
    }

    private Result pullLocked(final boolean full) {
        final Set<UUID> seen = new HashSet<>();
        String cursor = full ? null : cursor();
        int applied = 0;
        boolean merged = false;

        jdbc.sql("UPDATE sync_source SET last_pulled_at = :at WHERE name = :name").param("at", now()).param("name", SOURCE).update();
        if (full) {
            // Everything is applied again, so what was skipped gets a new chance
            jdbc.sql("DELETE FROM sync_skipped WHERE source = :name").param("name", SOURCE).update();
        }
        try {
            for (int pages = 0; pages < MAX_PAGES; pages++) {
                final FeedChange.Page page = client.changes(cursor);
                final String next = page.cursor() == null ? cursor : page.cursor();

                page.changes().forEach(change -> seen.add(change.id()));
                merged |= applyPage(page, next);
                applied += page.changes().size();
                cursor = next;
                if (!page.hasMore()) {
                    break;
                }
            }
        } catch (final RuntimeException e) {
            final String error = Objects.requireNonNullElse(e.getMessage(), e.getClass().getSimpleName());

            log.warn("Sync from Spexipedia failed after {} changes: {}", applied, error);
            jdbc.sql("UPDATE sync_source SET last_error = :error WHERE name = :name")
                    .param("error", error.substring(0, Math.min(error.length(), MAX_ERROR))).param("name", SOURCE).update();
            return new Result(applied, List.of(), error);
        }
        jdbc.sql("UPDATE sync_source SET last_success = :at, last_error = NULL WHERE name = :name").param("at", now()).param("name", SOURCE).update();
        if (applied > 0) {
            log.info("Applied {} changes from Spexipedia", applied);
            // Spexare are searched by their spex and tasks; their index follows what changed under them
            indexingService.initiateIndexingFor(Spexare.class, true);
        }
        if (merged) {
            entityManagerFactory.getCache().evictAll();
        }
        if (applied > 0) {
            events.publishEvent(new BaseDataSyncedEvent());
        }
        return new Result(applied, full ? missing(seen) : List.of(), null);
    }

    /**
     * Applies a page in one transaction with the cursor. When that fails its changes are applied one at a time, so that
     * the ones before the failing one are kept. A change that keeps failing is tried {@value #MAX_ATTEMPTS} times, at
     * as many pulls; then it is recorded as skipped, for admins to see, and the feed goes on without it.
     *
     * @return whether a merge was applied
     */
    boolean applyPage(final FeedChange.Page page, final @Nullable String next) {
        try {
            return Boolean.TRUE.equals(transactionTemplate.execute(_ -> {
                boolean merged = false;

                for (final FeedChange change : page.changes()) {
                    merged |= apply(change);
                }
                moveCursor(next, page.changes().size());
                return merged;
            }));
        } catch (final RuntimeException e) {
            log.warn("A page of {} changes from Spexipedia could not be applied, applying them one at a time: {}",
                    page.changes().size(), message(e));
        }

        boolean merged = false;

        for (final FeedChange change : page.changes()) {
            try {
                merged |= Boolean.TRUE.equals(transactionTemplate.execute(_ -> {
                    final boolean any = apply(change);

                    moveCursor(change.sequence(), 1);
                    return any;
                }));
            } catch (final RuntimeException e) {
                if (attempt(change) < MAX_ATTEMPTS) {
                    throw e;
                }
                skip(change, message(e));
            }
        }
        transactionTemplate.executeWithoutResult(_ -> moveCursor(next, 0));
        return merged;
    }

    /**
     * Moves the cursor; whatever was failing before it no longer is.
     */
    private void moveCursor(final @Nullable String cursor, final int applied) {
        jdbc.sql("""
                        UPDATE sync_source SET cursor_value = :cursor, applied = applied + :n, failing_sequence = NULL, failing_attempts = 0
                        WHERE name = :name""")
                .param("cursor", cursor).param("n", applied).param("name", SOURCE).update();
    }

    /**
     * Counts one more failed attempt at the change and answers how many there have been.
     */
    private int attempt(final FeedChange change) {
        jdbc.sql("""
                        UPDATE sync_source
                        SET failing_attempts = CASE WHEN failing_sequence = :sequence THEN failing_attempts + 1 ELSE 1 END,
                            failing_sequence = :sequence
                        WHERE name = :name""")
                .param("sequence", change.sequence()).param("name", SOURCE).update();
        return jdbc.sql("SELECT failing_attempts FROM sync_source WHERE name = :name").param("name", SOURCE).query(Integer.class).single();
    }

    private void skip(final FeedChange change, final String error) {
        log.error("Skipped change {} from Spexipedia ({} {} {}) after {} attempts: {}", change.sequence(), change.change(),
                change.kind(), change.id(), MAX_ATTEMPTS, error);
        transactionTemplate.executeWithoutResult(_ -> {
            jdbc.sql("""
                            INSERT INTO sync_skipped (source, sequence, kind, record_id, change_type, error, skipped_at)
                            VALUES (:source, :sequence, :kind, :id, :change, :error, :at)""")
                    .param("source", SOURCE).param("sequence", change.sequence()).param("kind", change.kind())
                    .param("id", change.id().toString()).param("change", change.change())
                    .param("error", error.substring(0, Math.min(error.length(), MAX_ERROR))).param("at", now()).update();
            moveCursor(change.sequence(), 0);
        });
    }

    private static String message(final RuntimeException e) {
        return Objects.requireNonNullElse(e.getMessage(), e.getClass().getSimpleName());
    }

    private Instant now() {
        return clock.instant();
    }

    private @Nullable String cursor() {
        return jdbc.sql("SELECT cursor_value FROM sync_source WHERE name = :name").param("name", SOURCE).query(String.class).optional().orElse(null);
    }

    /**
     * Records here that the feed never mentioned in a full pull: reported, never deleted.
     */
    private List<String> missing(final Set<UUID> seen) {
        final List<String> missing = new java.util.ArrayList<>();

        for (final String table : List.of("spex_category", "spex", "task_category", "task", "spex_character")) {
            jdbc.sql("SELECT uuid FROM " + table).query(String.class).list().stream()
                    .filter(uuid -> !seen.contains(UUID.fromString(uuid)))
                    .forEach(uuid -> missing.add(table + ":" + uuid));
        }
        if (!missing.isEmpty()) {
            log.warn("{} records in Spexregister are not in Spexipedia's feed: {}", missing.size(), missing);
        }
        return missing;
    }

    // Applying changes -------------------------------------------------------------------------------------------------

    /**
     * Applies changes in one transaction, as a pull applies a page, without moving the cursor; for tests that need base
     * data, which only the sync may write.
     */
    public void applyAll(final List<FeedChange> changes) {
        SecurityUtil.runAs(AUDITOR, () -> transactionTemplate.executeWithoutResult(_ -> changes.forEach(this::apply)));
    }

    /**
     * @return whether it was a merge
     */
    boolean apply(final FeedChange change) {
        if ("MERGE".equals(change.change())) {
            merge(change.kind(), change.id(), Objects.requireNonNull(change.mergedIntoId()));
            return true;
        }
        switch (change.kind()) {
            case "SPEX_CATEGORY" -> upsert(Objects.requireNonNull(change.spexCategory()));
            case "SPEX" -> upsert(Objects.requireNonNull(change.spex()));
            case "TASK_CATEGORY" -> upsert(Objects.requireNonNull(change.taskCategory()));
            case "TASK" -> upsert(Objects.requireNonNull(change.task()));
            case "CHARACTER" -> upsert(Objects.requireNonNull(change.character()));
            default -> {
                // Numbers, and kinds Spexregister does not keep
            }
        }
        return false;
    }

    private void upsert(final FeedChange.SpexCategory record) {
        final SpexCategory category = categoryRepository.findByUuid(record.id()).orElseGet(() -> {
            final SpexCategory created = new SpexCategory();

            created.setUuid(record.id());
            return created;
        });

        category.setName(record.name());
        category.setFirstYear(record.firstYear());
        category.setLogoUrl(record.logoUrl());
        category.setRetired(record.retired());
        categoryRepository.save(category);
    }

    private void upsert(final FeedChange.Spex record) {
        final Spex spex = spexRepository.findByUuid(record.id()).orElseGet(() -> {
            final Spex created = new Spex();

            created.setUuid(record.id());
            return created;
        });

        if (record.parentId() != null) {
            final Spex parent = spexRepository.findByUuid(record.parentId())
                    .orElseThrow(() -> new IllegalStateException("The parent " + record.parentId() + " of " + record.id() + " is not here yet"));

            spex.setParent(parent);
            spex.setDetails(parent.getDetails());
        } else {
            final SpexDetails details = spex.getDetails() != null && spex.getParent() == null ? spex.getDetails() : new SpexDetails();

            details.setTitle(record.title());
            details.setCategory(record.categoryId() == null ? null : categoryRepository.findByUuid(record.categoryId())
                    .orElseThrow(() -> new IllegalStateException("The category " + record.categoryId() + " is not here yet")));
            spex.setParent(null);
            spex.setDetails(detailsRepository.save(details));
        }
        spex.setYear(record.year());
        spex.setPosterUrl(record.posterUrl());
        spex.setRetired(record.retired());
        spexRepository.save(spex);
    }

    private void upsert(final FeedChange.TaskCategory record) {
        final TaskCategory category = taskCategoryRepository.findByUuid(record.id()).orElseGet(() -> {
            final TaskCategory created = new TaskCategory();

            created.setUuid(record.id());
            return created;
        });

        category.setName(record.name());
        category.setActorPresent(record.actorPresent());
        category.setRetired(record.retired());
        taskCategoryRepository.save(category);
    }

    private void upsert(final FeedChange.Task record) {
        final Task task = taskRepository.findByUuid(record.id()).orElseGet(() -> {
            final Task created = new Task();

            created.setUuid(record.id());
            return created;
        });

        task.setName(record.name());
        task.setCategory(record.categoryId() == null ? null : taskCategoryRepository.findByUuid(record.categoryId())
                .orElseThrow(() -> new IllegalStateException("The task category " + record.categoryId() + " is not here yet")));
        task.setBoard(record.board());
        task.setRetired(record.retired());
        taskRepository.save(task);
    }

    private void upsert(final FeedChange.Character record) {
        final SpexCharacter character = characterRepository.findByUuid(record.id()).orElseGet(() -> {
            final SpexCharacter created = new SpexCharacter();

            created.setUuid(record.id());
            return created;
        });

        character.setSpex(spexRepository.findByUuid(record.spexId())
                .orElseThrow(() -> new IllegalStateException("The spex " + record.spexId() + " of character " + record.id() + " is not here yet")));
        character.setName(record.name());
        character.setPosition(record.position());
        character.setRetired(record.retired());
        characterRepository.save(character);
    }

    /**
     * The tables and columns that point at each kind, by internal key.
     */
    private static final Map<String, List<String>> REFERENCES = Map.of(
            "SPEX_CATEGORY", List.of("spex_details.category_id"),
            "SPEX", List.of("spex_activity.spex_id", "spex.parent_id", "spex_character.spex_id", "permission_grant.spex_id"),
            "TASK_CATEGORY", List.of("task.category_id"),
            "TASK", List.of("task_activity.task_id"),
            "CHARACTER", List.of("actor.character_id"));

    private static final Map<String, String> TABLES = Map.of("SPEX_CATEGORY", "spex_category", "SPEX", "spex",
            "TASK_CATEGORY", "task_category", "TASK", "task", "CHARACTER", "spex_character");

    /**
     * Moves everything that points at the merged record to the one it was merged into, then deletes it. A record
     * already gone (merged before, or never copied) is skipped.
     */
    private void merge(final String kind, final UUID from, final UUID into) {
        final String table = TABLES.get(kind);

        if (table == null) {
            return;
        }

        final Long fromKey = key(table, from);
        final Long intoKey = key(table, into);

        if (fromKey == null || intoKey == null) {
            log.info("Skipped merging {} {} into {}: not both here", kind, from, into);
            return;
        }
        for (final String reference : REFERENCES.get(kind)) {
            final String[] parts = reference.split("\\.");

            jdbc.sql("UPDATE %s SET %s = :into WHERE %s = :from".formatted(parts[0], parts[1], parts[1]))
                    .param("into", intoKey).param("from", fromKey).update();
        }

        final Long details = "SPEX".equals(kind)
                ? jdbc.sql("SELECT details_id FROM spex WHERE id = :id").param("id", fromKey).query(Long.class).single() : null;

        jdbc.sql("DELETE FROM " + table + " WHERE id = :id").param("id", fromKey).update();
        if (details != null && jdbc.sql("SELECT COUNT(*) FROM spex WHERE details_id = :d").param("d", details).query(Long.class).single() == 0) {
            jdbc.sql("DELETE FROM spex_details WHERE id = :d").param("d", details).update();
        }
        log.info("Merged {} {} into {}", kind, from, into);
    }

    private @Nullable Long key(final String table, final UUID uuid) {
        return jdbc.sql("SELECT id FROM " + table + " WHERE uuid = :uuid").param("uuid", uuid.toString()).query(Long.class).optional().orElse(null);
    }

    // Status -----------------------------------------------------------------------------------------------------------

    public Status status() {
        final List<Skipped> skipped = jdbc.sql("""
                        SELECT sequence, kind, record_id, change_type, error, skipped_at FROM sync_skipped
                        WHERE source = :name ORDER BY id DESC LIMIT :max""")
                .param("name", SOURCE).param("max", MAX_SKIPPED)
                .query((rs, _) -> new Skipped(rs.getString("sequence"), rs.getString("kind"), rs.getString("record_id"),
                        rs.getString("change_type"), rs.getString("error"), rs.getTimestamp("skipped_at").toInstant()))
                .list();

        return jdbc.sql("SELECT name, cursor_value, last_pulled_at, last_success, last_error, applied FROM sync_source WHERE name = :name")
                .param("name", SOURCE)
                .query((rs, _) -> {
                    final Instant success = rs.getTimestamp("last_success") == null ? null : rs.getTimestamp("last_success").toInstant();

                    return new Status(rs.getString("name"), rs.getString("cursor_value"),
                            rs.getTimestamp("last_pulled_at") == null ? null : rs.getTimestamp("last_pulled_at").toInstant(),
                            success, rs.getString("last_error"), rs.getLong("applied"),
                            properties.enabled() && (success == null || success.isBefore(now().minus(WARN_AFTER))), skipped);
                })
                .single();
    }

    public record Status(String name, @Nullable String cursor, @Nullable Instant lastPulledAt,
                         @Nullable Instant lastSuccess,
                         @Nullable String lastError, long applied, boolean stale, List<Skipped> skipped) {
    }

    /**
     * A change that could not be applied and was left out: the copy lacks it until it is made again in Spexipedia, or
     * the whole feed is pulled again.
     */
    public record Skipped(String sequence, String kind, String id, String change, String error, Instant skippedAt) {
    }

    public record Result(int applied, List<String> missing, @Nullable String error) {
    }

    /**
     * Published after a pull that changed something, e.g. for linking actors to new characters.
     */
    public record BaseDataSyncedEvent() {
    }
}
