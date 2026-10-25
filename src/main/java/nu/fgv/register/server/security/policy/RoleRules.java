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

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Which roles hold which {@link Permission}. Roles given {@link Permission#ADMINISTRATION} hold every permission.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
public final class RoleRules {

    private final Map<Permission, Set<String>> roles;

    private RoleRules(final Map<Permission, Set<String>> roles) {
        this.roles = roles;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @param <T> the entity type
     * @return a restriction that matches every row
     */
    public static <T> Specification<T> all() {
        return (_, _, criteriaBuilder) -> criteriaBuilder.conjunction();
    }

    /**
     * @param <T> the entity type
     * @return a restriction that matches no row
     */
    public static <T> Specification<T> none() {
        return (_, _, criteriaBuilder) -> criteriaBuilder.disjunction();
    }

    /**
     * @param caller     who is asking
     * @param permission the permission asked for
     * @return whether one of the caller's roles holds the permission or administration
     */
    public boolean allows(final Caller caller, final Permission permission) {
        return caller.hasAnyRole(roles.getOrDefault(permission, Set.of()))
                || caller.hasAnyRole(roles.getOrDefault(Permission.ADMINISTRATION, Set.of()));
    }

    public static final class Builder {

        private final Map<Permission, Set<String>> roles = new EnumMap<>(Permission.class);

        public Builder read(final String... names) {
            return with(Permission.READ, names);
        }

        public Builder write(final String... names) {
            return with(Permission.WRITE, names);
        }

        public Builder delete(final String... names) {
            return with(Permission.DELETE, names);
        }

        public Builder administer(final String... names) {
            return with(Permission.ADMINISTRATION, names);
        }

        private Builder with(final Permission permission, final String... names) {
            roles.put(permission, Set.of(names));
            return this;
        }

        public RoleRules build() {
            return new RoleRules(Map.copyOf(roles));
        }
    }
}
