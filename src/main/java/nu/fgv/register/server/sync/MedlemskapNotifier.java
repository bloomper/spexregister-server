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
import nu.fgv.register.server.spexare.SpexareDeletedEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.util.List;
import java.util.Map;

/**
 * Tells Medlemskap that a spexare was deleted ({@code POST /api/sync/spexare-deleted}), so that it can mark the member
 * as deleted and keep the memberships and payments as bookkeeping records. The deletion is written to
 * {@code medlemskap_notification} together with the spexare's deletion; the call is made at once after the commit and
 * retried by a job until it works (spexregister-spec, docs/base-data-and-memberships.md, section 5.5).
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Component
public class MedlemskapNotifier {

    private static final int MAX_BATCH = 200;

    private final RestClient client;
    private final MedlemskapProperties properties;
    private final JdbcClient jdbc;
    private final Clock clock = Clock.systemUTC();

    public MedlemskapNotifier(@Qualifier(SyncConfig.REST_CLIENT) final RestClient client, final MedlemskapProperties properties,
                              final JdbcClient jdbc) {
        this.client = client;
        this.properties = properties;
        this.jdbc = jdbc;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void record(final SpexareDeletedEvent event) {
        if (properties.enabled()) {
            jdbc.sql("INSERT INTO medlemskap_notification (spexare_id, created_at) VALUES (:id, :at)")
                    .param("id", event.id().toString()).param("at", java.sql.Timestamp.from(clock.instant())).update();
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void send(final SpexareDeletedEvent event) {
        sendPending();
    }

    @Scheduled(cron = "${spexregister.jobs.notify-medlemskap.cron-expression}")
    @SchedulerLock(name = "notify-medlemskap", lockAtMostFor = "PT10M")
    void retry() {
        sendPending();
    }

    /**
     * Sends what has not been sent. A failure leaves the row for the next try.
     *
     * @return how many were sent
     */
    public int sendPending() {
        if (!properties.enabled()) {
            return 0;
        }

        final List<Map.Entry<Long, String>> pending = jdbc.sql("SELECT id, spexare_id FROM medlemskap_notification ORDER BY id LIMIT " + MAX_BATCH)
                .query((rs, _) -> Map.entry(rs.getLong(1), rs.getString(2))).list();
        int sent = 0;

        for (final Map.Entry<Long, String> row : pending) {
            try {
                client.post().uri(properties.apiUrl() + "/api/sync/spexare-deleted")
                        .body(Map.of("spexareId", row.getValue())).retrieve().toBodilessEntity();
                jdbc.sql("DELETE FROM medlemskap_notification WHERE id = :id").param("id", row.getKey()).update();
                sent++;
            } catch (final RuntimeException e) {
                jdbc.sql("UPDATE medlemskap_notification SET attempts = attempts + 1 WHERE id = :id").param("id", row.getKey()).update();
                log.warn("Could not tell Medlemskap that spexare {} was deleted: {}", row.getValue(), e.getMessage());
                break;
            }
        }
        return sent;
    }
}
