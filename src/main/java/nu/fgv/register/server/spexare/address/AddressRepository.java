package nu.fgv.register.server.spexare.address;

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
public interface AddressRepository extends JpaRepository<Address, Long>, QuerydslPredicateExecutor<Address> {

    Page<Address> findBySpexare(Spexare spexare, Pageable pageable);

    @Query("""
              SELECT a FROM Address a
              JOIN a.type t
              WHERE a.spexare = :spexare
              AND t.type = :type
            """)
    Page<Address> findBySpexareAndType(@Param("spexare") Spexare spexare, @Param("type") TypeType type, Pageable pageable);

    boolean existsBySpexareAndType(Spexare spexare, Type type);

    boolean existsBySpexareAndTypeAndId(Spexare spexare, Type type, Long id);

}
