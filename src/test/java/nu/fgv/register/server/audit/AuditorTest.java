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

import nu.fgv.register.server.util.security.JwtAuditorAwareImpl;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_EDITOR;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_REVIVAL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class AuditorTest {

    private static final String SERVICE_ACCOUNT = "service-account-fgv-revival-service";

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        AuditContext.clear();
    }

    @Test
    void should_record_who_nyuppsattning_acts_for() {
        authenticate(null, SERVICE_ACCOUNT, ROLE_REVIVAL);
        actingFor("Anna Andersson (via Nyuppsättning)");

        assertThat(Auditor.current(), is(equalTo(SERVICE_ACCOUNT + " on behalf of Anna Andersson (via Nyuppsättning)")));
        assertThat(new JwtAuditorAwareImpl().getCurrentAuditor().orElseThrow(), is(equalTo(Auditor.current())));
    }

    @Test
    void should_record_the_service_account_alone_without_an_acting_person() {
        authenticate(null, SERVICE_ACCOUNT, ROLE_REVIVAL);
        actingFor(null);

        assertThat(Auditor.current(), is(equalTo(SERVICE_ACCOUNT)));
    }

    @Test
    void should_fall_back_to_the_client_id_without_a_user_name() {
        final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("subject").claim("azp", "fgv-revival-service").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(ROLE_REVIVAL))));
        actingFor("Anna Andersson");

        assertThat(Auditor.current(), is(equalTo("fgv-revival-service on behalf of Anna Andersson")));
    }

    @Test
    void should_ignore_the_acting_person_for_everyone_else() {
        authenticate("editor@fgv.nu", "editor", ROLE_EDITOR);
        actingFor("Someone Else");

        assertThat(Auditor.current(), is(equalTo("editor@fgv.nu")));
    }

    @Test
    void should_fit_the_audit_columns() {
        authenticate(null, SERVICE_ACCOUNT, ROLE_REVIVAL);
        actingFor("x".repeat(255));

        assertThat(Auditor.current().length(), is(equalTo(Auditor.MAX_LENGTH)));
        assertThat(Auditor.current(), startsWith(SERVICE_ACCOUNT + Auditor.ON_BEHALF_OF + "xxx"));
    }

    @Test
    void should_record_nobody_without_authentication() {
        actingFor("Anna Andersson");

        assertThat(Auditor.current(), is(equalTo("")));
    }

    private static void actingFor(final @Nullable String actingPerson) {
        AuditContext.set(new AuditContext.Origin(AuditSource.WEB, "POST /api/graphql", null, actingPerson));
    }

    private static void authenticate(final @Nullable String email, final String username, final String role) {
        final Jwt.Builder jwt = Jwt.withTokenValue("token").header("alg", "none").subject("subject").claim("preferred_username", username);

        if (email != null) {
            jwt.claim("email", email);
        }
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt.build(), List.of(new SimpleGrantedAuthority(role))));
    }
}
