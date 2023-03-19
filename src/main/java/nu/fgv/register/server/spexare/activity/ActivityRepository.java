package nu.fgv.register.server.spexare.activity;

import nu.fgv.register.server.spexare.Spexare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long>, QuerydslPredicateExecutor<Activity> {

    Page<Activity> findBySpexare(Spexare spexare, Pageable pageable);

    boolean existsBySpexareAndId(Spexare spexare, Long id);

}
