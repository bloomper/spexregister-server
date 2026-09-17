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

package nu.fgv.register.server.util.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class KeycloakJwtRolesConverterTest {

    private final KeycloakJwtRolesConverter converter = new KeycloakJwtRolesConverter();

    @Test
    void should_convert_realm_roles() {
        final List<String> authorities = convert(Map.of("realm_access", Map.of("roles", List.of("offline_access", "uma_authorization"))));

        assertThat(authorities, containsInAnyOrder("ROLE_realm_offline_access", "ROLE_realm_uma_authorization"));
    }

    @Test
    void should_convert_resource_roles() {
        final List<String> authorities = convert(Map.of("resource_access", Map.of("spexregister", Map.of("roles", List.of("ADMIN", "USER")))));

        assertThat(authorities, containsInAnyOrder("ROLE_spexregister_ADMIN", "ROLE_spexregister_USER"));
    }

    @Test
    void should_convert_both_realm_and_resource_roles() {
        final List<String> authorities = convert(Map.of(
                "realm_access", Map.of("roles", List.of("offline_access")),
                "resource_access", Map.of("spexregister", Map.of("roles", List.of("USER")))
        ));

        assertThat(authorities, containsInAnyOrder("ROLE_realm_offline_access", "ROLE_spexregister_USER"));
    }

    @Test
    void should_ignore_resource_without_roles() {
        final List<String> authorities = convert(Map.of("resource_access", Map.of("account", Map.of())));

        assertThat(authorities, is(empty()));
    }

    @Test
    void should_ignore_resource_with_null_claims() {
        final Map<String, Object> resourceAccess = new HashMap<>();
        resourceAccess.put("account", null);

        final List<String> authorities = convert(Map.of("resource_access", resourceAccess));

        assertThat(authorities, is(empty()));
    }

    @Test
    void should_ignore_resource_with_empty_roles() {
        final List<String> authorities = convert(Map.of("resource_access", Map.of("account", Map.of("roles", List.of()))));

        assertThat(authorities, is(empty()));
    }

    @Test
    void should_still_convert_other_resources_when_one_is_malformed() {
        final Map<String, Object> resourceAccess = new HashMap<>();
        resourceAccess.put("account", Map.of());
        resourceAccess.put("broker", null);
        resourceAccess.put("spexregister", Map.of("roles", List.of("USER")));

        final List<String> authorities = convert(Map.of("resource_access", resourceAccess));

        assertThat(authorities, contains("ROLE_spexregister_USER"));
    }

    @Test
    void should_return_no_authorities_when_claims_are_absent() {
        final List<String> authorities = convert(Map.of());

        assertThat(authorities, is(empty()));
    }

    @Test
    void should_return_no_authorities_when_access_claims_are_empty() {
        final List<String> authorities = convert(Map.of("realm_access", Map.of(), "resource_access", Map.of()));

        assertThat(authorities, is(empty()));
    }

    private List<String> convert(final Map<String, Object> claims) {
        final Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user-1");

        claims.forEach(builder::claim);

        return converter.convert(builder.build())
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

}
