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

@Slf4j
@RequiredArgsConstructor
@Service
public class IndexingService {

    private final EntityManager entityManager;

    @Transactional
    @Async
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
                    .thenAccept((action) -> log.info("All entities indexed")));
        }

        log.info("Not starting index due to existing documents (count: {})", count);

        return CompletableFuture.completedFuture(CompletableFuture
                .completedFuture(null)
                .thenAccept(i -> {
                }));
    }

    @Async
    @Scheduled(cron = "${spexregister.jobs.full-index.cron-expression}")
    public void scheduledRun() {
        initiateIndexingFor(Spexare.class, true);
    }
}
