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

package nu.fgv.register.server.sync;

import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.settings.TypeRepository;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.membership.Membership;
import nu.fgv.register.server.spexare.membership.MembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MembershipSyncServiceTest {

    private final MembershipRepository memberships = mock(MembershipRepository.class);
    private final SpexareRepository spexareRepository = mock(SpexareRepository.class);
    private final TypeRepository types = mock(TypeRepository.class);
    private final Spexare spexare = new Spexare();
    private final Type fgv = new Type();
    private final UUID spexareId = spexare.getUuid();

    private MembershipSyncService service;

    @BeforeEach
    void setUp() {
        fgv.setId("FGV");
        service = new MembershipSyncService(mock(MedlemskapFeedClient.class),
                new MedlemskapProperties(true, "http://x/graphql", "http://x", "http://w", List.of("FGV")), mock(JdbcClient.class),
                mock(PlatformTransactionManager.class), memberships, spexareRepository, types);
        when(spexareRepository.findByUuid0(spexareId)).thenReturn(Optional.of(spexare));
        when(types.findById("FGV")).thenReturn(Optional.of(fgv));
        when(memberships.findBySpexareAndTypeAndYear(any(), any(), any())).thenReturn(Optional.empty());
    }

    private static MembershipFeedChange change(final String kind, final UUID spexareId, final int year, final boolean lifetime) {
        return new MembershipFeedChange("1", UUID.randomUUID(), kind, spexareId, "FGV", year, lifetime);
    }

    @Test
    void anUpsertCreatesTheMembership() {
        service.apply(change(MembershipFeedChange.UPSERT, spexareId, 2025, false));

        final ArgumentCaptor<Membership> saved = ArgumentCaptor.forClass(Membership.class);

        verify(memberships).save(saved.capture());
        assertThat(saved.getValue().getYear()).isEqualTo("2025");
        assertThat(saved.getValue().getType()).isSameAs(fgv);
        assertThat(saved.getValue().getSpexare()).isSameAs(spexare);
        assertThat(saved.getValue().isLifetime()).isFalse();
    }

    @Test
    void anUpsertOfAnExistingMembershipUpdatesIt() {
        final Membership existing = new Membership();

        existing.setYear("2019");
        when(memberships.findBySpexareAndTypeAndYear(spexare, fgv, "2019")).thenReturn(Optional.of(existing));

        service.apply(change(MembershipFeedChange.UPSERT, spexareId, 2019, true));

        verify(memberships).save(existing);
        assertThat(existing.isLifetime()).isTrue();
    }

    @Test
    void aDeleteRemovesTheMembership() {
        final Membership existing = new Membership();

        when(memberships.findBySpexareAndTypeAndYear(spexare, fgv, "2025")).thenReturn(Optional.of(existing));

        service.apply(change(MembershipFeedChange.DELETE, spexareId, 2025, false));

        verify(memberships).delete(existing);
    }

    @Test
    void aDeleteOfWhatIsNotThereIsHarmless() {
        service.apply(change(MembershipFeedChange.DELETE, spexareId, 2025, false));

        verify(memberships, never()).delete(any(Membership.class));
    }

    @Test
    void aSpexareThatIsGoneIsSkipped() {
        final UUID gone = UUID.randomUUID();

        when(spexareRepository.findByUuid0(gone)).thenReturn(Optional.empty());

        service.apply(change(MembershipFeedChange.UPSERT, gone, 2025, false));

        verify(memberships, never()).save(any());
    }

    @Test
    void anUnknownTypeFailsSoThatTheChangeIsNotLost() {
        when(types.findById("FGV")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.apply(change(MembershipFeedChange.UPSERT, spexareId, 2025, false)))
                .isInstanceOf(IllegalStateException.class);
    }
}
