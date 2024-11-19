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

package nu.fgv.register.server.util.filter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Getter
public class BaseSpecification<T> implements Specification<T> {

    private final FilterCriteria criteria;

    public BaseSpecification(final FilterCriteria criteria) {
        this.criteria = criteria;
    }

    @Override
    public Predicate toPredicate(final Root<T> root, @Nullable final CriteriaQuery<?> query, final CriteriaBuilder builder) {
        return switch (criteria.getOperation()) {
            case EQUALITY -> {
                if (FilterOperation.NULL.equalsIgnoreCase((String) criteria.getValue())) {
                    yield builder.isNull(root.get(criteria.getKey()));
                } else if (FilterOperation.TRUE.equalsIgnoreCase((String) criteria.getValue())) {
                    yield builder.isTrue(root.get(criteria.getKey()));
                } else if (FilterOperation.FALSE.equalsIgnoreCase((String) criteria.getValue())) {
                    yield builder.isFalse(root.get(criteria.getKey()));
                } else {
                    yield builder.equal(root.get(criteria.getKey()), criteria.getValue());
                }
            }
            case NEGATION -> {
                if (FilterOperation.NULL.equalsIgnoreCase((String) criteria.getValue())) {
                    yield builder.isNotNull(root.get(criteria.getKey()));
                } else if (FilterOperation.TRUE.equalsIgnoreCase((String) criteria.getValue())) {
                    yield builder.isFalse(root.get(criteria.getKey()));
                } else if (FilterOperation.FALSE.equalsIgnoreCase((String) criteria.getValue())) {
                    yield builder.isTrue(root.get(criteria.getKey()));
                } else {
                    yield builder.notEqual(root.get(criteria.getKey()), criteria.getValue());
                }
            }
            case GREATER_THAN -> builder.greaterThan(root.get(criteria.getKey()), criteria.getValue().toString());
            case LESS_THAN -> builder.lessThan(root.get(criteria.getKey()), criteria.getValue().toString());
            case LIKE -> builder.like(root.get(criteria.getKey()), criteria.getValue().toString());
            case STARTS_WITH -> builder.like(root.get(criteria.getKey()), criteria.getValue() + "%");
            case ENDS_WITH -> builder.like(root.get(criteria.getKey()), "%" + criteria.getValue());
            case CONTAINS -> builder.like(root.get(criteria.getKey()), "%" + criteria.getValue() + "%");
        };
    }

}
