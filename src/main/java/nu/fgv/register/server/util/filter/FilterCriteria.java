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

import org.jspecify.annotations.Nullable;

/**
 * One criterion of a filter: an attribute, an operation and a value.
 *
 * @param key       the attribute path
 * @param operation the operation
 * @param value     the value to compare with
 * @author Anders Jacobsson
 * @since 2.0
 */
public record FilterCriteria(String key, FilterOperation operation, Object value) {

    /**
     * A criterion as parsed, where asterisks before or after the value of an equality make it a contains, ends with or
     * starts with.
     *
     * @param key       the attribute path
     * @param operation the operator character
     * @param prefix    the asterisks before the value, if any
     * @param value     the value
     * @param suffix    the asterisks after the value, if any
     */
    public FilterCriteria(final String key, final String operation, @Nullable final String prefix, final String value, @Nullable final String suffix) {
        FilterOperation op = FilterOperation.getSimpleOperation(operation.charAt(0));

        if (op == FilterOperation.EQUALITY) {
            final boolean startWithAsterisk = prefix != null && prefix.contains(FilterOperation.WILDCARD);
            final boolean endWithAsterisk = suffix != null && suffix.contains(FilterOperation.WILDCARD);

            if (startWithAsterisk && endWithAsterisk) {
                op = FilterOperation.CONTAINS;
            } else if (startWithAsterisk) {
                op = FilterOperation.ENDS_WITH;
            } else if (endWithAsterisk) {
                op = FilterOperation.STARTS_WITH;
            }
        }
        this(key, op, value);
    }

}
