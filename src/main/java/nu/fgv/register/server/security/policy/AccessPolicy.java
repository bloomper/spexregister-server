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

package nu.fgv.register.server.security.policy;

import org.springframework.data.jpa.domain.Specification;

/**
 * The access rules for one entity type, in two forms that must agree: a query restriction for lists and a check for a
 * single entity.
 *
 * @param <T> the entity type
 * @author Anders Jacobsson
 * @since 2.0
 */
public interface AccessPolicy<T> {

    /**
     * @return the entity type the policy is for
     */
    Class<T> type();

    /**
     * The entities the caller has the permission on.
     *
     * @param caller     who is asking
     * @param permission the permission asked for
     * @return a restriction to add to a query
     */
    Specification<T> permitted(Caller caller, Permission permission);

    /**
     * Whether the caller has the permission on this entity.
     *
     * @param caller     who is asking
     * @param entity     the loaded entity
     * @param permission the permission asked for
     * @return whether the caller has the permission
     */
    boolean permits(Caller caller, T entity, Permission permission);
}
