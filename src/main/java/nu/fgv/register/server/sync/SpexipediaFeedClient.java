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
 * Spexipedia's change feed, read with {@code spexregister-service}'s token, which holds {@code SYNC} on
 * {@code fgv-spexipedia}.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
public class SpexipediaFeedClient {

    static final int PAGE_SIZE = 500;
    private static final String CHANGES = """
            query Changes($after: String, $first: Int) {
                baseDataChanges(after: $after, first: $first) {
                    cursor hasMore
                    changes {
                        sequence kind id change mergedIntoId
                        spexCategory { id name firstYear logoUrl retired }
                        spex { id year title categoryId parentId posterUrl retired }
                        taskCategory { id name actorPresent board retired }
                        task { id name categoryId retired }
                        character { id spexId name position retired }
                    }
                }
            }""";

    private final HttpSyncGraphQlClient client;

    public SpexipediaFeedClient(@Qualifier(SyncConfig.REST_CLIENT) final RestClient restClient, final SyncProperties properties) {
        this.client = HttpSyncGraphQlClient.builder(restClient).url(properties.graphqlUrl()).build();
    }

    /**
     * The changes after the cursor (null for the start).
     *
     * @throws FeedException when Spexipedia cannot be reached or answers with an error
     */
    public FeedChange.Page changes(final @Nullable String after) {
        final ClientGraphQlResponse response;

        try {
            response = client.document(CHANGES).variable("after", after).variable("first", PAGE_SIZE).executeSync();
        } catch (final RuntimeException e) {
            throw new FeedException("Spexipedia could not be reached: " + e.getMessage(), e);
        }
        if (!response.isValid() || !response.getErrors().isEmpty()) {
            throw new FeedException("Spexipedia answered with errors: " + response.getErrors(), null);
        }

        final FeedChange.Page page = response.field("baseDataChanges").toEntity(FeedChange.Page.class);

        if (page == null) {
            throw new FeedException("Spexipedia answered without changes", null);
        }
        return page;
    }

    public static class FeedException extends RuntimeException {
        public FeedException(final String message, final @Nullable Throwable cause) {
            super(message, cause);
        }
    }
}
