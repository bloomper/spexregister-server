package nu.fgv.register.server.util.search;

import jakarta.persistence.EntityManager;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.sort.dsl.CompositeSortComponentsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.security.util.FieldUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.util.StringUtils.hasText;

public abstract class AbstractSearchEnabledJpaRepository<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> implements SearchEnabledJpaRepository<T, ID> {

    private final EntityManager entityManager;

    public AbstractSearchEnabledJpaRepository(final Class<T> domainClass, final EntityManager entityManager) {
        super(domainClass, entityManager);
        this.entityManager = entityManager;
    }

    public AbstractSearchEnabledJpaRepository(final JpaEntityInformation<T, ID> entityInformation, final EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
    }

    @Override
    public SearchResult<T> search(final String query, final Pageable pageable) {
        return getSearchResult(Search.session(entityManager), parseQuery(query), pageable);
    }

    protected SearchQuery parseQuery(final String query) {
        // Query syntax: <free text query>:aggregation1:aggregation1Value:aggregation2:aggregation2Value
        // Query example: colgate:tags.name:detaljen

        if (!hasText(query)) {
            return new SearchQuery(null, Collections.emptyList());
        }

        final String[] parts = query.split(":");

        if (parts.length > 0) {
            final String freeTextQuery = parts[0].strip();
            final List<Aggregation> aggregations = new ArrayList<>();


            for (int i = 2; (i + 1) < parts.length; i = i + 2) {
                final String name = parts[i];
                final String value = parts[i + 1];

                aggregations.add(new Aggregation(URLDecoder.decode(name, StandardCharsets.UTF_8), URLDecoder.decode(value, StandardCharsets.UTF_8)));
            }

            return new SearchQuery(freeTextQuery, aggregations);
        } else {
            return new SearchQuery(query, Collections.emptyList());
        }
    }

    protected CompositeSortComponentsStep<?> determineSort(final Class<T> clazz, final SearchSortFactory f, final Sort sort) {
        final CompositeSortComponentsStep<?> composite = f.composite();
        final AtomicBoolean atLeastOneStepAdded = new AtomicBoolean(false);

        sort.stream()
                .filter(s -> {
                    if ("score".equals(s.getProperty())) {
                        return true;
                    }
                    try {
                        final Field field = FieldUtils.getField(clazz, s.getProperty());

                        if (field.isAnnotationPresent(GenericField.class)) {
                            return field.getAnnotation(GenericField.class).sortable().equals(Sortable.YES);
                        }
                        if (field.isAnnotationPresent(KeywordField.class)) {
                            return field.getAnnotation(KeywordField.class).sortable().equals(Sortable.YES);
                        }
                        return false;
                    } catch (final IllegalStateException e) {
                        return false;
                    }
                })
                .forEachOrdered(s -> {
                    atLeastOneStepAdded.set(true);
                    if ("score".equals(s.getProperty())) {
                        composite.add(f.score().order(s.isAscending() ? SortOrder.ASC : SortOrder.DESC));
                    } else {
                        composite.add(f.field(s.getProperty()).order(s.isAscending() ? SortOrder.ASC : SortOrder.DESC).missing().last());
                    }
                });

        return atLeastOneStepAdded.get() ? composite : f.composite().add(f.score());
    }

}
