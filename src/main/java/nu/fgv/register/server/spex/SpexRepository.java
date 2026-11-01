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

package nu.fgv.register.server.spex;

import nu.fgv.register.server.security.policy.PolicyJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Repository
public interface SpexRepository extends PolicyJpaRepository<Spex, Long>, JpaSpecificationExecutor<Spex> {

    /**
     * @param uuid the external id
     * @return the spex with that external id, or empty when there is none
     */
    Optional<Spex> findByUuid(UUID uuid);

    /**
     * @param uuids external ids
     * @return those that exist
     */
    List<Spex> findByUuidIn(Collection<UUID> uuids);

    /**
     * @param uuid the external id
     * @return whether there is one with that external id
     */
    boolean existsByUuid(UUID uuid);

    /**
     * {@link #findByUuid}, refused when the caller may not read the spex.
     *
     * @param id the spex id
     * @return the spex, or empty when there is none
     * @throws org.springframework.security.access.AccessDeniedException when the spex exists but may not be read
     */
    @PostAuthorize("!returnObject.isEmpty() ? (hasPermission(returnObject.get(), 'READ') or hasPermission(returnObject.get(), 'ADMINISTRATION')) : true")
    default Optional<Spex> findByUuid0(final UUID id) {
        return this
                .findByUuid(id);
    }

    /**
     * @param details shared details
     * @return the original spex of the details, without its revivals
     */
    Optional<Spex> findByDetailsAndParentIsNull(SpexDetails details);

    long countByParentIsNull();

    long countByParentIsNotNull();
}
