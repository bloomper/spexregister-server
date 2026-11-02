/*
 * Copyright 2024 the original author or authors.
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

import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.util.job.MaintenanceJob;
import nu.fgv.register.server.util.security.RequiresAdmin;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The administrative REST API under {@code /api/admin}.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class AdminApi {

    private final AdminService service;

    @PostMapping("/index/spexare")
    @RequiresAdmin
    public ResponseEntity<Void> indexSpexare() {
        service.indexSpexare();

        return ResponseEntity.accepted().build();
    }

    @GetMapping("/jobs")
    @RequiresAdmin
    public ResponseEntity<List<MaintenanceJobStatusDto>> jobs() {
        return ResponseEntity.ok(service.jobs());
    }

    @PostMapping("/jobs/{job}")
    @RequiresAdmin
    public ResponseEntity<Void> runJob(@PathVariable final MaintenanceJob job) {
        return service.runJob(job) ? ResponseEntity.accepted().build() : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
}
