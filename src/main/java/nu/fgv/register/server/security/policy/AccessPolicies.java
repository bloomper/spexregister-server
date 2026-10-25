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

import org.hibernate.Hibernate;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The registry of every {@link AccessPolicy}, keyed by entity type. Also reachable statically through {@link #get()}
 * for code Spring does not create, such as {@link PolicyPermissionEvaluator} and {@link SimplePolicyJpaRepository}.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
public class AccessPolicies {

    private static volatile @Nullable AccessPolicies instance;

    private final Map<Class<?>, AccessPolicy<?>> policies;

    public AccessPolicies(final List<AccessPolicy<?>> policies) {
        this.policies = policies.stream().collect(Collectors.toUnmodifiableMap(AccessPolicy::type, Function.identity()));
        instance = this;
    }

    /**
     * The instance Spring created.
     *
     * @return the registry
     * @throws IllegalStateException when called before the application context has started
     */
    public static AccessPolicies get() {
        final AccessPolicies current = instance;

        if (current == null) {
            throw new IllegalStateException("Access policies are not initialised");
        }

        return current;
    }

    /**
     * The policy for the type or its nearest superclass with one, so Hibernate proxies and subclasses resolve.
     *
     * @param type the entity type
     * @param <T>  the entity type
     * @return the policy
     * @throws org.springframework.security.access.AccessDeniedException when the type has no policy, so it is denied
     *     rather than allowed
     */
    @SuppressWarnings("unchecked")
    public <T> AccessPolicy<T> forType(final Class<T> type) {
        for (Class<?> candidate = type; candidate != null && candidate != Object.class; candidate = candidate.getSuperclass()) {
            final AccessPolicy<?> policy = policies.get(candidate);

            if (policy != null) {
                return (AccessPolicy<T>) policy;
            }
        }

        throw new AccessDeniedException("No access policy for " + type.getSimpleName());
    }

    /**
     * What the current caller may access with the permission, to combine with any other query criteria.
     *
     * @param type       the entity type
     * @param permission the permission asked for
     * @param <T>        the entity type
     * @return the restriction to add to a query
     */
    public <T> Specification<T> permitted(final Class<T> type, final Permission permission) {
        return forType(type).permitted(Caller.current(), permission);
    }

    /**
     * Whether the current caller has the permission on the entity, unproxied first so the right policy is used.
     *
     * @param entity     the entity, possibly a Hibernate proxy
     * @param permission the permission asked for
     * @param <T>        the entity type
     * @return whether the caller has the permission
     */
    @SuppressWarnings("unchecked")
    public <T> boolean permits(final T entity, final Permission permission) {
        final T target = (T) Hibernate.unproxy(entity);

        return forType((Class<T>) target.getClass()).permits(Caller.current(), target, permission);
    }
}
