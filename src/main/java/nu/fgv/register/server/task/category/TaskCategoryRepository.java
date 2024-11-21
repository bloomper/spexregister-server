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

package nu.fgv.register.server.task.category;

import nu.fgv.register.server.acl.AclJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Repository
public interface TaskCategoryRepository extends AclJpaRepository<TaskCategory, Long>, JpaSpecificationExecutor<TaskCategory> {

    @PostAuthorize("!returnObject.isEmpty() ? (hasPermission(returnObject.get(), 'READ') or hasPermission(returnObject.get(), 'ADMINISTRATION')) : true")
    default Optional<TaskCategory> findById0(final Long id) {
        return this
                .findById(id);
    }
}
