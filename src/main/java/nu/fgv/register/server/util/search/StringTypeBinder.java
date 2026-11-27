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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static nu.fgv.register.server.util.Constants.AGGREGATION_COMPOSITE_DELIMITER;

/**
 * Indexes a type and a string value of an entity, such as a membership year, as a hierarchical facet value labelled per
 * language.
 *
 * @param <T> the entity type
 * @author Anders Jacobsson
 * @since 2.0
 */
public class StringTypeBinder<T> extends AbstractHierarchicalTypeBinder {
    private final Class<T> bridgedType;
    private final Function<T, Type> typeExtractor;
    private final Function<T, Collection<String>> valuesExtractor;
    private final String valuePropertyName;
    private final boolean includeGroupLabel;

    public StringTypeBinder(final Class<T> bridgedType, final Function<T, Type> typeExtractor, final Function<T, String> valueExtractor, final String valuePropertyName, final boolean includeGroupLabel) {
        this.bridgedType = bridgedType;
        this.typeExtractor = typeExtractor;
        this.valuesExtractor = entity -> List.of(valueExtractor.apply(entity));
        this.valuePropertyName = valuePropertyName;
        this.includeGroupLabel = includeGroupLabel;
    }

    /**
     * For an entity that has several values, such as a life membership that counts in every year from its first.
     */
    public StringTypeBinder(final Class<T> bridgedType, final Function<T, Type> typeExtractor, final String valuePropertyName,
                            final boolean includeGroupLabel, final Function<T, Collection<String>> valuesExtractor) {
        this.bridgedType = bridgedType;
        this.typeExtractor = typeExtractor;
        this.valuesExtractor = valuesExtractor;
        this.valuePropertyName = valuePropertyName;
        this.includeGroupLabel = includeGroupLabel;
    }

    @Override
    public void bind(final TypeBindingContext context) {
        context.dependencies()
                .use("type")
                .use(valuePropertyName);
        extraDependencies(context);
        context.bridge(bridgedType, new Bridge<>(bindFields(context), typeExtractor, valuesExtractor, includeGroupLabel));
    }

    /**
     * Properties besides the type and the value that the indexed values depend on.
     */
    protected void extraDependencies(final TypeBindingContext context) {
        // None by default
    }

    private record Bridge<T>(
            Map<String, IndexFieldReference<String>> fieldReferences,
            Function<T, Type> typeExtractor,
            Function<T, Collection<String>> valuesExtractor,
            boolean includeGroupLabel
    ) implements TypeBridge<T> {
        @Override
        public void write(final DocumentElement target, final T bridgedElement, final TypeBridgeWriteContext context) {
            final Type typeObj = typeExtractor.apply(bridgedElement);

            for (final String value : valuesExtractor.apply(bridgedElement)) {
                final String id = "%s:%s".formatted(typeObj.getId().toLowerCase(), value);

                typeObj.getLabels().forEach((lang, typeLabel) -> {
                    final IndexFieldReference<String> fieldReference = fieldReferences.get(lang);

                    if (fieldReference != null) {
                        final String displayValue = includeGroupLabel ?
                                typeLabel + AGGREGATION_COMPOSITE_DELIMITER + value :
                                value;

                        target.addValue(fieldReference, id + AGGREGATION_COMPOSITE_DELIMITER + displayValue);
                    }
                });
            }
        }
    }
}
