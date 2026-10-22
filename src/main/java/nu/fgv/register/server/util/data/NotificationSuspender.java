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
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
final class NotificationSuspender {

    static final String NOTIFICATION_LISTENER = "fgv-admin-notification";

    private NotificationSuspender() {
    }

    static <T> T whileSuspended(final RealmResource realm, final Supplier<T> action) {
        final RealmEventsConfigRepresentation config;

        try {
            config = realm.getRealmEventsConfig();
        } catch (final ForbiddenException e) {
            log.warn("The Keycloak admin client lacks the realm-management role manage-events, so the event listener {} stays on and the sample users are emailed",
                    NOTIFICATION_LISTENER);
            return action.get();
        }

        if (config.getEventsListeners() == null || !config.getEventsListeners().contains(NOTIFICATION_LISTENER)) {
            return action.get();
        }

        config.setEventsListeners(config.getEventsListeners().stream().filter(listener -> !listener.equals(NOTIFICATION_LISTENER)).toList());
        realm.updateRealmEventsConfig(config);
        log.info("Turned off event listener {}", NOTIFICATION_LISTENER);

        try {
            return action.get();
        } finally {
            turnBackOn(realm);
        }
    }

    private static void turnBackOn(final RealmResource realm) {
        try {
            final RealmEventsConfigRepresentation current = realm.getRealmEventsConfig();
            final List<String> listeners = new ArrayList<>(current.getEventsListeners() == null ? List.of() : current.getEventsListeners());

            if (!listeners.contains(NOTIFICATION_LISTENER)) {
                listeners.add(NOTIFICATION_LISTENER);
                current.setEventsListeners(listeners);
                realm.updateRealmEventsConfig(current);
            }
            log.info("Turned event listener {} back on", NOTIFICATION_LISTENER);
        } catch (final RuntimeException e) {
            log.error("Could not turn event listener {} back on. Add it again under Realm settings -> Events -> Event listeners",
                    NOTIFICATION_LISTENER, e);
        }
    }
}
