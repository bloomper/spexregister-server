package nu.fgv.register.server.spex;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
interface SpexDetailsRepository extends JpaRepository<SpexDetails, Long>, QuerydslPredicateExecutor<SpexDetails> {
}
