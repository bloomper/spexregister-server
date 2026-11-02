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

/**
 * The scheduled maintenance jobs, which an administrator can also start by hand.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
public enum MaintenanceJob {

    SEARCH_INDEX("spexregister.jobs.full-index.cron-expression"),
    USER_SYNC("spexregister.jobs.sync-users.cron-expression"),
    NEWS_PUBLISHING("spexregister.jobs.publish-unpublish-news.cron-expression"),
    JOB_CLEANUP("spexregister.jobs.job-cleanup.cron-expression"),
    ADDRESS_GEOCODING("spexregister.jobs.geocode-addresses.cron-expression");

    private final String cronProperty;

    MaintenanceJob(final String cronProperty) {
        this.cronProperty = cronProperty;
    }

    /**
     * @return the name of the property holding the job's cron expression
     */
    public String cronProperty() {
        return cronProperty;
    }
}
