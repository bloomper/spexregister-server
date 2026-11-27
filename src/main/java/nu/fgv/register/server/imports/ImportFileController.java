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

package nu.fgv.register.server.imports;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * {@code HEAD} and {@code PUT /api/import/files/{sha256}}: the files import records refer to, uploaded once by content
 * ({@code IMPORT} only).
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@RestController
@PreAuthorize("hasRole('fgv-spexregister_IMPORT')")
public class ImportFileController {

    private final ImportFileStore store;

    public ImportFileController(final ImportFileStore store) {
        this.store = store;
    }

    @RequestMapping(path = "/api/import/files/{sha256}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> has(@PathVariable final String sha256) {
        return store.has(ImportFileStore.normalize(sha256)) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @RequestMapping(path = "/api/import/files/{sha256}", method = RequestMethod.PUT)
    public ResponseEntity<Void> put(@PathVariable final String sha256, final HttpServletRequest request) throws IOException {
        return ResponseEntity.status(store.put(sha256, request.getInputStream()) ? HttpStatus.CREATED : HttpStatus.OK).build();
    }
}
