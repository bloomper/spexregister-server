package nu.fgv.register.server.repository;

import nu.fgv.register.server.model.Spex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.graphql.data.GraphQlRepository;

@GraphQlRepository
public interface SpexRepository extends JpaRepository<Spex, Long>, QuerydslPredicateExecutor<Spex> {
}
