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

package nu.fgv.register.server.util.data;

import jakarta.ws.rs.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static nu.fgv.register.server.util.data.NotificationSuspender.NOTIFICATION_LISTENER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class NotificationSuspenderTest {

    private final RealmResource realm = mock(RealmResource.class);

    private static RealmEventsConfigRepresentation config(final String... listeners) {
        final RealmEventsConfigRepresentation config = new RealmEventsConfigRepresentation();

        config.setEventsListeners(List.of(listeners));
        return config;
    }

    @Test
    void turnsTheListenerOffAndBackOn() {
        final ArgumentCaptor<RealmEventsConfigRepresentation> updated = ArgumentCaptor.forClass(RealmEventsConfigRepresentation.class);

        when(realm.getRealmEventsConfig()).thenReturn(config("jboss-logging", NOTIFICATION_LISTENER), config("jboss-logging"));

        try (NotificationSuspender.Suspension ignored = NotificationSuspender.suspend(realm)) {
            verify(realm).updateRealmEventsConfig(updated.capture());
            assertThat(updated.getValue().getEventsListeners()).containsExactly("jboss-logging");
        }

        verify(realm, times(2)).updateRealmEventsConfig(updated.capture());
        assertThat(updated.getValue().getEventsListeners()).containsExactly("jboss-logging", NOTIFICATION_LISTENER);
    }

    @Test
    void leavesTheListenerAloneWhenAlreadyBackOn() {
        when(realm.getRealmEventsConfig()).thenReturn(config(NOTIFICATION_LISTENER), config(NOTIFICATION_LISTENER));

        NotificationSuspender.suspend(realm).close();

        verify(realm, times(1)).updateRealmEventsConfig(any());
    }

    @Test
    void doesNothingWhenTheListenerIsOff() {
        when(realm.getRealmEventsConfig()).thenReturn(config("jboss-logging"));

        NotificationSuspender.suspend(realm).close();

        verify(realm, never()).updateRealmEventsConfig(any());
    }

    @Test
    void goesOnWithoutTheRole() {
        when(realm.getRealmEventsConfig()).thenThrow(new ForbiddenException());

        NotificationSuspender.suspend(realm).close();

        verify(realm, never()).updateRealmEventsConfig(any());
    }
}
