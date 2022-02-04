package nu.fgv.register.server.spex;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpexRepository extends JpaRepository<Spex, Long>, QuerydslPredicateExecutor<Spex> {

    @Query("SELECT s FROM Spex s WHERE s.parent = ?1")
    List<Spex> findAllRevivals(Long id);
}
