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

package nu.fgv.register.server.spex;

import lombok.Getter;
import nu.fgv.register.server.util.filter.BaseSpecification;
import nu.fgv.register.server.util.filter.FilterCriteria;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Getter
public class SpexSpecification extends BaseSpecification<Spex> {

    public static final SpexSpecification NO_FILTER = new SpexSpecification();

    private SpexSpecification() {
    }

    public SpexSpecification(final FilterCriteria criteria) {
        super(criteria);
    }

    public static Specification<Spex> isRevival() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNotNull(root.get(Spex_.parent));
    }

    public static Specification<Spex> isNotRevival() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get(Spex_.parent));
    }

    public static Specification<Spex> hasId(final Long id) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Spex_.id), id);
    }

    public static Specification<Spex> hasParent(final Spex parent) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Spex_.parent), parent);
    }

    public static Specification<Spex> hasYear(final String year) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Spex_.year), year);
    }

    public static Specification<Spex> hasIds(final List<Long> ids) {
        return (root, query, criteriaBuilder) -> root.get(Spex_.id).in(ids);
    }

    public static Specification<Spex> hasParentIds(final List<Long> parentIds) {
        return (root, query, criteriaBuilder) -> root.join(Spex_.parent).get(Spex_.id).in(parentIds);
    }
}
