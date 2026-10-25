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

import nu.fgv.register.server.user.User;
import nu.fgv.register.server.user.User_;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import static nu.fgv.register.server.security.policy.Caller.ADMIN;

/**
 * Administrators manage every user; anyone else reads only their own.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
public class UserPolicy implements AccessPolicy<User> {

    private static final RoleRules RULES = RoleRules.builder()
            .administer(ADMIN)
            .build();

    @Override
    public Class<User> type() {
        return User.class;
    }

    @Override
    public Specification<User> permitted(final Caller caller, final Permission permission) {
        if (RULES.allows(caller, permission)) {
            return RoleRules.all();
        }
        if (permission == Permission.READ && caller.subject() != null) {
            final String subject = caller.subject();

            return (root, _, criteriaBuilder) -> criteriaBuilder.equal(root.get(User_.externalId), subject);
        }

        return RoleRules.none();
    }

    @Override
    public boolean permits(final Caller caller, final User user, final Permission permission) {
        return RULES.allows(caller, permission)
                || (permission == Permission.READ && caller.subject() != null && caller.subject().equals(user.getExternalId()));
    }
}
