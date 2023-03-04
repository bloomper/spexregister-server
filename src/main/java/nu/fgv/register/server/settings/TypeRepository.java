package nu.fgv.register.server.settings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TypeRepository extends JpaRepository<Type, Long>, QuerydslPredicateExecutor<Type> {
    List<Type> findByType(TypeType type);

    boolean existsByTypeAndValue(TypeType type, String value);

    Optional<Type> findByTypeAndValue(TypeType type, String value);
}
