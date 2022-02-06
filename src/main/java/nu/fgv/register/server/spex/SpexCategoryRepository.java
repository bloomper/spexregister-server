package nu.fgv.register.server.spex;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SpexCategoryRepository extends JpaRepository<SpexCategory, Long>, QuerydslPredicateExecutor<SpexCategory> {
}