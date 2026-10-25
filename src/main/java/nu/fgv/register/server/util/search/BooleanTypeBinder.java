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
import nu.fgv.register.server.util.ApplicationContextHolder;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.springframework.context.MessageSource;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static nu.fgv.register.server.util.Constants.AGGREGATION_COMPOSITE_DELIMITER;

/**
 * Indexes a type and a yes-or-no value of an entity, such as a consent, as a hierarchical facet value labelled per
 * language.
 *
 * @param <T> the entity type
 * @author Anders Jacobsson
 * @since 2.0
 */
public class BooleanTypeBinder<T> extends AbstractHierarchicalTypeBinder {
    private final Class<T> bridgedType;
    private final Function<T, Type> typeExtractor;
    private final Function<T, Boolean> valueExtractor;
    private final boolean includeGroupLabel;

    public BooleanTypeBinder(final Class<T> bridgedType, final Function<T, Type> typeExtractor, final Function<T, Boolean> valueExtractor, final boolean includeGroupLabel) {
        this.bridgedType = bridgedType;
        this.typeExtractor = typeExtractor;
        this.valueExtractor = valueExtractor;
        this.includeGroupLabel = includeGroupLabel;
    }

    @Override
    public void bind(final TypeBindingContext context) {
        context.dependencies()
                .use("value")
                .use("type");
        context.bridge(bridgedType, new Bridge<>(bindFields(context), typeExtractor, valueExtractor, includeGroupLabel));
    }

    private record Bridge<T>(
            Map<String, IndexFieldReference<String>> fieldReferences,
            Function<T, Type> typeExtractor,
            Function<T, Boolean> valueExtractor,
            boolean includeGroupLabel
    ) implements TypeBridge<T> {
        @Override
        public void write(final DocumentElement target, final T bridgedElement, final TypeBridgeWriteContext context) {
            final Type typeObj = typeExtractor.apply(bridgedElement);
            final boolean value = valueExtractor.apply(bridgedElement);

            final String id = "%s:%s".formatted(typeObj.getId().toLowerCase(), value);
            final MessageSource messageSource = ApplicationContextHolder.getBean(MessageSource.class);

            typeObj.getLabels().forEach((lang, typeLabel) -> {
                final IndexFieldReference<String> fieldReference = fieldReferences.get(lang);

                if (fieldReference != null) {
                    final Locale locale = Locale.forLanguageTag(lang);
                    final String booleanLabel = Objects.requireNonNullElse(messageSource.getMessage("boolean.%s".formatted(value), null, String.valueOf(value), locale), String.valueOf(value));

                    final String displayValue = includeGroupLabel ?
                            typeLabel + AGGREGATION_COMPOSITE_DELIMITER + booleanLabel :
                            booleanLabel;

                    target.addValue(fieldReference, id + AGGREGATION_COMPOSITE_DELIMITER + displayValue);
                }
            });
        }
    }
}