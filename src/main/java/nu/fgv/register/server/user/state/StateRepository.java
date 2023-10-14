package nu.fgv.register.server.user.state;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface StateRepository extends JpaRepository<State, String>, QuerydslPredicateExecutor<State> {
}
