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

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public record Caller(Set<String> roles, @Nullable String subject) {

    public static final String ADMIN = "ADMIN";
    public static final String EDITOR = "EDITOR";
    public static final String USER = "USER";
    public static final String REVIVAL = "REVIVAL";
    private static final String ROLE_PREFIX = "ROLE_spexregister_";
    private static final Caller NOBODY = new Caller(Set.of(), null);

    public static Caller current() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return NOBODY;
        }

        final Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .collect(Collectors.toUnmodifiableSet());

        return new Caller(roles, subjectOf(authentication));
    }

    private static @Nullable String subjectOf(final Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken token) {
            final Jwt jwt = token.getToken();

            return jwt.getClaimAsString(JwtClaimNames.SUB);
        }

        return null;
    }

    public boolean hasRole(final String role) {
        return roles.contains(role);
    }

    public boolean hasAnyRole(final Set<String> candidates) {
        return candidates.stream().anyMatch(roles::contains);
    }

    public boolean isAdmin() {
        return hasRole(ADMIN);
    }

    public boolean isAdminOrEditor() {
        return hasRole(ADMIN) || hasRole(EDITOR);
    }
}
