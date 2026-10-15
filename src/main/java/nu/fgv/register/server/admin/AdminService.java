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

package nu.fgv.register.server.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.util.search.IndexingService;
import nu.fgv.register.server.util.security.RequiresAdmin;
import org.springframework.stereotype.Service;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AdminService {

    private final IndexingService indexingService;

    @RequiresAdmin
    public void indexSpexare() {
        indexingService.initiateIndexingFor(Spexare.class, true)
                .whenComplete((ignored, ex) -> {
                    if (ex != null) {
                        log.error("Admin-triggered indexing finished with errors for {}", Spexare.class.getSimpleName(), ex);
                    } else {
                        log.info("Admin-triggered indexing finished successfully for {}", Spexare.class.getSimpleName());
                    }
                });
    }
}
