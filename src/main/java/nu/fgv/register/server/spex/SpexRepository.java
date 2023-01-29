package nu.fgv.register.server.spex;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpexRepository extends JpaRepository<Spex, Long>, QuerydslPredicateExecutor<Spex> {

    List<Spex> findAllByParentIsNull(Sort sort);

    Page<Spex> findAllByParentIsNull(Pageable pageable);

    Page<Spex> findAllByParentIsNotNull(Pageable pageable);

    Page<Spex> findRevivalsByParent(Spex parent, Pageable pageable);

    List<Spex> findAllRevivalsByParent(Spex parent);

    boolean existsRevivalByParentAndYear(Spex parent, String year);

    Optional<Spex> findRevivalByParentAndYear(Spex parent, String year);

    @Query("""
                SELECT s FROM Spex s
                 WHERE s.id IN :ids
            """)
    List<Spex> findByIds(@Param("ids") List<Long> ids, Sort sort);

    @Query("""
            SELECT s FROM Spex s
             JOIN s.parent p
              WHERE p.id IN :parentIds
              """)
    List<Spex> findRevivalsByParentIds(@Param("parentIds") List<Long> parentIds, Sort sort);
}
