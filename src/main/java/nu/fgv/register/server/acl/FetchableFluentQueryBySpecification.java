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

package nu.fgv.register.server.acl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.ScrollDelegate;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.support.PageableUtils;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class FetchableFluentQueryBySpecification<S, R> extends FluentQuerySupport<S, R> implements FluentQuery.FetchableFluentQuery<R> {
    private final Specification<S> spec;
    private final Function<Sort, TypedQuery<S>> finder;
    private final FetchableFluentQueryBySpecification.SpecificationScrollDelegate<S> scroll;
    private final Function<Specification<S>, Long> countOperation;
    private final Function<Specification<S>, Boolean> existsOperation;
    private final EntityManager entityManager;

    FetchableFluentQueryBySpecification(final Specification<S> spec,
                                        final Class<S> entityType,
                                        final Function<Sort, TypedQuery<S>> finder,
                                        final FetchableFluentQueryBySpecification.SpecificationScrollDelegate<S> scrollDelegate,
                                        final Function<Specification<S>, Long> countOperation,
                                        final Function<Specification<S>, Boolean> existsOperation,
                                        final EntityManager entityManager,
                                        final ProjectionFactory projectionFactory) {
        this(spec, entityType, (Class<R>) entityType, Sort.unsorted(), 0, Collections.emptySet(), finder, scrollDelegate, countOperation, existsOperation, entityManager, projectionFactory);
    }

    private FetchableFluentQueryBySpecification(final Specification<S> spec,
                                                final Class<S> entityType,
                                                final Class<R> resultType,
                                                final Sort sort,
                                                final int limit,
                                                final Collection<String> properties,
                                                final Function<Sort, TypedQuery<S>> finder,
                                                final FetchableFluentQueryBySpecification.SpecificationScrollDelegate<S> scrollDelegate,
                                                final Function<Specification<S>, Long> countOperation,
                                                final Function<Specification<S>, Boolean> existsOperation,
                                                final EntityManager entityManager,
                                                final ProjectionFactory projectionFactory) {
        super(resultType, sort, limit, properties, entityType, projectionFactory);
        this.spec = spec;
        this.finder = finder;
        this.scroll = scrollDelegate;
        this.countOperation = countOperation;
        this.existsOperation = existsOperation;
        this.entityManager = entityManager;
    }

    public FluentQuery.FetchableFluentQuery<R> sortBy(final Sort sort) {
        Assert.notNull(sort, "Sort must not be null");
        return new FetchableFluentQueryBySpecification<>(spec, entityType, resultType, sort.and(sort), limit, properties, finder, scroll, countOperation, existsOperation, entityManager, projectionFactory);
    }

    public FluentQuery.FetchableFluentQuery<R> limit(final int limit) {
        Assert.isTrue(limit >= 0, "Limit must not be negative");
        return new FetchableFluentQueryBySpecification<>(spec, entityType, resultType, sort, limit, properties, finder, scroll, countOperation, existsOperation, entityManager, projectionFactory);
    }

    public <NR> FluentQuery.FetchableFluentQuery<NR> as(final Class<NR> resultType) {
        Assert.notNull(resultType, "Projection target type must not be null");
        if (!resultType.isInterface()) {
            throw new UnsupportedOperationException("Class-based DTOs are not yet supported.");
        } else {
            return new FetchableFluentQueryBySpecification<>(spec, entityType, resultType, sort, limit, properties, finder, scroll, countOperation, existsOperation, entityManager, projectionFactory);
        }
    }

    public FluentQuery.FetchableFluentQuery<R> project(final Collection<String> properties) {
        return new FetchableFluentQueryBySpecification<>(spec, entityType, resultType, sort, limit, properties, finder, scroll, countOperation, existsOperation, entityManager, projectionFactory);
    }

    public R oneValue() {
        final List<?> results = createSortedAndProjectedQuery().setMaxResults(2).getResultList();

        if (results.size() > 1) {
            throw new IncorrectResultSizeDataAccessException(1);
        } else {
            return results.isEmpty() ? null : getConversionFunction().apply(results.getFirst());
        }
    }

    public R firstValue() {
        final List<?> results = createSortedAndProjectedQuery().setMaxResults(1).getResultList();

        return results.isEmpty() ? null : getConversionFunction().apply(results.getFirst());
    }

    public List<R> all() {
        return convert(createSortedAndProjectedQuery().getResultList());
    }

    public Window<R> scroll(final ScrollPosition scrollPosition) {
        Assert.notNull(scrollPosition, "ScrollPosition must not be null");
        return scroll.scroll(sort, limit, scrollPosition).map(getConversionFunction());
    }

    public Page<R> page(final Pageable pageable) {
        return (Page<R>) (pageable.isUnpaged() ? new PageImpl(all()) : readPage(pageable));
    }

    public Stream<R> stream() {
        return createSortedAndProjectedQuery().getResultStream().map(getConversionFunction());
    }

    public long count() {
        return countOperation.apply(spec);
    }

    public boolean exists() {
        return existsOperation.apply(spec);
    }

    private TypedQuery<S> createSortedAndProjectedQuery() {
        final TypedQuery<S> query = finder.apply(sort);

        if (!properties.isEmpty()) {
            query.setHint("jakarta.persistence.fetchgraph", EntityGraphFactory.create(entityManager, entityType, properties));
        }

        if (limit != 0) {
            query.setMaxResults(limit);
        }

        return query;
    }

    private Page<R> readPage(final Pageable pageable) {
        final TypedQuery<S> pagedQuery = createSortedAndProjectedQuery();

        if (pageable.isPaged()) {
            pagedQuery.setFirstResult(PageableUtils.getOffsetAsInteger(pageable));
            pagedQuery.setMaxResults(pageable.getPageSize());
        }

        final List<R> paginatedResults = convert(pagedQuery.getResultList());

        return PageableExecutionUtils.getPage(paginatedResults, pageable, () -> countOperation.apply(spec));
    }

    private List<R> convert(final List<S> resultList) {
        final Function<Object, R> conversionFunction = getConversionFunction();
        final List<R> mapped = new ArrayList<>(resultList.size());

        for (final S s : resultList) {
            mapped.add(conversionFunction.apply(s));
        }

        return mapped;
    }

    private Function<Object, R> getConversionFunction() {
        return getConversionFunction(entityType, resultType);
    }

    static class SpecificationScrollDelegate<T> extends ScrollDelegate<T> {
        private final FluentQuerySupport.ScrollQueryFactory scrollFunction;

        SpecificationScrollDelegate(final FluentQuerySupport.ScrollQueryFactory scrollQueryFactory,
                                    final JpaEntityInformation<T, ?> entity) {
            super(entity);
            scrollFunction = scrollQueryFactory;
        }

        public Window<T> scroll(final Sort sort, final int limit, final ScrollPosition scrollPosition) {
            Query query = scrollFunction.createQuery(sort, scrollPosition);
            if (limit > 0) {
                query = query.setMaxResults(limit);
            }

            return scroll(query, sort, scrollPosition);
        }
    }
}
