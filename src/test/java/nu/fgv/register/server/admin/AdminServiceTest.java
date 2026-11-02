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


package nu.fgv.register.server.admin;

import nu.fgv.register.server.util.job.MaintenanceJob;
import nu.fgv.register.server.util.job.MaintenanceJobTracker;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class AdminServiceTest {

    private final MaintenanceJobTracker tracker = new MaintenanceJobTracker();
    private final AtomicInteger userSyncs = new AtomicInteger();
    private final MockEnvironment environment = new MockEnvironment()
            .withProperty(MaintenanceJob.USER_SYNC.cronProperty(), "0 */5 * * * ?")
            .withProperty(MaintenanceJob.JOB_CLEANUP.cronProperty(), "-");
    private final AdminService service = new AdminService(tracker, environment, Map.of(
            MaintenanceJob.USER_SYNC, () -> tracker.run(MaintenanceJob.USER_SYNC, userSyncs::incrementAndGet),
            MaintenanceJob.SEARCH_INDEX, () -> tracker.run(MaintenanceJob.SEARCH_INDEX, () -> {
            })
    ), Runnable::run);

    @Test
    void should_run_a_job() {
        assertThat(service.runJob(MaintenanceJob.USER_SYNC)).isTrue();

        assertThat(userSyncs).hasValue(1);
    }

    @Test
    void should_not_run_a_job_that_is_running() {
        tracker.runAsync(MaintenanceJob.USER_SYNC, CompletableFuture::new);

        assertThat(service.runJob(MaintenanceJob.USER_SYNC)).isFalse();
        assertThat(userSyncs).hasValue(0);
    }

    @Test
    void should_keep_running_when_a_job_fails() {
        final AdminService failing = new AdminService(tracker, environment, Map.of(
                MaintenanceJob.NEWS_PUBLISHING, () -> {
                    throw new IllegalStateException("boom");
                }
        ), Runnable::run);

        assertThat(failing.runJob(MaintenanceJob.NEWS_PUBLISHING)).isTrue();
    }

    @Test
    void should_index_spexare_as_the_search_index_job() {
        service.indexSpexare();

        assertThat(tracker.lastRun(MaintenanceJob.SEARCH_INDEX)).isPresent();
    }

    @Test
    void should_list_every_job_with_its_schedule_and_latest_run() {
        service.runJob(MaintenanceJob.USER_SYNC);

        assertThat(service.jobs())
                .extracting(MaintenanceJobStatusDto::job)
                .containsExactly(MaintenanceJob.values());
        assertThat(service.jobs())
                .filteredOn(status -> status.job() == MaintenanceJob.USER_SYNC)
                .singleElement()
                .satisfies(status -> {
                    assertThat(status.schedule()).isEqualTo("0 */5 * * * ?");
                    assertThat(status.running()).isFalse();
                    assertThat(status.lastStartedAt()).isNotNull();
                    assertThat(status.lastSucceeded()).isTrue();
                });
        assertThat(service.jobs())
                .filteredOn(status -> status.job() == MaintenanceJob.JOB_CLEANUP)
                .singleElement()
                .satisfies(status -> {
                    assertThat(status.schedule()).isNull();
                    assertThat(status.lastStartedAt()).isNull();
                });
    }
}
