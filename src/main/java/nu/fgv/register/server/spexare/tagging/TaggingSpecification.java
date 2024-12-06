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

package nu.fgv.register.server.spexare.tagging;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.Spexare_;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.tag.Tag_;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class TaggingSpecification {

    public static final TaggingSpecification NO_FILTER = new TaggingSpecification();

    private TaggingSpecification() {
    }

    public static Specification<Spexare> hasSpexare(final Spexare spexare) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Spexare_.ID), spexare.getId());
    }

    public static Specification<Spexare> hasTag(final Tag tag) {
        return (root, query, criteriaBuilder) -> {
            final Join<?, Tag> tagJoin = root.join(Spexare_.TAGS);

            return criteriaBuilder.equal(tagJoin.get(Tag_.ID), tag.getId());
        };
    }

    public static Specification<Tag> hasSpexareId(final Long spexareId) {
        return (root, query, criteriaBuilder) -> {
            assert query != null;
            query.distinct(true);

            final Subquery<Spexare> spexareSubQuery = query.subquery(Spexare.class);
            final Root<Spexare> spexare = spexareSubQuery.from(Spexare.class);
            final Expression<Collection<Tag>> taggings = spexare.get(Spexare_.TAGS);

            spexareSubQuery.select(spexare);
            spexareSubQuery.where(criteriaBuilder.equal(spexare.get(Spexare_.ID), spexareId), criteriaBuilder.isMember(root, taggings));

            return criteriaBuilder.exists(spexareSubQuery);
        };
    }

}
