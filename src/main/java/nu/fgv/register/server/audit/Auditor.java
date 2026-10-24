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

package nu.fgv.register.server.audit;

import nu.fgv.register.server.security.policy.Caller;
import org.jspecify.annotations.Nullable;

import static nu.fgv.register.server.util.security.SecurityUtil.getCurrentAuthorizedPartyClaim;
import static nu.fgv.register.server.util.security.SecurityUtil.getCurrentUserEmailClaim;
import static nu.fgv.register.server.util.security.SecurityUtil.getCurrentUserPreferredUsernameClaim;
import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public final class Auditor {

    static final int MAX_LENGTH = 255;
    static final String ON_BEHALF_OF = " on behalf of ";

    private Auditor() {
    }

    public static String current() {
        final boolean revival = Caller.current().hasRole(Caller.REVIVAL);
        final String account = accountOf(revival);
        final @Nullable String actingPerson = revival ? actingPerson() : null;

        if (actingPerson == null) {
            return account;
        }

        final String prefix = account + ON_BEHALF_OF;

        return prefix.length() >= MAX_LENGTH
                ? account.substring(0, Math.min(account.length(), MAX_LENGTH))
                : prefix + actingPerson.substring(0, Math.min(actingPerson.length(), MAX_LENGTH - prefix.length()));
    }

    private static String accountOf(final boolean revival) {
        final String email = getCurrentUserEmailClaim();

        if (hasText(email) || !revival) {
            return email;
        }

        final String username = getCurrentUserPreferredUsernameClaim();

        if (hasText(username)) {
            return username;
        }

        final String client = getCurrentAuthorizedPartyClaim();

        return hasText(client) ? client : Caller.REVIVAL;
    }

    private static @Nullable String actingPerson() {
        final AuditContext.Origin origin = AuditContext.current();

        return origin == null ? null : origin.actingPerson();
    }
}
