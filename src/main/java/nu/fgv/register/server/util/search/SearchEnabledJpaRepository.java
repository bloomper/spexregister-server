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

import nu.fgv.register.server.acl.AclJpaRepository;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.List;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@NoRepositoryBean
public interface SearchEnabledJpaRepository<T, ID extends Serializable> extends AclJpaRepository<T, ID> {

    SearchResult<T> search(String query, Pageable pageable);

    SearchResult<T> search(SearchSession searchSession, SearchQuery query, Pageable pageable);

    record SearchQuery(String freeTextQuery, List<Aggregation> aggregations) {
    }

    record Aggregation(String name, String value) {
    }
}
