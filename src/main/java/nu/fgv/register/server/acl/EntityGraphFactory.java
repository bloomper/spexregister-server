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

package nu.fgv.register.server.acl;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Subgraph;
import org.springframework.data.core.PropertyPath;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
abstract class EntityGraphFactory {
    public static final String HINT = "jakarta.persistence.fetchgraph";

    EntityGraphFactory() {
    }

    public static <T> EntityGraph<T> create(final EntityManager entityManager,
                                            final Class<T> domainType,
                                            final Set<String> properties) {
        final EntityGraph<T> entityGraph = entityManager.createEntityGraph(domainType);
        final Map<String, Subgraph<Object>> existingSubgraphs = new HashMap<>();

        for (final String property : properties) {
            Subgraph<Object> current = null;
            String currentFullPath = "";

            for (final PropertyPath path : PropertyPath.from(property, domainType)) {
                currentFullPath = currentFullPath + path.getSegment() + ".";
                if (path.hasNext()) {
                    final Subgraph<Object> finalCurrent = current;

                    current = current == null ? existingSubgraphs.computeIfAbsent(currentFullPath, k -> entityGraph.addSubgraph(path.getSegment())) : existingSubgraphs.computeIfAbsent(currentFullPath, k -> finalCurrent.addSubgraph(path.getSegment()));
                } else if (current == null) {
                    entityGraph.addAttributeNodes(path.getSegment());
                } else {
                    current.addAttributeNodes(path.getSegment());
                }
            }
        }

        return entityGraph;
    }
}
