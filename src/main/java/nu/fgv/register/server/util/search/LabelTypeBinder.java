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

import nu.fgv.register.server.settings.Type;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;

import java.util.Map;
import java.util.function.Function;

import static nu.fgv.register.server.util.Constants.AGGREGATION_COMPOSITE_DELIMITER;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class LabelTypeBinder<T> extends AbstractHierarchicalTypeBinder {
    private final Class<T> bridgedType;
    private final Function<T, Type> typeExtractor;
    private final String typePropertyName;

    public LabelTypeBinder(final Class<T> bridgedType, final Function<T, Type> typeExtractor, final String typePropertyName) {
        this.bridgedType = bridgedType;
        this.typeExtractor = typeExtractor;
        this.typePropertyName = typePropertyName;
    }

    @Override
    public void bind(final TypeBindingContext context) {
        context.dependencies().use(typePropertyName);
        context.bridge(bridgedType, new Bridge<>(bindFields(context), typeExtractor));
    }

    private record Bridge<T>(
            Map<String, IndexFieldReference<String>> fieldReferences,
            Function<T, Type> typeExtractor
    ) implements TypeBridge<T> {
        @Override
        public void write(final DocumentElement target, final T bridgedElement, final TypeBridgeWriteContext context) {
            final Type typeObj = typeExtractor.apply(bridgedElement);
            final String id = typeObj.getId().toLowerCase();

            typeObj.getLabels().forEach((lang, label) -> {
                final IndexFieldReference<String> fieldReference = fieldReferences.get(lang);
                if (fieldReference != null) {
                    target.addValue(fieldReference, id + AGGREGATION_COMPOSITE_DELIMITER + label);
                }
            });
        }
    }
}
