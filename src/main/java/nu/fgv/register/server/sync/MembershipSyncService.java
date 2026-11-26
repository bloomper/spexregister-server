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

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import nu.fgv.register.server.settings.TypeRepository;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.membership.Membership;
import nu.fgv.register.server.spexare.membership.MembershipRepository;
import nu.fgv.register.server.util.security.SecurityUtil;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Keeps Spexregister's copy of FGV memberships: pulls Medlemskap's feed every five minutes and when Medlemskap tells it
 * to ({@code POST /api/sync/notify}), reads pages until it has caught up, and applies each page in one transaction
 * together with the cursor, so that a crash never applies a change twice or skips one. A page that cannot be applied
 * stops the pull with the error shown to admins and is tried again at the next pull; nothing after it is skipped,
 * because a membership must never be missing from the copy (spexregister-spec, docs/base-data-and-memberships.md,
 * sections 2 and 5). Changes are audited as made by {@value #AUDITOR}.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Service
public class MembershipSyncService {

    public static final String SOURCE = "medlemskap";
    public static final String AUDITOR = "medlemskap";
    private static final int MAX_ERROR = 1000;
    private static final int MAX_PAGES = 1000;

    private final MedlemskapFeedClient client;
    private final MedlemskapProperties properties;
    private final JdbcClient jdbc;
    private final TransactionTemplate transactionTemplate;
    private final MembershipRepository membershipRepository;
    private final SpexareRepository spexareRepository;
    private final TypeRepository typeRepository;
    private final Clock clock = Clock.systemUTC();
    private final ReentrantLock running = new ReentrantLock();

    public MembershipSyncService(final MedlemskapFeedClient client, final MedlemskapProperties properties, final JdbcClient jdbc,
                                 final PlatformTransactionManager transactionManager, final MembershipRepository membershipRepository,
                                 final SpexareRepository spexareRepository, final TypeRepository typeRepository) {
        this.client = client;
        this.properties = properties;
        this.jdbc = jdbc;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.membershipRepository = membershipRepository;
        this.spexareRepository = spexareRepository;
        this.typeRepository = typeRepository;
    }

    @Scheduled(cron = "${spexregister.jobs.sync-medlemskap.cron-expression}")
    @SchedulerLock(name = "sync-medlemskap", lockAtMostFor = "PT10M")
    void scheduled() {
        if (properties.enabled()) {
            pull(false);
        }
    }

    /**
     * Pulls in the background, e.g. when Medlemskap tells us there is something new.
     */
    public void pullSoon() {
        if (properties.enabled()) {
            Executors.newVirtualThreadPerTaskExecutor().execute(() -> pull(false));
        }
    }

    /**
     * Pulls until caught up, one run at a time; a run that finds another running returns at once.
     *
     * @param full from the start of the feed; what is applied again is the same, so nothing changes unless the copy differs
     * @return the outcome, or null when another run is going on
     */
    public SyncService.@Nullable Result pull(final boolean full) {
        if (!running.tryLock()) {
            return null;
        }
        try {
            final SyncService.Result[] result = new SyncService.Result[1];

            SecurityUtil.runAs(AUDITOR, () -> result[0] = pullLocked(full));
            return result[0];
        } finally {
            running.unlock();
        }
    }

    private SyncService.Result pullLocked(final boolean full) {
        String cursor = full ? null : cursor();
        int applied = 0;

        jdbc.sql("UPDATE sync_source SET last_pulled_at = :at WHERE name = :name").param("at", now()).param("name", SOURCE).update();
        try {
            for (int pages = 0; pages < MAX_PAGES; pages++) {
                final MembershipFeedChange.Page page = client.changes(cursor);
                final String next = page.cursor() == null ? cursor : page.cursor();

                applyPage(page, next);
                applied += page.changes().size();
                cursor = next;
                if (!page.hasMore()) {
                    break;
                }
            }
        } catch (final RuntimeException e) {
            final String error = Objects.requireNonNullElse(e.getMessage(), e.getClass().getSimpleName());

            log.warn("Sync from Medlemskap failed after {} changes: {}", applied, error);
            jdbc.sql("UPDATE sync_source SET last_error = :error WHERE name = :name")
                    .param("error", error.substring(0, Math.min(error.length(), MAX_ERROR))).param("name", SOURCE).update();
            return new SyncService.Result(applied, List.of(), error);
        }
        jdbc.sql("UPDATE sync_source SET last_success = :at, last_error = NULL WHERE name = :name").param("at", now()).param("name", SOURCE).update();
        if (applied > 0) {
            log.info("Applied {} changes from Medlemskap", applied);
        }
        return new SyncService.Result(applied, List.of(), null);
    }

    /**
     * Applies a page in one transaction with the cursor.
     */
    void applyPage(final MembershipFeedChange.Page page, final @Nullable String next) {
        transactionTemplate.executeWithoutResult(_ -> {
            page.changes().forEach(this::apply);
            jdbc.sql("UPDATE sync_source SET cursor_value = :cursor, applied = applied + :n WHERE name = :name")
                    .param("cursor", next).param("n", page.changes().size()).param("name", SOURCE).update();
        });
    }

    /**
     * Applies changes in one transaction, without moving the cursor; for tests, which need internal memberships that only
     * the sync may write.
     */
    public void applyAll(final List<MembershipFeedChange> changes) {
        SecurityUtil.runAs(AUDITOR, () -> transactionTemplate.executeWithoutResult(_ -> changes.forEach(this::apply)));
    }

    void apply(final MembershipFeedChange change) {
        final var spexare = spexareRepository.findByUuid0(change.spexareId());

        if (spexare.isEmpty()) {
            // Deleted here since; Medlemskap is told and keeps its records as bookkeeping
            log.info("Skipped {} of {} for spexare {}: not here", change.kind(), change.type(), change.spexareId());
            return;
        }

        final var type = typeRepository.findById(change.type())
                .orElseThrow(() -> new IllegalStateException("Membership type " + change.type() + " does not exist here"));
        final String year = Integer.toString(change.year());
        final var existing = membershipRepository.findBySpexareAndTypeAndYear(spexare.get(), type, year);

        if (MembershipFeedChange.DELETE.equals(change.kind())) {
            existing.ifPresent(membershipRepository::delete);
            return;
        }

        final Membership membership = existing.orElseGet(() -> {
            final Membership created = new Membership();

            created.setSpexare(spexare.get());
            created.setType(type);
            created.setYear(year);
            return created;
        });

        membership.setLifetime(change.lifetime());
        membershipRepository.save(membership);
    }

    public SyncService.Status status() {
        return jdbc.sql("SELECT name, cursor_value, last_pulled_at, last_success, last_error, applied FROM sync_source WHERE name = :name")
                .param("name", SOURCE)
                .query((rs, _) -> {
                    final Instant success = rs.getTimestamp("last_success") == null ? null : rs.getTimestamp("last_success").toInstant();

                    return new SyncService.Status(rs.getString("name"), rs.getString("cursor_value"),
                            rs.getTimestamp("last_pulled_at") == null ? null : rs.getTimestamp("last_pulled_at").toInstant(),
                            success, rs.getString("last_error"), rs.getLong("applied"),
                            properties.enabled() && (success == null || success.isBefore(now().minus(Duration.ofHours(1)))), List.of());
                })
                .single();
    }

    private @Nullable String cursor() {
        return jdbc.sql("SELECT cursor_value FROM sync_source WHERE name = :name").param("name", SOURCE).query(String.class).optional().orElse(null);
    }

    private Instant now() {
        return clock.instant();
    }
}
