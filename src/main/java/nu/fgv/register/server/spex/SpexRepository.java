package nu.fgv.register.server.spex;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpexRepository extends JpaRepository<Spex, Long>, QuerydslPredicateExecutor<Spex> {

    Page<Spex> findAllByParentIsNull(Pageable pageable);

    Page<Spex> findAllByParentIsNotNull(Pageable pageable);

    Page<Spex> findRevivalsByParent(Spex parent, Pageable pageable);

    boolean existsRevivalByParentAndYear(Spex parent, String year);

    Optional<Spex> findRevivalByParentAndYear(Spex parent, String year);
}
