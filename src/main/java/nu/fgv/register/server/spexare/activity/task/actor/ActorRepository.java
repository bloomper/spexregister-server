package nu.fgv.register.server.spexare.activity.task.actor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ActorRepository extends JpaRepository<Actor, Long>, QuerydslPredicateExecutor<Actor> {
}
