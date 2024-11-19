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
import jakarta.persistence.criteria.Path;
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
        final Path<T> path = getPath(root, criteria.getKey());

        return switch (criteria.getOperation()) {
            case EQUALITY -> {
                if (FilterOperation.NULL.equalsIgnoreCase((String) criteria.getValue())) {
                    yield builder.isNull(path);
                } else if (FilterOperation.TRUE.equalsIgnoreCase((String) criteria.getValue())) {
                    yield builder.isTrue(path.as(Boolean.class));
                } else if (FilterOperation.FALSE.equalsIgnoreCase((String) criteria.getValue())) {
                    yield builder.isFalse(path.as(Boolean.class));
                } else {
                    yield builder.equal(path, criteria.getValue());
                }
            }
            case NEGATION -> {
                if (FilterOperation.NULL.equalsIgnoreCase((String) criteria.getValue())) {
                    yield builder.isNotNull(path);
                } else if (FilterOperation.TRUE.equalsIgnoreCase((String) criteria.getValue())) {
                    yield builder.isFalse(path.as(Boolean.class));
                } else if (FilterOperation.FALSE.equalsIgnoreCase((String) criteria.getValue())) {
                    yield builder.isTrue(path.as(Boolean.class));
                } else {
                    yield builder.notEqual(path, criteria.getValue());
                }
            }
            case GREATER_THAN -> builder.greaterThan(path.as(String.class), criteria.getValue().toString());
            case LESS_THAN -> builder.lessThan(path.as(String.class), criteria.getValue().toString());
            case LIKE -> builder.like(path.as(String.class), criteria.getValue().toString());
            case STARTS_WITH -> builder.like(path.as(String.class), criteria.getValue() + "%");
            case ENDS_WITH -> builder.like(path.as(String.class), "%" + criteria.getValue());
            case CONTAINS -> builder.like(path.as(String.class), "%" + criteria.getValue() + "%");
        };
    }

    private Path<T> getPath(final Root<T> root, final String attributePath) {
        Path<T> path = root;

        for (final String part : attributePath.split("\\.")) {
            path = path.get(part);
        }

        return path;
    }
}
