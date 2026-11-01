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

package nu.fgv.register.server.user;

import nu.fgv.register.server.security.policy.PolicyJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
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
public interface UserRepository extends PolicyJpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * @param uuid the external id
     * @return the user with that external id, or empty when there is none
     */
    Optional<User> findByUuid(UUID uuid);

    /**
     * @param uuids external ids
     * @return those that exist
     */
    List<User> findByUuidIn(Collection<UUID> uuids);

    /**
     * @param uuid the external id
     * @return whether there is one with that external id
     */
    boolean existsByUuid(UUID uuid);

    @PostAuthorize("!returnObject.isEmpty() ? (hasPermission(returnObject.get(), 'READ') or hasPermission(returnObject.get(), 'ADMINISTRATION')) : true")
    default Optional<User> findByUuid0(final UUID id) {
        return this
                .findByUuid(id);
    }

    @PostAuthorize("!returnObject.isEmpty() ? (hasPermission(returnObject.get(), 'READ') or hasPermission(returnObject.get(), 'ADMINISTRATION')) : true")
    Optional<User> findByExternalId(final String externalId);

    boolean existsByExternalId(String externalId);

    @Query("SELECT u.state.id AS stateId, COUNT(u) AS count FROM User u GROUP BY u.state.id")
    List<StateCount> countByState();

    interface StateCount {
        String getStateId();

        long getCount();
    }
}
