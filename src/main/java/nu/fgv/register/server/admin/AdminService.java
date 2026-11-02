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

import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.impex.JobService;
import nu.fgv.register.server.map.GeocodingService;
import nu.fgv.register.server.news.NewsService;
import nu.fgv.register.server.user.UserService;
import nu.fgv.register.server.util.job.MaintenanceJob;
import nu.fgv.register.server.util.job.MaintenanceJobTracker;
import nu.fgv.register.server.util.search.IndexingService;
import nu.fgv.register.server.util.security.RequiresAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Administrative tasks: the scheduled maintenance jobs, which an administrator can also start by hand.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Service
public class AdminService {

    private static final String UNSCHEDULED = "-";

    private final MaintenanceJobTracker tracker;
    private final Environment environment;
    private final Map<MaintenanceJob, Runnable> jobs;
    private final Executor executor;

    @Autowired
    public AdminService(final MaintenanceJobTracker tracker, final Environment environment,
                        final IndexingService indexingService, final UserService userService,
                        final NewsService newsService, final JobService jobService,
                        final GeocodingService geocodingService) {
        this(tracker, environment, Map.of(
                MaintenanceJob.SEARCH_INDEX, indexingService::scheduledRun,
                MaintenanceJob.USER_SYNC, userService::scheduledSync,
                MaintenanceJob.NEWS_PUBLISHING, newsService::publishAndUnpublishNews,
                MaintenanceJob.JOB_CLEANUP, jobService::scheduledCleanup,
                MaintenanceJob.ADDRESS_GEOCODING, geocodingService::scheduledRun
        ), Executors.newVirtualThreadPerTaskExecutor());
    }

    AdminService(final MaintenanceJobTracker tracker, final Environment environment,
                 final Map<MaintenanceJob, Runnable> jobs, final Executor executor) {
        this.tracker = tracker;
        this.environment = environment;
        this.jobs = jobs;
        this.executor = executor;
    }

    /**
     * Rebuilds the spexare search index in the background.
     */
    @RequiresAdmin
    public void indexSpexare() {
        runJob(MaintenanceJob.SEARCH_INDEX);
    }

    /**
     * @return every maintenance job, with its schedule and latest run
     */
    @RequiresAdmin
    public List<MaintenanceJobStatusDto> jobs() {
        return Arrays.stream(MaintenanceJob.values()).map(this::status).toList();
    }

    /**
     * Starts the job in the background, as the schedule would.
     *
     * @param job the job
     * @return false when it is already running, and so was not started again
     */
    @RequiresAdmin
    public boolean runJob(final MaintenanceJob job) {
        if (tracker.isRunning(job)) {
            return false;
        }

        log.info("Administrator starting {}", job);
        executor.execute(() -> {
            try {
                jobs.get(job).run();
            } catch (final RuntimeException e) {
                log.error("{} started by an administrator failed", job, e);
            }
        });
        return true;
    }

    private MaintenanceJobStatusDto status(final MaintenanceJob job) {
        final String cron = environment.getProperty(job.cronProperty());
        final var lastRun = tracker.lastRun(job);

        return new MaintenanceJobStatusDto(
                job,
                cron == null || UNSCHEDULED.equals(cron) ? null : cron,
                tracker.isRunning(job),
                lastRun.map(MaintenanceJobTracker.Run::startedAt).orElse(null),
                lastRun.map(MaintenanceJobTracker.Run::finishedAt).orElse(null),
                lastRun.map(MaintenanceJobTracker.Run::succeeded).orElse(null)
        );
    }
}
