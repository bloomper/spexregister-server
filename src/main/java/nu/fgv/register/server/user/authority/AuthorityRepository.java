package nu.fgv.register.server.user.authority;

import nu.fgv.register.server.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorityRepository extends JpaRepository<Authority, String>, QuerydslPredicateExecutor<User> {
}
