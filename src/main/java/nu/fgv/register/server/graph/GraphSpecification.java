/*
 * Copyright 2026 the original author or authors.
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

package nu.fgv.register.server.graph;

import jakarta.persistence.criteria.Join;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.SpexDetails_;
import nu.fgv.register.server.spex.Spex_;
import nu.fgv.register.server.spex.category.SpexCategory_;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.Spexare_;
import nu.fgv.register.server.spexare.activity.Activity_;
import nu.fgv.register.server.spexare.activity.spex.SpexActivity_;
import nu.fgv.register.server.spexare.activity.task.TaskActivity_;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.tag.Tag_;
import nu.fgv.register.server.task.Task;
import nu.fgv.register.server.task.Task_;
import nu.fgv.register.server.task.category.TaskCategory_;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class GraphSpecification {

    private GraphSpecification() {
    }

    public static Specification<Spexare> spexareInSpex(final Long spexId) {
        return (root, query, criteriaBuilder) -> {
            assert query != null;
            query.distinct(true);

            final Join<?, ?> activity = root.join(Spexare_.ACTIVITIES);
            final Join<?, ?> spexActivity = activity.join(Activity_.SPEX_ACTIVITY);

            return criteriaBuilder.equal(spexActivity.get(SpexActivity_.SPEX).get(Spex_.ID), spexId);
        };
    }

    public static Specification<Spexare> spexareWithTask(final Long taskId) {
        return (root, query, criteriaBuilder) -> {
            assert query != null;
            query.distinct(true);

            final Join<?, ?> activity = root.join(Spexare_.ACTIVITIES);
            final Join<?, ?> taskActivity = activity.join(Activity_.TASK_ACTIVITIES);

            return criteriaBuilder.equal(taskActivity.get(TaskActivity_.TASK).get(Task_.ID), taskId);
        };
    }

    public static Specification<Spexare> spexareWithId(final Long spexareId) {
        return (root, _, criteriaBuilder) -> criteriaBuilder.equal(root.get(Spexare_.ID), spexareId);
    }

    public static Specification<Spexare> partnerOf(final Long spexareId) {
        return (root, _, criteriaBuilder) -> criteriaBuilder.equal(root.get(Spexare_.PARTNER).get(Spexare_.ID), spexareId);
    }

    public static Specification<Spexare> spexareWithTag(final Long tagId) {
        return (root, query, criteriaBuilder) -> {
            assert query != null;
            query.distinct(true);

            final Join<?, Tag> tag = root.join(Spexare_.TAGS);

            return criteriaBuilder.equal(tag.get(Tag_.ID), tagId);
        };
    }

    public static Specification<Spex> spexInCategory(final Long categoryId) {
        return (root, _, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get(Spex_.DETAILS).get(SpexDetails_.CATEGORY).get(SpexCategory_.ID), categoryId),
                criteriaBuilder.isNull(root.get(Spex_.PARENT))
        );
    }

    public static Specification<Task> taskInCategory(final Long categoryId) {
        return (root, _, criteriaBuilder) -> criteriaBuilder.equal(root.get(Task_.CATEGORY).get(TaskCategory_.ID), categoryId);
    }

    public static Specification<Spexare> spexareMatching(final String term) {
        return (root, _, criteriaBuilder) -> {
            final String pattern = pattern(term);

            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get(Spexare_.FIRST_NAME)), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get(Spexare_.LAST_NAME)), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get(Spexare_.NICK_NAME)), pattern)
            );
        };
    }

    public static Specification<Spex> spexMatching(final String term) {
        return (root, _, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.like(criteriaBuilder.lower(root.get(Spex_.DETAILS).get(SpexDetails_.TITLE)), pattern(term)),
                criteriaBuilder.isNull(root.get(Spex_.PARENT))
        );
    }

    public static Specification<Task> taskMatching(final String term) {
        return (root, _, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.lower(root.get(Task_.NAME)), pattern(term));
    }

    public static Specification<Tag> tagMatching(final String term) {
        return (root, _, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.lower(root.get(Tag_.NAME)), pattern(term));
    }

    private static String pattern(final String term) {
        return "%%%s%%".formatted(term.toLowerCase().replace("%", "\\%").replace("_", "\\_"));
    }

}
