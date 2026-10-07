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

package nu.fgv.register.keycloak;

import org.keycloak.Config;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public record ApprovalPolicy(String clientId, String pendingGroup) {

    private static final String DEFAULT_CLIENT_ID = "spexregister";
    private static final String DEFAULT_PENDING_GROUP = "pending-approval";

    public static ApprovalPolicy from(final Config.Scope scope) {
        return new ApprovalPolicy(
                scope.get("client-id", DEFAULT_CLIENT_ID),
                scope.get("pending-group", DEFAULT_PENDING_GROUP)
        );
    }

    public boolean appliesTo(final ClientModel client) {
        return client != null && clientId.equals(client.getClientId());
    }

    public boolean isApproved(final RealmModel realm, final UserModel user) {
        final ClientModel client = realm.getClientByClientId(clientId);

        return client != null && client.getRolesStream().anyMatch(user::hasRole);
    }

    public boolean markPending(final KeycloakSession session, final RealmModel realm, final UserModel user) {
        final GroupModel group = KeycloakModelUtils.findGroupByPath(session, realm, "/" + pendingGroup);

        if (group == null) {
            return false;
        }

        user.joinGroup(group);

        return true;
    }
}
