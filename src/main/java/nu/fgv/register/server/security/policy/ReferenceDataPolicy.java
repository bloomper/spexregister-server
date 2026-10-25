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
 * A policy that depends on role only, for reference data where every row is treated alike.
 *
 * @param <T> the entity type
 * @author Anders Jacobsson
 * @since 2.0
 */
public abstract class ReferenceDataPolicy<T> implements AccessPolicy<T> {

    private final Class<T> type;
    private final RoleRules rules;

    protected ReferenceDataPolicy(final Class<T> type, final RoleRules rules) {
        this.type = type;
        this.rules = rules;
    }

    @Override
    public Class<T> type() {
        return type;
    }

    @Override
    public Specification<T> permitted(final Caller caller, final Permission permission) {
        return rules.allows(caller, permission) ? RoleRules.all() : RoleRules.none();
    }

    @Override
    public boolean permits(final Caller caller, final T entity, final Permission permission) {
        return rules.allows(caller, permission);
    }
}
