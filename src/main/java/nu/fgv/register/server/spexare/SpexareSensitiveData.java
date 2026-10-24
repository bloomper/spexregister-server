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

package nu.fgv.register.server.spexare;

import nu.fgv.register.server.security.policy.Caller;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Set;

import static nu.fgv.register.server.util.security.SecurityUtil.getCurrentUserSubClaim;
import static nu.fgv.register.server.util.security.SecurityUtil.isAdministratorOrEditor;
import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public final class SpexareSensitiveData {

    public static final String FIELD = Spexare_.SOCIAL_SECURITY_NUMBER;

    public static final Set<String> RESTRICTED_FIELDS = Set.of(Spexare_.MEMBERSHIPS, Spexare_.USER);

    private static final int BIRTH_DATE_LENGTH = 8;

    private SpexareSensitiveData() {
    }

    public static @Nullable String birthDateOf(@Nullable final String socialSecurityNumber) {
        return socialSecurityNumber != null && socialSecurityNumber.length() > BIRTH_DATE_LENGTH
                ? socialSecurityNumber.substring(0, BIRTH_DATE_LENGTH)
                : socialSecurityNumber;
    }

    public static boolean isReadable(final Spexare spexare) {
        return isAdministratorOrEditor() || isOwnRecord(spexare);
    }

    public static boolean isReadableForAll() {
        return isAdministratorOrEditor();
    }

    public static boolean isSensitivePath(final String path) {
        return Arrays.asList(path.split("\\.")).contains(FIELD);
    }

    public static boolean isLimitedToRevival() {
        final Caller caller = Caller.current();

        return caller.hasRole(Caller.REVIVAL) && !caller.hasAnyRole(Set.of(Caller.ADMIN, Caller.EDITOR, Caller.USER));
    }

    public static boolean isRestrictedPath(final String path) {
        return Arrays.stream(path.split("\\.")).anyMatch(RESTRICTED_FIELDS::contains);
    }

    private static boolean isOwnRecord(final Spexare spexare) {
        final String externalId = getCurrentUserSubClaim();

        return hasText(externalId) && spexare.getUser() != null && externalId.equals(spexare.getUser().getExternalId());
    }
}
