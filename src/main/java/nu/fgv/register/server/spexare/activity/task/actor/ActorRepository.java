package nu.fgv.register.server.spexare.activity.task.actor;

import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.spexare.activity.task.TaskActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ActorRepository extends JpaRepository<Actor, Long>, QuerydslPredicateExecutor<Actor> {

    Page<Actor> findByTaskActivity(TaskActivity taskActivity, Pageable pageable);

    boolean existsByTaskActivityAndVocal(TaskActivity taskActivity, Type vocal);

    boolean existsByTaskActivityAndVocalAndId(TaskActivity taskActivity, Type vocal, Long id);
}
