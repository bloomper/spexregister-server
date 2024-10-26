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

import lombok.Getter;

@Getter
public class FilterCriteria {

    private String key;
    private FilterOperation operation;
    private Object value;
    private boolean orPredicate;

    public FilterCriteria() {
    }

    public FilterCriteria(final String key, final FilterOperation operation, final Object value) {
        super();
        this.key = key;
        this.operation = operation;
        this.value = value;
    }

    public FilterCriteria(final String orPredicate, final String key, final FilterOperation operation, final Object value) {
        super();
        this.orPredicate = orPredicate != null && orPredicate.equals(FilterOperation.OR_PREDICATE_FLAG);
        this.key = key;
        this.operation = operation;
        this.value = value;
    }

    public FilterCriteria(final String key, final String operation, final String prefix, final String value, final String suffix) {
        FilterOperation op = FilterOperation.getSimpleOperation(operation.charAt(0));

        if (op == FilterOperation.EQUALITY) {
                final boolean startWithAsterisk = prefix != null && prefix.contains(FilterOperation.ZERO_OR_MORE_REGEX);
                final boolean endWithAsterisk = suffix != null && suffix.contains(FilterOperation.ZERO_OR_MORE_REGEX);

                if (startWithAsterisk && endWithAsterisk) {
                    op = FilterOperation.CONTAINS;
                } else if (startWithAsterisk) {
                    op = FilterOperation.ENDS_WITH;
                } else if (endWithAsterisk) {
                    op = FilterOperation.STARTS_WITH;
                }
        }
        this.key = key;
        this.operation = op;
        this.value = value;
    }

}
