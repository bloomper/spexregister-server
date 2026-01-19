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
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Service
public class JobProgressService implements JobExecutionListener {

    private final Sinks.Many<JobStatusDto> sink = Sinks.many().replay().latest();

    @Override
    public void beforeJob(final JobExecution jobExecution) {
        broadcast(jobExecution);
    }

    @Override
    public void afterJob(final JobExecution jobExecution) {
        broadcast(jobExecution);
    }

    private void broadcast(final JobExecution jobExecution) {
        final JobStatusDto dto = JobStatusDto.builder()
                .id(jobExecution.getJobInstance().getInstanceId())
                .status(jobExecution.getStatus().name())
                .exitStatus(jobExecution.getExitStatus().getExitCode())
                .build();

        sink.emitNext(dto, (signalType, emissionException) ->
                emissionException == Sinks.EmitResult.FAIL_NON_SERIALIZED);
    }

    public Flux<JobStatusDto> getStream(final Long jobId) {
        return sink.asFlux()
                .filter(u -> u.getId().equals(jobId));
    }
}
