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

import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.impex.model.JobStatusDto;
import nu.fgv.register.server.util.security.RequiresAdminOrEditor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@RestController
@RequestMapping(path = "/api/jobs", version = "1.0")
@RequiredArgsConstructor
public class JobApi {

    private final JobService jobService;
    private final JobProgressService progressService;

    @GetMapping("/{id}")
    @RequiresAdminOrEditor
    public Mono<ResponseEntity<JobStatusDto>> status(@PathVariable final Long id) {
        return jobService.getJobExecution(id)
                .map(execution -> ResponseEntity.ok(JobStatusDto.builder()
                        .id(id)
                        .status(execution.getStatus().name())
                        .exitStatus(execution.getExitStatus().getExitCode())
                        .build()));
    }

    @GetMapping(path = "/{id}/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequiresAdminOrEditor
    public Flux<JobStatusDto> progress(@PathVariable final Long id) {
        return jobService.getJobExecution(id)
                .flatMapMany(execution -> progressService.getStream(id));
    }

    @GetMapping("/{id}/results")
    @RequiresAdminOrEditor
    public Mono<ResponseEntity<Resource>> results(@PathVariable final Long id) {
        return jobService.getJobOutputFile(id)
                .map(resource -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource));
    }
}
