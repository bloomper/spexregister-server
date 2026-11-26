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

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Medlemskap's membership feed, read with {@code fgv-spexregister-service}'s token, which holds {@code SYNC} on
 * {@code fgv-membership}.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
public class MedlemskapFeedClient {

    static final int PAGE_SIZE = 500;
    private static final String CHANGES = """
            query Changes($after: String, $first: Int) {
                membershipChanges(after: $after, first: $first) {
                    cursor hasMore
                    changes { sequence id kind spexareId type year lifetime }
                }
            }""";

    private final HttpSyncGraphQlClient client;

    public MedlemskapFeedClient(@Qualifier(SyncConfig.REST_CLIENT) final RestClient restClient, final MedlemskapProperties properties) {
        this.client = HttpSyncGraphQlClient.builder(restClient).url(properties.graphqlUrl()).build();
    }

    /**
     * The changes after the cursor (null for the start). Asking also tells Medlemskap how far Spexregister has read.
     *
     * @throws SpexipediaFeedClient.FeedException when Medlemskap cannot be reached or answers with an error
     */
    public MembershipFeedChange.Page changes(final @Nullable String after) {
        final ClientGraphQlResponse response;

        try {
            response = client.document(CHANGES).variable("after", after).variable("first", PAGE_SIZE).executeSync();
        } catch (final RuntimeException e) {
            throw new SpexipediaFeedClient.FeedException("Medlemskap could not be reached: " + e.getMessage(), e);
        }
        if (!response.isValid() || !response.getErrors().isEmpty()) {
            throw new SpexipediaFeedClient.FeedException("Medlemskap answered with errors: " + response.getErrors(), null);
        }

        final MembershipFeedChange.Page page = response.field("membershipChanges").toEntity(MembershipFeedChange.Page.class);

        if (page == null) {
            throw new SpexipediaFeedClient.FeedException("Medlemskap answered without changes", null);
        }
        return page;
    }
}
