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
import nu.fgv.register.server.util.error.BadRequestException;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Set;

/**
 * A specification for one {@link FilterCriteria}. Subclasses list the nested attribute paths that may be filtered on in
 * {@link #nestedPaths()}; any other dotted path is refused, so a filter cannot reach into related entities it was not
 * meant to.
 *
 * @param <T> the entity type
 * @author Anders Jacobsson
 * @since 2.0
 */
@Getter
public class BaseSpecification<T> implements Specification<T> {

    @Nullable
    private final FilterCriteria criteria;

    protected BaseSpecification() {
        this.criteria = null;
    }

    protected BaseSpecification(final FilterCriteria criteria) {
        this.criteria = criteria;
    }

    @Override
    @Nullable
    public Predicate toPredicate(final Root<T> root, @Nullable final CriteriaQuery<?> query, final CriteriaBuilder builder) {
        if (criteria == null) {
            return null;
        }
        final Path<T> path = getPath(root, criteria.key());

        return switch (criteria.operation()) {
            case EQUALITY -> {
                if (FilterOperation.NULL.equalsIgnoreCase((String) criteria.value())) {
                    yield builder.isNull(path);
                } else if (FilterOperation.TRUE.equalsIgnoreCase((String) criteria.value())) {
                    yield builder.isTrue(path.as(Boolean.class));
                } else if (FilterOperation.FALSE.equalsIgnoreCase((String) criteria.value())) {
                    yield builder.isFalse(path.as(Boolean.class));
                } else if (path.getJavaType().equals(LocalDate.class)) {
                    yield builder.equal(path.as(LocalDate.class), LocalDate.parse((String) criteria.value()));
                } else {
                    yield builder.equal(path, criteria.value());
                }
            }
            case NEGATION -> {
                if (FilterOperation.NULL.equalsIgnoreCase((String) criteria.value())) {
                    yield builder.isNotNull(path);
                } else if (FilterOperation.TRUE.equalsIgnoreCase((String) criteria.value())) {
                    yield builder.isFalse(path.as(Boolean.class));
                } else if (FilterOperation.FALSE.equalsIgnoreCase((String) criteria.value())) {
                    yield builder.isTrue(path.as(Boolean.class));
                } else if (path.getJavaType().equals(LocalDate.class)) {
                    yield builder.notEqual(path.as(LocalDate.class), LocalDate.parse((String) criteria.value()));
                } else {
                    yield builder.notEqual(path, criteria.value());
                }
            }
            case GREATER_THAN -> {
                if (path.getJavaType().equals(LocalDate.class)) {
                    yield builder.greaterThan(path.as(LocalDate.class), LocalDate.parse((String) criteria.value()));
                } else {
                    yield builder.greaterThan(path.as(String.class), criteria.value().toString());
                }
            }
            case LESS_THAN -> {
                if (path.getJavaType().equals(LocalDate.class)) {
                    yield builder.lessThan(path.as(LocalDate.class), LocalDate.parse((String) criteria.value()));
                } else {
                    yield builder.lessThan(path.as(String.class), criteria.value().toString());
                }
            }
            case LIKE -> builder.like(path.as(String.class), criteria.value().toString());
            case STARTS_WITH -> builder.like(path.as(String.class), criteria.value() + "%");
            case ENDS_WITH -> builder.like(path.as(String.class), "%" + criteria.value());
            case CONTAINS -> builder.like(path.as(String.class), "%" + criteria.value() + "%");
        };
    }

    /**
     * @return the dotted attribute paths, such as {@code details.title}, that may be filtered on
     */
    protected Set<String> nestedPaths() {
        return Set.of();
    }

    private Path<T> getPath(final Root<T> root, final String attributePath) {
        if (attributePath.contains(".") && !nestedPaths().contains(attributePath)) {
            throw new BadRequestException("Filtering on %s is not permitted".formatted(attributePath));
        }

        Path<T> path = root;

        try {
            for (final String part : attributePath.split("\\.")) {
                path = path.get(part);
            }
        } catch (final IllegalArgumentException | IllegalStateException _) {
            throw new BadRequestException("Unknown filter attribute %s".formatted(attributePath));
        }

        return path;
    }
}
