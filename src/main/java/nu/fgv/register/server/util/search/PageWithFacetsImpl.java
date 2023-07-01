package nu.fgv.register.server.util.search;

import org.hibernate.search.engine.search.query.SearchResultTotal;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class PageWithFacetsImpl<T> extends PageImpl<T> implements PageWithFacets<T> {

    private final List<Facet> facets;

    public PageWithFacetsImpl(final List<T> content, final Pageable pageable, final SearchResultTotal total, final List<Facet> facets) {
        super(content, pageable, total.hitCountLowerBound());

        this.facets = facets;
    }

    @Override
    public List<Facet> getFacets() {
        return facets;
    }
}
