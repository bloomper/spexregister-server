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

import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLSchemaElement;
import graphql.schema.GraphQLTypeVisitorStub;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class TotalCountTypeVisitor extends GraphQLTypeVisitorStub {

    private static final String TOTAL_COUNT = "totalCount";

    private static final DataFetcher<Long> DATA_FETCHER = environment ->
            environment.getSource() instanceof final TotalCountAware container ? container.getTotalCount() : 0L;

    @Override
    public TraversalControl visitGraphQLFieldDefinition(final GraphQLFieldDefinition fieldDefinition,
                                                        final TraverserContext<GraphQLSchemaElement> context) {
        if (TOTAL_COUNT.equals(fieldDefinition.getName())
                && context.getParentNode() instanceof final GraphQLFieldsContainer parent
                && parent.getName().endsWith("Connection")) {
            final GraphQLCodeRegistry.Builder codeRegistry = context.getVarFromParents(GraphQLCodeRegistry.Builder.class);

            if (codeRegistry != null) {
                codeRegistry.dataFetcher(FieldCoordinates.coordinates(parent, fieldDefinition), DATA_FETCHER);
            }
        }

        return TraversalControl.CONTINUE;
    }
}
