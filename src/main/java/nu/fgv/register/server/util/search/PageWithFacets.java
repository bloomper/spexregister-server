package nu.fgv.register.server.util.search;

import org.springframework.data.domain.Page;

import java.util.List;

public interface PageWithFacets<T> extends Page<T> {

    List<Facet> getFacets();
}
