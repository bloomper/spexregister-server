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

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Keeps one run of each maintenance job at a time and remembers how its latest run went, whether it was started by the
 * schedule or by an administrator. The state is held in memory, so it starts over when the application restarts.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Component
public class MaintenanceJobTracker {

    private final Set<MaintenanceJob> running = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<MaintenanceJob, Run> lastRuns = new ConcurrentHashMap<>();

    /**
     * Runs the work as the job unless the job is already running.
     *
     * @param job  the job
     * @param work the work, done in the calling thread
     * @return whether the work ran
     */
    public boolean run(final MaintenanceJob job, final Runnable work) {
        return runAsync(job, () -> {
            work.run();
            return CompletableFuture.completedFuture(null);
        });
    }

    /**
     * Starts the work as the job unless the job is already running. The job counts as running until the returned stage
     * completes.
     *
     * @param job  the job
     * @param work starts the work and returns its completion
     * @return whether the work was started
     */
    public boolean runAsync(final MaintenanceJob job, final Supplier<? extends CompletionStage<?>> work) {
        if (!running.add(job)) {
            log.info("Not starting {}, it is already running", job);
            return false;
        }

        final Instant startedAt = Instant.now();
        lastRuns.put(job, new Run(startedAt, null, null));

        try {
            work.get().whenComplete((ignored, e) -> finish(job, startedAt, e == null));
        } catch (final RuntimeException e) {
            finish(job, startedAt, false);
            throw e;
        }
        return true;
    }

    /**
     * @param job the job
     * @return whether the job is running
     */
    public boolean isRunning(final MaintenanceJob job) {
        return running.contains(job);
    }

    /**
     * @param job the job
     * @return the latest run since the application started, finished or not
     */
    public Optional<Run> lastRun(final MaintenanceJob job) {
        return Optional.ofNullable(lastRuns.get(job));
    }

    private void finish(final MaintenanceJob job, final Instant startedAt, final boolean succeeded) {
        lastRuns.put(job, new Run(startedAt, Instant.now(), succeeded));
        running.remove(job);
    }

    /**
     * A run of a job.
     *
     * @param startedAt  when it started
     * @param finishedAt when it finished, or null while running
     * @param succeeded  whether it finished without an error, or null while running
     */
    public record Run(Instant startedAt, @Nullable Instant finishedAt, @Nullable Boolean succeeded) {
    }
}
