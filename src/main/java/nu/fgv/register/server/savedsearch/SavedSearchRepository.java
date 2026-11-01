/*
 * Copyright 2026 the original author or authors.
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

package nu.fgv.register.server.savedsearch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Saved searches are private to their owner, so every lookup is scoped by owner rather than by an access policy.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Repository
public interface SavedSearchRepository extends JpaRepository<SavedSearch, Long> {

    /**
     * @param uuid the external id
     * @return the savedSearch with that external id, or empty when there is none
     */
    Optional<SavedSearch> findByUuid(UUID uuid);

    /**
     * @param uuids external ids
     * @return those that exist
     */
    List<SavedSearch> findByUuidIn(Collection<UUID> uuids);

    /**
     * @param uuid the external id
     * @return whether there is one with that external id
     */
    boolean existsByUuid(UUID uuid);

    List<SavedSearch> findByOwnerExternalIdOrderByNameAsc(String ownerExternalId);

    Optional<SavedSearch> findByUuidAndOwnerExternalId(UUID uuid, String ownerExternalId);

    boolean existsByOwnerExternalIdAndNameIgnoreCase(String ownerExternalId, String name);
}
