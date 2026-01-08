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

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.Spexare;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static nu.fgv.register.server.util.security.SecurityUtil.runAsSystem;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class IndexingService {

    private final EntityManager entityManager;

    @Async
    @Transactional
    public CompletableFuture<CompletionStage<Void>> initiateIndexingFor(final Class<?> clazz, final boolean force) {
        log.info("Initiating indexing for {}", clazz.getSimpleName());

        final SearchSession searchSession = Search.session(entityManager);
        final long count = searchSession
                .search(clazz)
                .where(f -> f.bool().with(b -> b.must(f.matchAll())))
                .fetchTotalHitCount();

        if (force || count == 0) {
            return CompletableFuture
                    .completedFuture(searchSession.massIndexer()
                    .start()
                    .thenAccept(action -> log.info("All entities indexed")));
        }

        log.info("Not starting index due to existing documents (count: {})", count);

        return CompletableFuture.completedFuture(CompletableFuture
                .completedFuture(null)
                .thenAccept(i -> {
                }));
    }

    @Scheduled(cron = "${spexregister.jobs.full-index.cron-expression}")
    @Transactional
    public void scheduledRun() {
        log.info("Starting full re-indexing job");
        runAsSystem(() -> {
            initiateIndexingFor(Spexare.class, true);
            log.info("Finished full re-indexing job");
        });
    }
}
