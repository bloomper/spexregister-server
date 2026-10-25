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
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Repository
public interface SpexareRepository extends SearchEnabledJpaRepository<Spexare, Long>, JpaSpecificationExecutor<Spexare> {

    /**
     * {@link #findById}, refused when the caller may not read the spexare.
     *
     * @param id the spexare id
     * @return the spexare, or empty when there is none
     * @throws org.springframework.security.access.AccessDeniedException when the spexare exists but may not be read
     */
    @PostAuthorize("!returnObject.isEmpty() ? (hasPermission(returnObject.get(), 'READ') or hasPermission(returnObject.get(), 'ADMINISTRATION')) : true")
    default Optional<Spexare> findById0(final Long id) {
        return this
                .findById(id);
    }

    /**
     * @param externalId the Keycloak subject of a user
     * @return the user's spexare, or empty when the user has none
     */
    @PostAuthorize("!returnObject.isEmpty() ? (hasPermission(returnObject.get(), 'READ') or hasPermission(returnObject.get(), 'ADMINISTRATION')) : true")
    Optional<Spexare> findByUserExternalId(final String externalId);

    /**
     * @param ids spexare ids
     * @return those of the spexare that have a partner, with the partner fetched
     */
    @Query("SELECT s FROM Spexare s JOIN FETCH s.partner WHERE s.id IN :ids")
    List<Spexare> findWithPartnerByIdIn(Collection<Long> ids);

    /**
     * @param ids spexare ids
     * @return the spexare with their tags fetched
     */
    @Query("SELECT DISTINCT s FROM Spexare s LEFT JOIN FETCH s.tags WHERE s.id IN :ids")
    List<Spexare> findWithTagsByIdIn(Collection<Long> ids);

    long countByPublishedTrue();

    long countByDataQualityIssue(DataQualityIssue issue);
}
