package nu.fgv.register.server.event;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, QuerydslPredicateExecutor<Event> {

    @Query(value = """
              SELECT e FROM Event e
              WHERE e.createdAt >= :since
              AND e.source = :source
            """
    )
    List<Event> findAllBySourceSince(final Instant since, final Event.SourceType source, final Sort sort);
}
