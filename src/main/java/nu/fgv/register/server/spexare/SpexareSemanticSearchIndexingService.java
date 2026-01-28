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

package nu.fgv.register.server.spexare;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static nu.fgv.register.server.util.security.SecurityUtil.runAsSystem;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SpexareSemanticSearchIndexingService {

    private final SpexareRepository spexareRepository;
    private final SpexareEmbeddingDocumentBuilder documentBuilder;
    private final VectorStore vectorStore;

    @Async
    @Transactional
    public CompletableFuture<Void> reindex() {
        LocaleContextHolder.setLocale(Locale.of("sv"));

        final long startedAtNanos = System.nanoTime();
        final long total = spexareRepository.count();
        long processed = 0L;
        int page = 0;

        log.info("Starting semantic search reindex for Spexare. total={}, pageSize={}", total, 50);

        while (true) {
            final Page<Long> idPage = spexareRepository.findIds(PageRequest.of(page, 50));

            if (idPage.isEmpty()) {
                break;
            }

            final List<Spexare> spexareList = new ArrayList<>(spexareRepository.findAllById(idPage.getContent()));
            final List<Document> inputs = new ArrayList<>(spexareList.size());

            for (final Spexare spexare : spexareList) {
                inputs.add(new Document(documentBuilder.build(spexare)));
            }

            vectorStore.add(inputs);

            processed += idPage.getNumberOfElements();

            final double percent = total <= 0 ? 0.0 : (processed * 100.0 / total);

            log.info(
                    "Semantic search reindex progress: page={} processed={}/{} ({})",
                    page,
                    processed,
                    total,
                    total <= 0 ? "n/a" : "%.1f%%".formatted(percent)
            );

            if (!idPage.hasNext()) {
                break;
            }
            page++;
        }

        final Duration duration = Duration.ofNanos(System.nanoTime() - startedAtNanos);

        log.info("Semantic search reindex completed for Spexare. processed={} duration={}", processed, duration);

        return CompletableFuture.completedFuture(null);
    }

    @Scheduled(cron = "${spexregister.jobs.semantic-search-full-index.cron-expression}")
    @Transactional
    public void scheduledRun() {
        log.info("Starting semantic search full re-indexing job (async)");
        runAsSystem(() -> reindex().whenComplete((ignored, ex) -> {
            if (ex != null) {
                log.error("Semantic search full re-indexing job finished with errors", ex);
            } else {
                log.info("Semantic search full re-indexing job finished successfully");
            }
        }));
    }

}
