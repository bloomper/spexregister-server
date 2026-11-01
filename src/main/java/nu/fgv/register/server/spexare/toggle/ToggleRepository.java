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

package nu.fgv.register.server.spexare.toggle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
public interface ToggleRepository extends JpaRepository<Toggle, Long>, JpaSpecificationExecutor<Toggle> {

    /**
     * @param uuid the external id
     * @return the toggle with that external id, or empty when there is none
     */
    Optional<Toggle> findByUuid(UUID uuid);

    /**
     * @param uuids external ids
     * @return those that exist
     */
    List<Toggle> findByUuidIn(Collection<UUID> uuids);

    /**
     * @param uuid the external id
     * @return whether there is one with that external id
     */
    boolean existsByUuid(UUID uuid);
}
