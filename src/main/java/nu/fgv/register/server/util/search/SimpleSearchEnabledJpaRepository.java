package nu.fgv.register.server.util.search;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import java.io.Serializable;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.springframework.util.StringUtils.hasText;

@Transactional
public abstract class SimpleSearchEnabledJpaRepository<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> implements SearchEnabledJpaRepository<T, ID> {

    private final EntityManager entityManager;

    public SimpleSearchEnabledJpaRepository(final Class<T> domainClass, final EntityManager entityManager) {
        super(domainClass, entityManager);
        this.entityManager = entityManager;
    }

    public SimpleSearchEnabledJpaRepository(final JpaEntityInformation<T, ID> entityInformation, final EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
    }

    @Override
    public SearchResult<T> search(final String query, final Pageable pageable) {
        return getSearchResult(Search.session(entityManager), parseQuery(query), pageable);
    }

    private SearchQuery parseQuery(final String query) {
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

}
