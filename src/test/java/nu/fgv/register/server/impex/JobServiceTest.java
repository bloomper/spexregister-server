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

import nu.fgv.register.server.impex.model.JobStatusDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_EDITOR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class JobServiceTest {

    private final JobRepository jobRepository = mock(JobRepository.class);
    private final JobProgressService progressService = mock(JobProgressService.class);
    private final JobService service = new JobService(
            jobRepository,
            mock(JobOperator.class),
            mock(Job.class),
            mock(Job.class),
            mock(ApplicationContext.class),
            mock(JdbcTemplate.class),
            mock(ObjectMapper.class),
            progressService
    );

    @BeforeEach
    void setUp() {
        final JobExecution execution = mock(JobExecution.class);

        when(execution.getJobParameters()).thenReturn(new JobParametersBuilder().addString("requestor", "owner").toJobParameters());
        when(jobRepository.getJobExecution(1L)).thenReturn(execution);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_stream_the_progress_of_the_callers_own_job() {
        final JobStatusDto started = JobStatusDto.builder().id(1L).name("import").status("STARTED").build();

        authenticateAs("owner");
        when(progressService.getStream(1L)).thenReturn(Flux.just(started));

        assertThat(service.getJobProgress(1L).collectList().block(), contains(started));
    }

    @Test
    void should_not_stream_the_progress_of_another_callers_job() {
        authenticateAs("someone-else");

        assertThrows(AccessDeniedException.class, () -> service.getJobProgress(1L).blockLast());
        verify(progressService, never()).getStream(1L);
    }

    private static void authenticateAs(final String subject) {
        final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("sub", subject).build();

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(ROLE_EDITOR))));
    }
}
