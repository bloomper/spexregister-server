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

package nu.fgv.register.server.sync;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spexipedia tells Spexregister that its change feed has something new; the call only starts a pull and carries no
 * data. Requires {@code NOTIFY} on {@code spexregister}.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@RestController
public class SyncApi {

    private final SyncService service;

    public SyncApi(final SyncService service) {
        this.service = service;
    }

    @PostMapping("/api/sync/notify")
    @PreAuthorize("hasRole('spexregister_NOTIFY')")
    public ResponseEntity<Void> notifyChanged() {
        service.pullSoon();
        return ResponseEntity.accepted().build();
    }
}
