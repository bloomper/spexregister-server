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

import graphql.schema.DataFetchingEnvironment;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.query.AbstractSortStrategy;

import java.util.List;
import java.util.Objects;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class CustomSortStrategy extends AbstractSortStrategy {
    @Override
    protected List<String> getProperties(final DataFetchingEnvironment environment) {
        return Objects.requireNonNull(environment.getArgument("sort"));
    }

    @Override
    protected Sort.Direction getDirection(final DataFetchingEnvironment environment) {
        return Objects.requireNonNull(Sort.Direction.fromOptionalString(Objects.requireNonNull(environment.getArgument("direction"))).orElse(null));
    }
}
