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

    record SearchQuery(String freeTextQuery, List<Aggregation> aggregations) {
    }

    record Aggregation(String name, String value) {
    }
}
