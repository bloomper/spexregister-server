package nu.fgv.register.server.spexare.consent;

import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.spexare.Spexare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsentRepository extends JpaRepository<Consent, Long>, QuerydslPredicateExecutor<Consent> {

    Page<Consent> findBySpexare(Spexare spexare, Pageable pageable);

    @Query("""
              SELECT c FROM Consent c
              JOIN c.type t
              WHERE c.spexare = :spexare
              AND t.type = :type
            """)
    Page<Consent> findBySpexareAndType(@Param("spexare") Spexare spexare, @Param("type") TypeType type, Pageable pageable);

    boolean existsBySpexareAndType(Spexare spexare, Type type);

    boolean existsBySpexareAndTypeAndId(Spexare spexare, Type type, Long id);

}
