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

import jakarta.persistence.EntityManager;
import nu.fgv.register.server.acl.SimpleAclJpaRepository;
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

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public abstract class AbstractSearchEnabledJpaRepository<T, ID extends Serializable> extends SimpleAclJpaRepository<T, ID> implements SearchEnabledJpaRepository<T, ID> {

    private final EntityManager entityManager;

    protected AbstractSearchEnabledJpaRepository(final Class<T> domainClass, final EntityManager entityManager) {
        super(domainClass, entityManager);
        this.entityManager = entityManager;
    }

    protected AbstractSearchEnabledJpaRepository(final JpaEntityInformation<T, ID> entityInformation, final EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
    }

    @Override
    public SearchResult<T> search(final String query, final Pageable pageable) {
        return search(Search.session(entityManager), parseQuery(query), pageable);
    }

    protected SearchQuery parseQuery(final String query) {
        // Query syntax: <free text query>:aggregation1:aggregation1Value:aggregation2:aggregation2Value
        // Query example: colgate:tags.name:detaljen

        if (!hasText(query)) {
            return new SearchQuery("", Collections.emptyList());
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
