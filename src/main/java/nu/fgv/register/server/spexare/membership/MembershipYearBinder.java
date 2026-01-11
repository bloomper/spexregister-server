package nu.fgv.register.server.spexare.membership;

import nu.fgv.register.server.config.SpexregisterConfig;
import nu.fgv.register.server.util.ApplicationContextHolder;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.springframework.context.MessageSource;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static nu.fgv.register.server.util.Constants.FACET_COMPOSITE_DELIMITER;

public class MembershipYearBinder implements TypeBinder {
    @Override
    public void bind(final TypeBindingContext context) {
        context.dependencies()
                .use("year")
                .use("type");

        try (final BeanHolder<SpexregisterConfig> config = context.beanResolver().resolve(SpexregisterConfig.class, BeanRetrieval.ANY)) {
            final IndexSchemaElement root = context.indexSchemaElement();
            final Map<String, IndexFieldReference<String>> fieldReferences = new HashMap<>();

            for (final String lang : config.get().getLanguages()) {
                fieldReferences.put(lang, root.field("hierarchical_" + lang, f -> f.asString().aggregable(Aggregable.YES)).toReference());
            }
            context.bridge(Membership.class, new Bridge(fieldReferences));
        }
    }

    private record Bridge(Map<String, IndexFieldReference<String>> fieldReferences) implements TypeBridge<Membership> {
        @Override
        public void write(final DocumentElement target, final Membership bridgedElement, final TypeBridgeWriteContext context) {
            final String typeId = bridgedElement.getType().getId();
            final String year = bridgedElement.getYear();
            final String id = "%s:%s".formatted(typeId, year);

            bridgedElement.getType().getLabels().forEach((lang, typeLabel) -> {
                final IndexFieldReference<String> fieldReference = fieldReferences.get(lang);
                if (fieldReference != null) {
                    final String displayValue = "%s: %s".formatted(typeLabel, year);
                    target.addValue(fieldReference, id + FACET_COMPOSITE_DELIMITER + displayValue);
                }
            });
        }
    }
}