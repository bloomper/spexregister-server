package nu.fgv.register.server.spex;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpexCategoryRepository extends JpaRepository<SpexCategory, Long>, QuerydslPredicateExecutor<SpexCategory> {

    @Query("""
              SELECT s FROM SpexCategory s
              WHERE s.id IN :ids
            """)
    List<SpexCategory> findByIds(@Param("ids") List<Long> ids, Sort sort);
}
