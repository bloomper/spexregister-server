package nu.fgv.register.server.task.category;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskCategoryRepository extends JpaRepository<TaskCategory, Long>, QuerydslPredicateExecutor<TaskCategory> {

    @Query("""
              SELECT t FROM TaskCategory t
              WHERE t.id IN :ids
            """)
    List<TaskCategory> findByIds(@Param("ids") List<Long> ids, Sort sort);
}
