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
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spexare.Spexare;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static nu.fgv.register.server.security.policy.PolicyTestSupport.caller;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Grants add rights on spexare and spexes, and never take any away.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
class GrantedPolicyTest {

    private final PermissionGrants grants = mock(PermissionGrants.class);
    private final SpexarePolicy spexarePolicy = new SpexarePolicy(grants);
    private final SpexPolicy spexPolicy = new SpexPolicy(grants);

    @ParameterizedTest
    @EnumSource(Permission.class)
    void should_give_a_user_what_a_grant_on_a_spexare_covers(final Permission permission) {
        final Spexare spexare = unpublishedSpexare();
        when(grants.covers(any(Caller.class), eq(spexare), eq(permission))).thenReturn(true);

        assertThat(spexarePolicy.permits(caller("USER"), spexare, permission)).isTrue();
    }

    @Test
    void should_not_give_a_user_more_than_the_grant_covers() {
        final Spexare spexare = unpublishedSpexare();
        when(grants.covers(any(Caller.class), eq(spexare), eq(Permission.READ))).thenReturn(true);

        assertThat(spexarePolicy.permits(caller("USER"), spexare, Permission.READ)).isTrue();
        assertThat(spexarePolicy.permits(caller("USER"), spexare, Permission.WRITE)).isFalse();
        assertThat(spexarePolicy.permits(caller("USER"), spexare, Permission.DELETE)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(Permission.class)
    void should_give_a_user_what_a_grant_on_a_spex_covers(final Permission permission) {
        final Spex spex = new Spex();
        when(grants.covers(any(Caller.class), eq(spex), eq(permission))).thenReturn(true);

        assertThat(spexPolicy.permits(caller("USER"), spex, permission)).isTrue();
    }

    @Test
    void should_not_ask_for_grants_when_the_rules_already_allow() {
        final Spexare spexare = unpublishedSpexare();

        assertThat(spexarePolicy.permits(caller("EDITOR"), spexare, Permission.WRITE)).isTrue();
        assertThat(spexPolicy.permits(caller("ADMIN"), new Spex(), Permission.DELETE)).isTrue();
        verify(grants, never()).covers(any(Caller.class), any(Spexare.class), any());
        verify(grants, never()).covers(any(Caller.class), any(Spex.class), any());
    }

    @Test
    void should_keep_the_rules_when_no_grant_covers_the_record() {
        final Spexare published = unpublishedSpexare();
        published.setPublished(true);

        assertThat(spexarePolicy.permits(caller("USER"), published, Permission.READ)).isTrue();
        assertThat(spexarePolicy.permits(caller("USER"), unpublishedSpexare(), Permission.READ)).isFalse();
        assertThat(spexPolicy.permits(caller("USER"), new Spex(), Permission.READ)).isTrue();
        assertThat(spexPolicy.permits(caller("USER"), new Spex(), Permission.WRITE)).isFalse();
    }

    private static Spexare unpublishedSpexare() {
        final Spexare spexare = new Spexare();
        spexare.setPublished(false);
        return spexare;
    }
}
