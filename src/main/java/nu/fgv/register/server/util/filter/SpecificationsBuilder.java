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

package nu.fgv.register.server.util.filter;

import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class SpecificationsBuilder<T> {

    private SpecificationsBuilder() {
    }

    public static <T> SpecificationsBuilder<T> builder() {
        return new SpecificationsBuilder<>();
    }

    public Specification<T> build(final Deque<?> postFixedExpressionStack, final Function<FilterCriteria, Specification<T>> converter) {
        final Deque<Specification<T>> specificationStack = new LinkedList<>();

        Collections.reverse((List<?>) postFixedExpressionStack);

        while (!postFixedExpressionStack.isEmpty()) {
            final Object mayBeOperand = postFixedExpressionStack.pop();

            if (!(mayBeOperand instanceof String)) {
                specificationStack.push(converter.apply((FilterCriteria) mayBeOperand));
            } else {
                final Specification<T> operand1 = specificationStack.pop();
                final Specification<T> operand2 = specificationStack.pop();

                if (mayBeOperand.equals(FilterOperation.AND_OPERATOR)) {
                    specificationStack.push(Specification.where(operand1).and(operand2));
                } else if (mayBeOperand.equals(FilterOperation.OR_OPERATOR)) {
                    specificationStack.push(Specification.where(operand1).or(operand2));
                }
            }
        }

        if (specificationStack.isEmpty()) {
            throw new IllegalStateException("Expected non-empty specification stack");
        }
        return specificationStack.pop();
    }

}
