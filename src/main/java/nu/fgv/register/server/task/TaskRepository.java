package nu.fgv.register.server.task;

import nu.fgv.register.server.spex.Spex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("""
                SELECT t FROM Task t
                 WHERE t.id IN :ids
            """)
    List<Task> findByIds(@Param("ids") List<Long> ids, Sort sort);

}
