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

package nu.fgv.register.server.util.filter;

import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareSpecification;
import nu.fgv.register.server.util.error.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class FilterTest {

    @Test
    void should_reject_unbalanced_parentheses() {
        assertThrows(BadRequestException.class, () -> FilterParser.parse("firstName:Ada)"));
    }

    @Test
    void should_reject_an_operator_without_operands() {
        assertThrows(BadRequestException.class, () -> SpecificationsBuilder.<Spexare>builder()
                .build(FilterParser.parse("firstName:Ada AND"), SpexareSpecification::new));
    }

    @Test
    void should_reject_a_path_into_an_association_that_is_not_allowed() {
        final SpexareSpecification specification = new SpexareSpecification(new FilterCriteria("partner.comment", ":", null, "x", null));

        assertThrows(BadRequestException.class, () -> specification.toPredicate(null, null, null));
    }
}
