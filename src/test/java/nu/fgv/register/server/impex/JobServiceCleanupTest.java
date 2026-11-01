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

package nu.fgv.register.server.impex;

import nu.fgv.register.server.util.error.JobDeleteNotAllowedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Removing finished jobs with their files, and the scheduled clean-up of old jobs and files.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
class JobServiceCleanupTest {

    private final JobRepository jobRepository = mock(JobRepository.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final JobService service = new JobService(jobRepository, mock(JobOperator.class), mock(Job.class), mock(Job.class),
            mock(ApplicationContext.class), jdbcTemplate, mock(ObjectMapper.class), mock(JobProgressService.class));
    @TempDir
    private Path storage;

    private JobExecution execution(final BatchStatus status, final Path output) {
        final JobExecution execution = mock(JobExecution.class);
        final ExecutionContext context = new ExecutionContext();

        context.putString("outputFilePath", output.toString());
        when(execution.getId()).thenReturn(1L);
        when(execution.getStatus()).thenReturn(status);
        when(execution.getJobInstance()).thenReturn(new JobInstance(11L, "export"));
        when(execution.getExecutionContext()).thenReturn(context);
        when(execution.getJobParameters()).thenReturn(new JobParametersBuilder().addString("requestor", "owner").toJobParameters());
        when(jobRepository.getJobExecution(1L)).thenReturn(execution);
        return execution;
    }

    @BeforeEach
    void setUp() {
        final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("sub", "owner").build();

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        ReflectionTestUtils.setField(service, "storagePath", storage.toString());
        ReflectionTestUtils.setField(service, "purgeThresholdInDays", 30);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_not_delete_a_job_that_is_still_running() throws Exception {
        execution(BatchStatus.STARTED, Files.createFile(storage.resolve("out.xlsx")));

        assertThrows(JobDeleteNotAllowedException.class, () -> service.deleteJob(1L).block());
        verify(jdbcTemplate, never()).update(anyString(), eq(1L));
    }

    @Test
    void should_delete_a_finished_job_with_its_file_and_its_instance_when_nothing_else_uses_it() throws Exception {
        final Path output = Files.createFile(storage.resolve("out.xlsx"));

        execution(BatchStatus.COMPLETED, output);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(11L))).thenReturn(0);

        service.deleteJob(1L).block();

        assertThat(Files.exists(output), is(false));
        verify(jdbcTemplate).update("DELETE FROM BATCH_JOB_EXECUTION WHERE JOB_EXECUTION_ID = ?", 1L);
        verify(jdbcTemplate).update("DELETE FROM BATCH_JOB_INSTANCE WHERE JOB_INSTANCE_ID = ?", 11L);
    }

    @Test
    void should_purge_old_files_and_job_metadata_and_keep_recent_files() throws Exception {
        final Path exports = Files.createDirectories(storage.resolve("exports"));
        final Path old = Files.createFile(exports.resolve("old.xlsx"));
        final Path recent = Files.createFile(exports.resolve("recent.xlsx"));

        Files.setLastModifiedTime(old, FileTime.from(Instant.now().minus(40, ChronoUnit.DAYS)));

        service.scheduledCleanup();

        assertThat(Files.exists(old), is(false));
        assertThat(Files.exists(recent), is(true));
        verify(jdbcTemplate, atLeastOnce()).update(eq("DELETE FROM BATCH_JOB_EXECUTION WHERE CREATE_TIME < ?"), org.mockito.ArgumentMatchers.<Object>any());
        verify(jdbcTemplate).update("DELETE FROM BATCH_JOB_INSTANCE WHERE JOB_INSTANCE_ID NOT IN (SELECT JOB_INSTANCE_ID FROM BATCH_JOB_EXECUTION)");
    }
}
