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


package nu.fgv.register.server.util.job;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class MaintenanceJobTrackerTest {

    private final MaintenanceJobTracker tracker = new MaintenanceJobTracker();

    @Test
    void should_record_a_successful_run() {
        final AtomicBoolean ran = new AtomicBoolean();

        assertThat(tracker.run(MaintenanceJob.USER_SYNC, () -> ran.set(true))).isTrue();

        assertThat(ran).isTrue();
        assertThat(tracker.isRunning(MaintenanceJob.USER_SYNC)).isFalse();
        assertThat(tracker.lastRun(MaintenanceJob.USER_SYNC)).hasValueSatisfying(run -> {
            assertThat(run.startedAt()).isNotNull();
            assertThat(run.finishedAt()).isNotNull();
            assertThat(run.succeeded()).isTrue();
        });
    }

    @Test
    void should_record_a_failed_run_and_rethrow() {
        assertThatThrownBy(() -> tracker.run(MaintenanceJob.JOB_CLEANUP, () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(tracker.isRunning(MaintenanceJob.JOB_CLEANUP)).isFalse();
        assertThat(tracker.lastRun(MaintenanceJob.JOB_CLEANUP)).hasValueSatisfying(run -> assertThat(run.succeeded()).isFalse());
    }

    @Test
    void should_count_an_async_job_as_running_until_it_completes() {
        final CompletableFuture<Void> work = new CompletableFuture<>();

        assertThat(tracker.runAsync(MaintenanceJob.SEARCH_INDEX, () -> work)).isTrue();
        assertThat(tracker.isRunning(MaintenanceJob.SEARCH_INDEX)).isTrue();
        assertThat(tracker.lastRun(MaintenanceJob.SEARCH_INDEX)).hasValueSatisfying(run -> {
            assertThat(run.finishedAt()).isNull();
            assertThat(run.succeeded()).isNull();
        });

        work.completeExceptionally(new IllegalStateException("boom"));

        assertThat(tracker.isRunning(MaintenanceJob.SEARCH_INDEX)).isFalse();
        assertThat(tracker.lastRun(MaintenanceJob.SEARCH_INDEX)).hasValueSatisfying(run -> assertThat(run.succeeded()).isFalse());
    }

    @Test
    void should_not_start_a_job_that_is_running() {
        final CompletableFuture<Void> work = new CompletableFuture<>();
        final AtomicBoolean ranAgain = new AtomicBoolean();

        tracker.runAsync(MaintenanceJob.ADDRESS_GEOCODING, () -> work);

        assertThat(tracker.run(MaintenanceJob.ADDRESS_GEOCODING, () -> ranAgain.set(true))).isFalse();
        assertThat(ranAgain).isFalse();

        work.complete(null);

        assertThat(tracker.run(MaintenanceJob.ADDRESS_GEOCODING, () -> ranAgain.set(true))).isTrue();
        assertThat(ranAgain).isTrue();
    }

    @Test
    void should_have_no_run_before_the_first() {
        assertThat(tracker.lastRun(MaintenanceJob.NEWS_PUBLISHING)).isEmpty();
    }
}
