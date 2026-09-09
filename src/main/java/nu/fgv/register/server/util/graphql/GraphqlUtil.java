/*
 * Copyright 2024 the original author or authors.
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

import graphql.GraphQLContext;
import graphql.execution.DataFetcherResult;
import org.springframework.data.domain.OffsetScrollPosition;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.graphql.data.query.ScrollSubrange;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class GraphqlUtil {

    private static final int DEFAULT_LIMIT = 10;

    private GraphqlUtil() {
    }

    public static <T> CountedWindow<T> emptyWindow() {
        return CountedWindow.of(Window.from(Collections.emptyList(), _ -> ScrollPosition.offset(0), false), 0L);
    }

    public static ScrollRequest extractScrollRequest(final ScrollSubrange subrange) {
        return new ScrollRequest(
                subrange.position(),
                subrange.count().orElse(DEFAULT_LIMIT),
                !subrange.forward() && subrange.position().isEmpty()
        );
    }

    public static <T> DataFetcherResult<T> buildDataFetcherResult(final T data, final Map<Object, Object> localContextMap) {
        return DataFetcherResult.<T>newResult()
                .data(data)
                .localContext(GraphQLContext.newContext()
                        .of(localContextMap)
                        .build()
                )
                .build();
    }

    public record ScrollRequest(Optional<ScrollPosition> position, int limit, boolean lastPage) {

        public ScrollPosition positionFor(final long total) {
            if (!lastPage) {
                return position.orElseGet(ScrollPosition::offset);
            }

            final long start = Math.max(0L, total - limit);

            return start == 0L ? ScrollPosition.offset() : ScrollPosition.offset(start - 1);
        }

        public long offsetFor(final long total) {
            if (!lastPage) {
                return position
                        .map(p -> p.isInitial() ? 0L : ((OffsetScrollPosition) p).getOffset() + 1)
                        .orElse(0L);
            }

            return Math.max(0L, total - limit);
        }
    }
}
