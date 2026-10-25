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

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import nu.fgv.register.server.permission.PermissionGrants;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.Spexare_;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.user.User_;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import static nu.fgv.register.server.security.policy.Caller.ADMIN;
import static nu.fgv.register.server.security.policy.Caller.EDITOR;
import static nu.fgv.register.server.security.policy.Caller.REVIVAL;
import static nu.fgv.register.server.security.policy.Caller.USER;

/**
 * Editors and the revival service read and change every spexare, administrators administer them, and users read the
 * published ones. A user also reads and changes their own spexare and their partner's. Permission grants add rights on
 * one spexare or on everyone who took part in a spex.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
public class SpexarePolicy implements AccessPolicy<Spexare> {

    private static final RoleRules RULES = RoleRules.builder()
            .read(EDITOR, ADMIN, REVIVAL)
            .write(EDITOR, REVIVAL)
            .administer(ADMIN)
            .build();

    private final PermissionGrants grants;

    public SpexarePolicy(final PermissionGrants grants) {
        this.grants = grants;
    }

    @Override
    public Class<Spexare> type() {
        return Spexare.class;
    }

    @Override
    public Specification<Spexare> permitted(final Caller caller, final Permission permission) {
        if (RULES.allows(caller, permission)) {
            return RoleRules.all();
        }

        Specification<Spexare> permitted = RoleRules.none();

        if (permission == Permission.READ && caller.hasRole(USER)) {
            permitted = permitted.or(isPublished());
        }
        if (isOwnRecordPermission(permission) && caller.subject() != null) {
            permitted = permitted.or(isOwnOrPartners(caller.subject()));
        }

        return permitted.or(grants.coveringSpexare(caller, permission));
    }

    @Override
    public boolean permits(final Caller caller, final Spexare spexare, final Permission permission) {
        return RULES.allows(caller, permission)
                || (permission == Permission.READ && caller.hasRole(USER) && Boolean.TRUE.equals(spexare.getPublished()))
                || (isOwnRecordPermission(permission) && caller.subject() != null
                && (isUserOf(spexare, caller.subject()) || (spexare.getPartner() != null && isUserOf(spexare.getPartner(), caller.subject()))))
                || grants.covers(caller, spexare, permission);
    }

    private static boolean isOwnRecordPermission(final Permission permission) {
        return permission == Permission.READ || permission == Permission.WRITE;
    }

    private static boolean isUserOf(final Spexare spexare, final String subject) {
        final @Nullable User user = spexare.getUser();

        return user != null && subject.equals(user.getExternalId());
    }

    private static Specification<Spexare> isOwnOrPartners(final String subject) {
        return (root, _, criteriaBuilder) -> {
            final Join<Spexare, User> user = root.join(Spexare_.user, JoinType.LEFT);
            final Join<Spexare, Spexare> partner = root.join(Spexare_.partner, JoinType.LEFT);
            final Join<Spexare, User> partnerUser = partner.join(Spexare_.user, JoinType.LEFT);

            return criteriaBuilder.or(
                    criteriaBuilder.equal(user.get(User_.externalId), subject),
                    criteriaBuilder.equal(partnerUser.get(User_.externalId), subject));
        };
    }

    private static Specification<Spexare> isPublished() {
        return (root, _, criteriaBuilder) -> criteriaBuilder.isTrue(root.get(Spexare_.published));
    }
}
