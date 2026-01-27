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
import nu.fgv.register.server.config.SemanticSearchProperties;
import nu.fgv.register.server.util.semanticsearch.QdrantClient;
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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import static nu.fgv.register.server.util.semanticsearch.VectorUtil.toFloatList;
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
    private final SpexareEmbeddingService embeddingService;
    private final QdrantClient qdrantClient;
    private final SemanticSearchProperties props;

    @Async
    @Transactional
    public CompletableFuture<Void> reindex() {
        final long startedAtNanos = System.nanoTime();
        LocaleContextHolder.setLocale(Locale.of("sv"));

        final int pageSize = props.embedding().pageSize();
        final int batchSize = props.embedding().batchSize();

        final int maxEmbedConcurrency = 4; // 4..16
        final Semaphore embedPermits = new Semaphore(maxEmbedConcurrency);
        final AtomicBoolean collectionInitialized = new AtomicBoolean(false);

        final long total = spexareRepository.count();
        long processed = 0L;
        long skippedEmptyVector = 0L;
        long upsertedPoints = 0L;

        int page = 0;

        log.info(
                "Starting semantic search reindex for Spexare. total={}, pageSize={}, batchSize={}, embedConcurrency={}",
                total, pageSize, batchSize, maxEmbedConcurrency
        );

        try (final var embedExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            while (true) {
                final Page<Long> idPage = spexareRepository.findIds(PageRequest.of(page, pageSize));

                if (idPage.isEmpty()) {
                    break;
                }

                final List<Spexare> spexareList = new ArrayList<>(spexareRepository.findAllById(idPage.getContent()));
                final List<EmbeddingTaskInput> inputs = new ArrayList<>(spexareList.size());

                for (final Spexare spexare : spexareList) {
                    inputs.add(new EmbeddingTaskInput(
                            spexare.getId(),
                            spexare.getPublished(),
                            documentBuilder.build(spexare)
                    ));
                }

                final List<CompletableFuture<EmbeddingTaskOutput>> futures = new ArrayList<>(inputs.size());

                for (final EmbeddingTaskInput in : inputs) {
                    embedPermits.acquireUninterruptibly();

                    final CompletableFuture<EmbeddingTaskOutput> f = CompletableFuture
                            .supplyAsync(() -> {
                                final float[] vector = embeddingService.embed(in.document());

                                return new EmbeddingTaskOutput(in.id(), in.published(), vector);
                            }, embedExecutor)
                            .whenComplete((ignored, ex) -> embedPermits.release());

                    futures.add(f);
                }

                final List<QdrantClient.Point> batch = new ArrayList<>(batchSize);

                for (final CompletableFuture<EmbeddingTaskOutput> f : futures) {
                    final EmbeddingTaskOutput out = f.join();
                    final float[] vector = out.vector();

                    if (vector.length == 0) {
                        skippedEmptyVector++;
                        continue;
                    }

                    if (collectionInitialized.compareAndSet(false, true)) {
                        qdrantClient.ensureCollectionExists(vector.length);
                        log.info("Semantic search collection initialized (vectorSize={})", vector.length);
                    }

                    batch.add(new QdrantClient.Point(
                            out.id(),
                            toFloatList(vector),
                            Map.of(
                                    "spexareId", out.id(),
                                    "published", out.published()
                            )
                    ));
                    processed++;

                    if (batch.size() >= batchSize) {
                        qdrantClient.upsertPoints(batch);
                        upsertedPoints += batch.size();
                        batch.clear();
                    }
                }

                if (!batch.isEmpty()) {
                    qdrantClient.upsertPoints(batch);
                    upsertedPoints += batch.size();
                }

                final double percent = total <= 0 ? 0.0 : (processed * 100.0 / total);

                log.info(
                        "Semantic search reindex progress: page={} processed={}/{} ({}) upsertedPoints={} skippedEmptyVector={}",
                        page,
                        processed,
                        total,
                        total <= 0 ? "n/a" : "%.1f%%".formatted(percent),
                        upsertedPoints,
                        skippedEmptyVector
                );

                if (!idPage.hasNext()) {
                    break;
                }
                page++;
            }
        } catch (final Exception e) {
            final Duration duration = Duration.ofNanos(System.nanoTime() - startedAtNanos);

            log.error(
                    "Semantic search reindex FAILED for Spexare after {}. processed={} upsertedPoints={} skippedEmptyVector={}",
                    duration, processed, upsertedPoints, skippedEmptyVector, e
            );

            return CompletableFuture.failedFuture(e);
        }

        final Duration duration = Duration.ofNanos(System.nanoTime() - startedAtNanos);

        log.info(
                "Semantic search reindex completed for Spexare. processed={} upsertedPoints={} skippedEmptyVector={} duration={}",
                processed, upsertedPoints, skippedEmptyVector, duration
        );

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

    private record EmbeddingTaskInput(Long id, Boolean published, String document) {
    }

    private record EmbeddingTaskOutput(Long id, Boolean published, float[] vector) {
    }
}
