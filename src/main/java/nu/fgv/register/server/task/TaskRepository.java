package nu.fgv.register.server.task;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, QuerydslPredicateExecutor<Task> {

    @Query("""
                SELECT t FROM Task t
                 WHERE t.id IN :ids
            """)
    List<Task> findByIds(@Param("ids") List<Long> ids, Sort sort);

}
