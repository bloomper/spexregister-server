/*
 * Copyright 2024 the original author or authors.
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

package nu.fgv.register.server.util.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class KeycloakJwtRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    public static final String PREFIX_REALM_ROLE = "ROLE_realm_";
    public static final String PREFIX_RESOURCE_ROLE = "ROLE_";

    private static final String CLAIM_REALM_ACCESS = "realm_access";
    private static final String CLAIM_RESOURCE_ACCESS = "resource_access";
    private static final String CLAIM_ROLES = "roles";

    @Override
    public Collection<GrantedAuthority> convert(final Jwt jwt) {
        final Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        final Map<String, Collection<String>> realmAccess = jwt.getClaim(CLAIM_REALM_ACCESS);

        if (realmAccess != null && !realmAccess.isEmpty()) {
            final Collection<String> roles = realmAccess.get(CLAIM_ROLES);

            if (roles != null && !roles.isEmpty()) {
                final List<SimpleGrantedAuthority> realmRoles = roles.stream()
                        .map(role -> new SimpleGrantedAuthority(PREFIX_REALM_ROLE + role))
                        .toList();
                grantedAuthorities.addAll(realmRoles);
            }
        }

        final Map<String, Map<String, Collection<String>>> resourceAccess = jwt.getClaim(CLAIM_RESOURCE_ACCESS);

        if (resourceAccess != null && !resourceAccess.isEmpty()) {
            resourceAccess.forEach((resource, resourceClaims) -> resourceClaims.get(CLAIM_ROLES).forEach(
                    role -> grantedAuthorities.add(new SimpleGrantedAuthority(PREFIX_RESOURCE_ROLE + resource + "_" + role))
            ));
        }

        return grantedAuthorities;
    }
}
