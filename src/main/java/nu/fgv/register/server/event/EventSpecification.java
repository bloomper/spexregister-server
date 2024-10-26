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

package nu.fgv.register.server.event;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public class EventSpecification {

    private EventSpecification() {
    }

    public static Specification<Event> hasSource(final Event.SourceType source) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Event_.source), source);
    }

    public static Specification<Event> hasCreatedAtGreaterThanEqual(final Instant createdAt) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get(Event_.createdAt), createdAt);
    }

}
