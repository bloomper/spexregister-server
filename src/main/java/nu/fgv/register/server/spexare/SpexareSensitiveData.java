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
 * Who sees the social security number, and what callers limited to the revival service may filter on. Administrators
 * and editors see every number and the spexare themself sees their own; for anyone else only the birth date is shown,
 * and the number can neither be set nor filtered on. A caller with only the {@code REVIVAL} role may not filter on
 * memberships or users.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
public final class SpexareSensitiveData {

    public static final String FIELD = Spexare_.SOCIAL_SECURITY_NUMBER;

    public static final Set<String> RESTRICTED_FIELDS = Set.of(Spexare_.MEMBERSHIPS, Spexare_.USER);

    private static final int BIRTH_DATE_LENGTH = 8;

    private SpexareSensitiveData() {
    }

    /**
     * @param socialSecurityNumber a social security number, or null
     * @return its first eight characters, the birth date, or the value unchanged when it is no longer
     */
    public static @Nullable String birthDateOf(@Nullable final String socialSecurityNumber) {
        return socialSecurityNumber != null && socialSecurityNumber.length() > BIRTH_DATE_LENGTH
                ? socialSecurityNumber.substring(0, BIRTH_DATE_LENGTH)
                : socialSecurityNumber;
    }

    /**
     * @param spexare the spexare
     * @return whether the caller may see and set its full social security number
     */
    public static boolean isReadable(final Spexare spexare) {
        return isAdministratorOrEditor() || isOwnRecord(spexare);
    }

    /**
     * @return whether the caller may see and set every spexare's full social security number
     */
    public static boolean isReadableForAll() {
        return isAdministratorOrEditor();
    }

    /**
     * @param path a filter attribute path
     * @return whether it reaches the social security number
     */
    public static boolean isSensitivePath(final String path) {
        return Arrays.asList(path.split("\\.")).contains(FIELD);
    }

    /**
     * @return whether the caller has the {@code REVIVAL} role and no other Spexregister role
     */
    public static boolean isLimitedToRevival() {
        final Caller caller = Caller.current();

        return caller.hasRole(Caller.REVIVAL) && !caller.hasAnyRole(Set.of(Caller.ADMIN, Caller.EDITOR, Caller.USER));
    }

    /**
     * @param path a filter attribute path
     * @return whether it reaches memberships or the user, which a caller limited to the revival service may not filter
     *     on
     */
    public static boolean isRestrictedPath(final String path) {
        return Arrays.stream(path.split("\\.")).anyMatch(RESTRICTED_FIELDS::contains);
    }

    private static boolean isOwnRecord(final Spexare spexare) {
        final String externalId = getCurrentUserSubClaim();

        return hasText(externalId) && spexare.getUser() != null && externalId.equals(spexare.getUser().getExternalId());
    }
}
