package nu.fgv.register.server.spexare.membership;

import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.spexare.Spexare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long>, QuerydslPredicateExecutor<Membership> {

    Page<Membership> findBySpexare(Spexare spexare, Pageable pageable);

    Page<Membership> findBySpexareAndType(Spexare spexare, Type type, Pageable pageable);

    boolean existsBySpexareAndTypeAndYear(Spexare spexare, Type type, String year);

    @Query("""
                SELECT m FROM Membership m
                JOIN m.spexare s
                WHERE s = :spexare
                AND m.type = :type
                AND m.year = :year
            """)
    Optional<Membership> findBySpexareAndTypeAndYear(@Param("spexare") Spexare spexare, @Param("type") Type type, @Param("year") String year);
}
