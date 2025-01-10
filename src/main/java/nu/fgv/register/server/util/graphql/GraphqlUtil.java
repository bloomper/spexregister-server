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
import nu.fgv.register.server.spex.SpexDto;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.graphql.data.query.ScrollSubrange;

import java.util.Collections;
import java.util.Map;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class GraphqlUtil {

    private GraphqlUtil() {
    }

    public static <T> Window<T> emptyWindow() {
        return Window.from(Collections.emptyList(), index -> ScrollPosition.offset(0), false);
    }

    public static Window<SpexDto> emptyWindow(final ScrollPosition scrollPosition) {
        return Window.from(Collections.emptyList(), index -> scrollPosition, false);
    }

    public static <T extends Enum<?>> ScrollPositionAndLimitHolder extractScrollPositionAndLimitAndOrder(final ScrollSubrange subrange) {
        final ScrollPosition scrollPosition = subrange.position().orElse(ScrollPosition.offset());
        final int limit = subrange.count().orElse(10);

        return new ScrollPositionAndLimitHolder(scrollPosition, limit);
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

    public record ScrollPositionAndLimitHolder(ScrollPosition scrollPosition, int limit) {
    }
}
