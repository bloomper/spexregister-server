package nu.fgv.register.server.spexare;

import nu.fgv.register.server.util.search.SearchEnabledJpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpexareRepository extends SearchEnabledJpaRepository<Spexare, Long>, QuerydslPredicateExecutor<Spexare> {

    @Query("""
              SELECT s FROM Spexare s
              WHERE s.id IN :ids
            """)
    List<Spexare> findByIds(@Param("ids") List<Long> ids, Sort sort);


}
