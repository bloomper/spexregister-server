package nu.fgv.register.server.spexare;

import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.SpexCategory;
import nu.fgv.register.server.spex.SpexDetails;
import nu.fgv.register.server.spexare.consent.Consent;
import nu.fgv.register.server.spexare.membership.Membership;
import nu.fgv.register.server.spexare.toggle.Toggle;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.task.Task;
import nu.fgv.register.server.task.TaskCategory;
import nu.fgv.register.server.util.search.SearchEnabledJpaRepository;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.data.domain.Pageable;
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
