package nu.fgv.register.server.spexare.toggle;

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
public interface ToggleRepository extends JpaRepository<Toggle, Long>, QuerydslPredicateExecutor<Toggle> {

    Page<Toggle> findBySpexare(Spexare spexare, Pageable pageable);

    @Query("""
              SELECT t FROM Toggle t
              JOIN t.type t2
              WHERE t.spexare = :spexare
              AND t2.type = :type
            """)
    Page<Toggle> findBySpexareAndType(@Param("spexare") Spexare spexare, @Param("type") TypeType type, Pageable pageable);

    boolean existsBySpexareAndType(Spexare spexare, Type type);

    boolean existsBySpexareAndTypeAndId(Spexare spexare, Type type, Long id);

}
