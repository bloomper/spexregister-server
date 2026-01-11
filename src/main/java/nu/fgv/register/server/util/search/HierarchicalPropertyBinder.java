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

package nu.fgv.register.server.util.search;

import nu.fgv.register.server.config.SpexregisterConfig;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;

import java.util.HashMap;
import java.util.Map;

import static nu.fgv.register.server.util.Constants.AGGREGATION_COMPOSITE_DELIMITER;
import static nu.fgv.register.server.util.Constants.AGGREGATION_HIERARCHICAL_MARKER;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class HierarchicalPropertyBinder implements PropertyBinder {
    @Override
    public void bind(final PropertyBindingContext context) {
        context.dependencies()
                .useRootOnly();

        try (final BeanHolder<SpexregisterConfig> config = context.beanResolver().resolve(SpexregisterConfig.class, BeanRetrieval.ANY)) {
            final IndexSchemaElement root = context.indexSchemaElement();
            final Map<String, IndexFieldReference<String>> fieldReferences = new HashMap<>();

            for (final String lang : config.get().getLanguages()) {
                fieldReferences.put(lang, root.field(AGGREGATION_HIERARCHICAL_MARKER + lang, f -> f.asString().aggregable(Aggregable.YES)).toReference());
            }
            context.bridge(String.class, new Bridge(fieldReferences));
        }
    }

    private record Bridge(Map<String, IndexFieldReference<String>> fieldReferences) implements PropertyBridge<String> {
        @Override
        public void write(final DocumentElement target, final String bridgedElement, final PropertyBridgeWriteContext context) {
            final String id = bridgedElement.toLowerCase();

            fieldReferences.values().forEach(ref ->
                    target.addValue(ref, id + AGGREGATION_COMPOSITE_DELIMITER + bridgedElement));
        }
    }
}