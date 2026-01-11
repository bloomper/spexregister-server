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

package nu.fgv.register.server.spexare.consent;

import nu.fgv.register.server.config.SpexregisterConfig;
import nu.fgv.register.server.util.ApplicationContextHolder;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.springframework.context.MessageSource;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static nu.fgv.register.server.util.Constants.FACET_COMPOSITE_DELIMITER;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class ConsentValueBinder implements TypeBinder {
    @Override
    public void bind(final TypeBindingContext context) {
        context.dependencies()
                .use("value")
                .use("type");

        try (final BeanHolder<SpexregisterConfig> config = context.beanResolver().resolve(SpexregisterConfig.class, BeanRetrieval.ANY)) {
            final IndexSchemaElement root = context.indexSchemaElement();
            final Map<String, IndexFieldReference<String>> fieldReferences = new HashMap<>();

            for (final String lang : config.get().getLanguages()) {
                fieldReferences.put(lang, root.field("hierarchical_" + lang,
                                f -> f.asString()
                                        .aggregable(Aggregable.YES))
                        .toReference()
                );
            }
            context.bridge(Consent.class, new Bridge(fieldReferences));
        }
    }

    private record Bridge(Map<String, IndexFieldReference<String>> fieldReferences) implements TypeBridge<Consent> {

        @Override
        public void write(final DocumentElement target, final Consent bridgedElement, final TypeBridgeWriteContext context) {
            final String typeId = bridgedElement.getType().getId();
            final boolean value = bridgedElement.getValue();
            final String id = "%s:%s".formatted(typeId, value);
            final MessageSource messageSource = ApplicationContextHolder.getBean(MessageSource.class);

            bridgedElement.getType().getLabels().forEach((lang, typeLabel) -> {
                final IndexFieldReference<String> fieldReference = fieldReferences.get(lang);

                if (fieldReference != null) {
                    final Locale locale = Locale.forLanguageTag(lang);
                    final String booleanLabel = messageSource.getMessage("boolean.%s".formatted(value), null, String.valueOf(value), locale);
                    final String displayValue = "%s: %s".formatted(typeLabel, booleanLabel);

                    target.addValue(fieldReference, id + FACET_COMPOSITE_DELIMITER + displayValue);
                }
            });
        }
    }
}
