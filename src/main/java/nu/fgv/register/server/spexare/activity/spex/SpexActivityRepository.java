package nu.fgv.register.server.spexare.activity.spex;

import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spexare.activity.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SpexActivityRepository extends JpaRepository<SpexActivity, Long>, QuerydslPredicateExecutor<SpexActivity> {

    Page<SpexActivity> findByActivity(Activity activity, Pageable pageable);

    boolean existsByActivityAndSpex(Activity activity, Spex spex);

    boolean existsByActivityAndId(Activity activity, Long id);

}
