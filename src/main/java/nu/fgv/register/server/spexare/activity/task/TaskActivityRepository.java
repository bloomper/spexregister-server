package nu.fgv.register.server.spexare.activity.task;

import nu.fgv.register.server.spexare.activity.Activity;
import nu.fgv.register.server.task.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskActivityRepository extends JpaRepository<TaskActivity, Long>, QuerydslPredicateExecutor<TaskActivity> {

    Page<TaskActivity> findByActivity(Activity activity, Pageable pageable);

    boolean existsByActivityAndTask(Activity activity, Task task);

    boolean existsByActivityAndId(Activity activity, Long id);

}
