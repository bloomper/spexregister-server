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

import nu.fgv.register.server.permission.PermissionGrants;
import nu.fgv.register.server.spex.Spex;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import static nu.fgv.register.server.security.policy.Caller.ADMIN;
import static nu.fgv.register.server.security.policy.Caller.EDITOR;
import static nu.fgv.register.server.security.policy.Caller.REVIVAL;
import static nu.fgv.register.server.security.policy.Caller.USER;

/**
 * Every role reads spex; editors and the revival service change them and administrators administer them. Permission
 * grants on a spex add rights on that spex.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
public class SpexPolicy implements AccessPolicy<Spex> {

    private static final RoleRules RULES = RoleRules.builder()
            .read(USER, EDITOR, ADMIN, REVIVAL)
            .write(EDITOR, REVIVAL)
            .administer(ADMIN)
            .build();

    private final PermissionGrants grants;

    public SpexPolicy(final PermissionGrants grants) {
        this.grants = grants;
    }

    @Override
    public Class<Spex> type() {
        return Spex.class;
    }

    @Override
    public Specification<Spex> permitted(final Caller caller, final Permission permission) {
        return RULES.allows(caller, permission) ? RoleRules.all() : grants.coveringSpex(caller, permission);
    }

    @Override
    public boolean permits(final Caller caller, final Spex spex, final Permission permission) {
        return RULES.allows(caller, permission) || grants.covers(caller, spex, permission);
    }
}
