package nu.fgv.register.server.settings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TypeRepository extends JpaRepository<Type, String>, QuerydslPredicateExecutor<Type> {

    boolean existsByType(TypeType type);

    List<Type> findByType(TypeType type);

}
