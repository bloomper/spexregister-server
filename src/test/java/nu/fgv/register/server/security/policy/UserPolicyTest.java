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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static nu.fgv.register.server.security.policy.PolicyTestSupport.OWN_SUBJECT;
import static nu.fgv.register.server.security.policy.PolicyTestSupport.caller;
import static nu.fgv.register.server.security.policy.PolicyTestSupport.permissions;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every role against every permission on one's own and someone else's account (data-model.md, Policies).
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
class UserPolicyTest {
    private final UserPolicy policy = new UserPolicy();

    @ParameterizedTest(name = "own={1}: {0} holds {2}")
    @CsvSource({
            // role,   own,   READ/WRITE/DELETE/ADMINISTRATION
            "ADMIN,    false, RWDA",
            "EDITOR,   true,  R---",
            "EDITOR,   false, ----",
            "USER,     true,  R---",
            "USER,     false, ----",
            "REVIVAL,  false, ----",
            "NONE,     false, ----"
    })
    void should_decide_user(final String role, final boolean own, final String flags) {
        final User user = new User();
        user.setExternalId(own ? OWN_SUBJECT : "someone-else");
        final Set<Permission> expected = permissions(flags);

        for (final Permission permission : Permission.values()) {
            assertThat(policy.permits(caller(role), user, permission))
                    .as("%s %s own=%s", role, permission, own)
                    .isEqualTo(expected.contains(permission));
        }
    }
}
