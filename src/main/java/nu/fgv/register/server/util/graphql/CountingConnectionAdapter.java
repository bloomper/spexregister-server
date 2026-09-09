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

package nu.fgv.register.server.util.graphql;

import graphql.relay.DefaultConnection;
import graphql.relay.Edge;
import graphql.relay.PageInfo;
import nu.fgv.register.server.util.search.FacetedConnection;
import nu.fgv.register.server.util.search.WindowWithFacets;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.graphql.data.pagination.CompositeConnectionAdapter;
import org.springframework.graphql.data.pagination.ConnectionAdapter;
import org.springframework.graphql.data.pagination.CursorStrategy;
import org.springframework.graphql.data.query.SliceConnectionAdapter;
import org.springframework.graphql.data.query.WindowConnectionAdapter;

import java.util.List;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class CountingConnectionAdapter extends CompositeConnectionAdapter {

    public CountingConnectionAdapter(final CursorStrategy<ScrollPosition> cursorStrategy) {
        super(List.of(new WindowConnectionAdapter(cursorStrategy), new SliceConnectionAdapter(cursorStrategy)));
    }

    @Override
    public <T> Object createConnection(final Object container, final List<Edge<T>> edges, final PageInfo pageInfo) {
        return switch (container) {
            case final WindowWithFacets<?> window ->
                    new FacetedConnection<>(edges, pageInfo, window.getTotalCount(), window.getFacets());
            case final TotalCountAware window -> new CountedConnection<>(edges, pageInfo, window.getTotalCount());
            default -> new DefaultConnection<>(edges, pageInfo);
        };
    }
}
