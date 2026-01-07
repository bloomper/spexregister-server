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

package nu.fgv.register.server.statistics;

import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/statistics", version = "1.0")
public class StatisticsApi {

    private final StatisticsService service;

    @GetMapping
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<StatisticsDto> getStatistics() {
        return ResponseEntity.ok(service.getStatistics());
    }

}
