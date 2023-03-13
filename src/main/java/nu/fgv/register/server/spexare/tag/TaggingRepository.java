package nu.fgv.register.server.spexare.tag;

import nu.fgv.register.server.tag.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaggingRepository extends JpaRepository<Tag, Long>, QuerydslPredicateExecutor<Tag> {

    @Query(value = """
              SELECT * FROM tag t
              JOIN tagging tg
              ON tg.tag_id = t.id
              WHERE tg.spexare_id = :spexareId
            """,
            countQuery = """
              SELECT COUNT(*) FROM tag t
              JOIN tagging tg
              ON tg.tag_id = t.id
              WHERE tg.spexare_id = :spexareId
            """,
            nativeQuery = true
    )
    Page<Tag> findBySpexareId(@Param("spexareId") Long spexareId, Pageable pageable);

    @Query(value = """
               SELECT
               CASE WHEN EXISTS (
                   SELECT 1
                   FROM tagging
                   WHERE spexare_id = :spexareId
                   AND tag_id = :tagId
               )
               THEN 'true'
               ELSE 'false'
               END
            """,
            nativeQuery = true
    )
    boolean existsBySpexareIdAndTagId(@Param("spexareId") Long spexareId, @Param("tagId") Long tagId);

}
