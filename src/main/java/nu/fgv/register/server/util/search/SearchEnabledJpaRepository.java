package nu.fgv.register.server.util.search;

import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.sort.dsl.CompositeSortComponentsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.security.util.FieldUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@NoRepositoryBean
public interface SearchEnabledJpaRepository<T, ID extends Serializable> extends JpaRepository<T, ID> {

    SearchResult<T> search(String query, Pageable pageable);

    SearchResult<T> getSearchResult(SearchSession searchSession, SearchQuery query, Pageable pageable);

    default CompositeSortComponentsStep<?> determineSort(final Class<T> clazz, final SearchSortFactory f, final Sort sort) {
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

    record SearchQuery(String freeTextQuery, List<Aggregation> aggregations) {
    }

    record Aggregation(String name, String value) {
    }
}
