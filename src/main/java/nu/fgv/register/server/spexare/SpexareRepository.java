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

package nu.fgv.register.server.spexare;

import nu.fgv.register.server.util.search.SearchEnabledJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Repository
public interface SpexareRepository extends SearchEnabledJpaRepository<Spexare, Long>, JpaSpecificationExecutor<Spexare> {

    @PostAuthorize("!returnObject.isEmpty() ? (hasPermission(returnObject.get(), 'READ') or hasPermission(returnObject.get(), 'ADMINISTRATION')) : true")
    default Optional<Spexare> findById0(final Long id) {
        return this
                .findById(id);
    }

    long countByPublishedTrue();

    @Query("select s.id from Spexare s")
    Page<Long> findIds(Pageable pageable);

    @EntityGraph(attributePaths = {
            "addresses",
            "addresses.type",
            "consents",
            "consents.type",
            "memberships",
            "memberships.type",
            "tags",
            "toggles",
            "toggles.type",
            "activities",
            "activities.spexActivity",
            "activities.spexActivity.spex",
            "activities.spexActivity.spex.details",
            "activities.spexActivity.spex.details.category",
            "activities.taskActivities",
            "activities.taskActivities.task",
            "activities.taskActivities.task.category",
            "activities.taskActivities.actors",
            "activities.taskActivities.actors.vocal"
    })
    @Query("select distinct s from Spexare s where s.id in :ids")
    List<Spexare> findAllByIdInWithGraph(@Param("ids") List<Long> ids);

}
