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
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * A maintenance job and how its latest run went.
 *
 * @param job            the job
 * @param schedule       its cron expression, or null when it is not scheduled
 * @param running        whether it is running
 * @param lastStartedAt  when the latest run started, or null when it has not run since the application started
 * @param lastFinishedAt when the latest run finished
 * @param lastSucceeded  whether the latest run finished without an error
 */
public record MaintenanceJobStatusDto(MaintenanceJob job, @Nullable String schedule, boolean running,
                                      @Nullable Instant lastStartedAt, @Nullable Instant lastFinishedAt,
                                      @Nullable Boolean lastSucceeded) {
}
