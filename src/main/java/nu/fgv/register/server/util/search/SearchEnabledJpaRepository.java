package nu.fgv.register.server.util.search;

import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.List;

@NoRepositoryBean
public interface SearchEnabledJpaRepository<T, ID extends Serializable> extends JpaRepository<T, ID> {

    SearchResult<T> search(String query, Pageable pageable);

    SearchResult<T> getSearchResult(SearchSession searchSession, SearchQuery query, Pageable pageable);

    record SearchQuery(String freeTextQuery, List<Aggregation> aggregations) {
    }

    record Aggregation(String name, String value) {
    }
}
