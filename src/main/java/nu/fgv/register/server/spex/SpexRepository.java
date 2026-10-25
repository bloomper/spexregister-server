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

import java.util.Optional;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Repository
public interface SpexRepository extends PolicyJpaRepository<Spex, Long>, JpaSpecificationExecutor<Spex> {

    /**
     * {@link #findById}, refused when the caller may not read the spex.
     *
     * @param id the spex id
     * @return the spex, or empty when there is none
     * @throws org.springframework.security.access.AccessDeniedException when the spex exists but may not be read
     */
    @PostAuthorize("!returnObject.isEmpty() ? (hasPermission(returnObject.get(), 'READ') or hasPermission(returnObject.get(), 'ADMINISTRATION')) : true")
    default Optional<Spex> findById0(final Long id) {
        return this
                .findById(id);
    }

    /**
     * @param details shared details
     * @return the original spex of the details, without its revivals
     */
    Optional<Spex> findByDetailsAndParentIsNull(SpexDetails details);

    long countByParentIsNull();

    long countByParentIsNotNull();
}
