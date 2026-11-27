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

import nu.fgv.register.server.util.security.RequiresAdmin;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * The sync of base data from Spexipedia, for admins.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Controller
public class SyncGraphqlApi {

    private final SyncService service;
    private final MembershipSyncService memberships;

    public SyncGraphqlApi(final SyncService service, final MembershipSyncService memberships) {
        this.service = service;
        this.memberships = memberships;
    }

    @QueryMapping
    @RequiresAdmin
    public List<SyncService.Status> syncSources() {
        return List.of(service.status(), memberships.status());
    }

    @MutationMapping
    @RequiresAdmin
    public SyncService.@Nullable Result syncRun(@Argument final @Nullable String source, @Argument final @Nullable Boolean full) {
        if (MembershipSyncService.SOURCE.equals(source)) {
            return memberships.pull(Boolean.TRUE.equals(full));
        }
        return service.pull(Boolean.TRUE.equals(full));
    }
}
