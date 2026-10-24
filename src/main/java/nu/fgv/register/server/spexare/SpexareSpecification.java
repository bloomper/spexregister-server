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

package nu.fgv.register.server.spexare;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.Getter;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.Spex_;
import nu.fgv.register.server.spexare.activity.Activity_;
import nu.fgv.register.server.spexare.activity.spex.SpexActivity;
import nu.fgv.register.server.spexare.activity.spex.SpexActivity_;
import nu.fgv.register.server.util.filter.BaseSpecification;
import nu.fgv.register.server.util.filter.FilterCriteria;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Getter
public class SpexareSpecification extends BaseSpecification<Spexare> {

    public static final SpexareSpecification NO_FILTER = new SpexareSpecification();

    private SpexareSpecification() {
    }

    public SpexareSpecification(final FilterCriteria criteria) {
        super(criteria);
    }

    public static Specification<Spexare> hasIds(final List<Long> ids) {
        return (root, query, criteriaBuilder) -> root.get(Spexare_.id).in(ids);
    }

    public static Specification<Spexare> hasSpexActivityInSpexOrRevivals(final Long spexId) {
        return (root, query, criteriaBuilder) -> {
            final Subquery<Long> participants = query.subquery(Long.class);
            final Root<SpexActivity> spexActivity = participants.from(SpexActivity.class);
            final Join<SpexActivity, Spex> spex = spexActivity.join(SpexActivity_.spex);

            participants
                    .select(spexActivity.get(SpexActivity_.activity).get(Activity_.spexare).get(Spexare_.id))
                    .where(criteriaBuilder.or(
                            criteriaBuilder.equal(spex.get(Spex_.id), spexId),
                            criteriaBuilder.equal(spex.get(Spex_.parent).get(Spex_.id), spexId)));

            return root.get(Spexare_.id).in(participants);
        };
    }

}
