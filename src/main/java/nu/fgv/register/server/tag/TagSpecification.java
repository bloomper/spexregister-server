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

package nu.fgv.register.server.tag;

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
public class TagSpecification extends BaseSpecification<Tag> {

    public static final TagSpecification NO_FILTER = new TagSpecification();

    private TagSpecification() {
    }

    public TagSpecification(final FilterCriteria criteria) {
        super(criteria);
    }

    public static Specification<Tag> hasIds(final List<Long> ids) {
        return (root, query, criteriaBuilder) -> root.get(Tag_.id).in(ids);
    }

}
