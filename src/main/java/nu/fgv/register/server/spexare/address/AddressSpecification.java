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

package nu.fgv.register.server.spexare.address;

import lombok.Getter;
import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.settings.Type_;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.util.filter.BaseSpecification;
import nu.fgv.register.server.util.filter.FilterCriteria;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Getter
public class AddressSpecification extends BaseSpecification<Address> {

    public static final AddressSpecification NO_FILTER = new AddressSpecification();

    private AddressSpecification() {
    }

    public AddressSpecification(final FilterCriteria criteria) {
        super(criteria);
    }

    public static Specification<Address> hasId(final Long id) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Address_.id), id);
    }

    public static Specification<Address> hasSpexare(final Spexare spexare) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Address_.spexare), spexare);
    }

    public static Specification<Address> hasType(final Type type) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Address_.type), type);
    }

    public static Specification<Address> hasType(final TypeType type) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.join(Address_.type).get(Type_.type), type);
    }

}
