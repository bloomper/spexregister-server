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

package nu.fgv.register.server.spexare.toggle;

import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.settings.Type_;
import nu.fgv.register.server.spexare.Spexare;
import org.springframework.data.jpa.domain.Specification;

public class ToggleSpecification {

    private ToggleSpecification() {
    }

    public static Specification<Toggle> hasId(final Long id) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Toggle_.id), id);
    }

    public static Specification<Toggle> hasSpexare(final Spexare spexare) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Toggle_.spexare), spexare);
    }

    public static Specification<Toggle> hasType(final Type type) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Toggle_.type), type);
    }

    public static Specification<Toggle> hasType(final TypeType type) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.join(Toggle_.type).get(Type_.type), type);
    }

}
