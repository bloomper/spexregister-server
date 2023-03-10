package nu.fgv.register.server.spexare.membership;

import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.spexare.Spexare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long>, QuerydslPredicateExecutor<Membership> {

    Page<Membership> findBySpexare(Spexare spexare, Pageable pageable);

    boolean existsBySpexareAndTypeAndYear(Spexare spexare, Type type, String year);

    boolean existsBySpexareAndTypeAndId(Spexare spexare, Type type, Long id);

}
