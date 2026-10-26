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

package nu.fgv.register.server.impex.util;

import nu.fgv.register.server.audit.Auditor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.security.core.context.SecurityContextHolder;

import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_EDITOR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class SecurityContextStepListenerTest {

    private final SecurityContextStepListener listener = new SecurityContextStepListener();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_name_the_requestor_by_email_in_the_audit() {
        final StepExecution stepExecution = stepExecution(new JobParametersBuilder()
                .addString("requestor", "subject")
                .addString("authorities", ROLE_EDITOR)
                .addString("requestorEmail", "editor@example.com", false)
                .addString("requestorUsername", "editor", false)
                .addString("requestorClient", "spexregister", false));

        listener.beforeStep(stepExecution);

        assertThat(Auditor.current(), equalTo("editor@example.com"));
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName(), equalTo("subject"));
    }

    @Test
    void should_run_without_claims_from_jobs_started_before_they_were_kept() {
        final StepExecution stepExecution = stepExecution(new JobParametersBuilder()
                .addString("requestor", "subject")
                .addString("authorities", ROLE_EDITOR));

        listener.beforeStep(stepExecution);

        assertThat(Auditor.current(), equalTo(""));
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName(), equalTo("subject"));
    }

    private static StepExecution stepExecution(final JobParametersBuilder parameters) {
        final StepExecution stepExecution = mock(StepExecution.class);

        when(stepExecution.getJobParameters()).thenReturn(parameters.toJobParameters());
        return stepExecution;
    }
}
