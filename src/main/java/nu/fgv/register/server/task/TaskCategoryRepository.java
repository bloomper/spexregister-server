package nu.fgv.register.server.task;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskCategoryRepository extends JpaRepository<TaskCategory, Long> {

    @Query("""
            SELECT t FROM TaskCategory t
             WHERE t.id IN :ids
             """)
    List<TaskCategory> findByIds(@Param("ids") List<Long> ids, Sort sort);
}
