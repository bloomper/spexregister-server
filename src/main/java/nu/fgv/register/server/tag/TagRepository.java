package nu.fgv.register.server.tag;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long>, QuerydslPredicateExecutor<Tag> {

    @Query("""
              SELECT t FROM Tag t
              WHERE t.id IN :ids
            """)
    List<Tag> findByIds(@Param("ids") List<Long> ids, Sort sort);
}
