/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nu.fgv.register.server.spexare.tag;

import nu.fgv.register.server.tag.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Repository
public interface TaggingRepository extends JpaRepository<Tag, Long> {

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

    @Query(value = """
            SELECT COUNT(*) FROM tagging
            """,
            nativeQuery = true)
    long countTaggings();

}
