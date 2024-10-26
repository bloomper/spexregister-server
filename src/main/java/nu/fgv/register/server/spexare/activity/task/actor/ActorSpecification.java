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

package nu.fgv.register.server.spexare.activity.task.actor;

import lombok.Getter;
import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.spexare.activity.task.TaskActivity;
import nu.fgv.register.server.util.filter.BaseSpecification;
import nu.fgv.register.server.util.filter.FilterCriteria;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Getter
public class ActorSpecification extends BaseSpecification<Actor> {

    public ActorSpecification(final FilterCriteria criteria) {
        super(criteria);
    }

    public static Specification<Actor> hasId(final Long id) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Actor_.id), id);
    }

    public static Specification<Actor> hasTaskActivity(final TaskActivity taskActivity) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Actor_.taskActivity), taskActivity);
    }

    public static Specification<Actor> hasVocal(final Type vocal) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Actor_.vocal), vocal);
    }

}
