/*
 * Copyright 2024 the original author or authors.
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

import org.hibernate.search.engine.search.query.SearchResultTotal;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class PageWithFacetsImpl<T> extends PageImpl<T> implements PageWithFacets<T> {

    private final List<Facet> facets;

    public PageWithFacetsImpl(final List<T> content, final Pageable pageable, final SearchResultTotal total, final List<Facet> facets) {
        super(content, pageable, total.hitCount());

        this.facets = facets;
    }

    @Override
    public List<Facet> getFacets() {
        return facets;
    }
}
