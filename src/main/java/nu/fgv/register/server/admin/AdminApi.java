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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.util.StringUtils.capitalize;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class AdminApi {

    private final IndexingService indexingService;

    @PostMapping(value = "/index/{entity}")
    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public ResponseEntity<Void> index(final @PathVariable String entity) {
        try {
            final Class<?> clazz = Class.forName(String.format("nu.fgv.register.%s.%s", entity.toLowerCase(), capitalize(entity))); // NOSONAR
            indexingService.initiateIndexingFor(clazz, true);
            return ResponseEntity.ok().build();
        } catch (final ClassNotFoundException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
