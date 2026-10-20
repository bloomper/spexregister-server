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

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;

/**
 * Callers and expectations for the table-driven policy tests.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
final class PolicyTestSupport {
    static final String OWN_SUBJECT = "own-subject";

    /**
     * Grants that cover nothing, for testing the rules alone.
     */
    static PermissionGrants noGrants() {
        return mock(PermissionGrants.class);
    }

    private PolicyTestSupport() {
    }

    /**
     * A caller from a role name: {@code ADMIN}, {@code EDITOR}, {@code USER}, {@code REVIVAL} or {@code NONE}.
     */
    static Caller caller(final String role) {
        return "NONE".equals(role) ? new Caller(Set.of(), OWN_SUBJECT) : new Caller(Set.of(role), OWN_SUBJECT);
    }

    /**
     * The permissions a role holds, as a string like {@code "RWDA"} or {@code "R---"}.
     */
    static Set<Permission> permissions(final String flags) {
        return Arrays.stream(Permission.values())
                .filter(permission -> flags.charAt(permission.ordinal()) != '-')
                .collect(Collectors.toSet());
    }
}
