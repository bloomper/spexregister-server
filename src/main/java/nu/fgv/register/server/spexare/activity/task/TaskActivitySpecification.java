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

package nu.fgv.register.server.spexare.activity.task;

import nu.fgv.register.server.spexare.activity.Activity;
import nu.fgv.register.server.task.Task;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class TaskActivitySpecification {

    private TaskActivitySpecification() {
    }

    public static Specification<TaskActivity> hasId(final Long id) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(TaskActivity_.id), id);
    }

    public static Specification<TaskActivity> hasActivity(final Activity activity) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(TaskActivity_.activity), activity);
    }

    public static Specification<TaskActivity> hasTask(final Task task) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(TaskActivity_.task), task);
    }

}
