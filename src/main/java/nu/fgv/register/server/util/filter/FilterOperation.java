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

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public enum FilterOperation {
    EQUALITY, NEGATION, GREATER_THAN, LESS_THAN, LIKE, STARTS_WITH, ENDS_WITH, CONTAINS;

    static final String[] SIMPLE_OPERATION_SET = { ":", "!", ">", "<", "~" };

    static final String WILDCARD = "*";

    static final String OR_OPERATOR = "OR";

    static final String AND_OPERATOR = "AND";

    static final String LEFT_PARENTHESIS = "(";

    static final String RIGHT_PARENTHESIS = ")";

    public static final String NULL = "NULL";

    public static FilterOperation getSimpleOperation(final char input) {
        return switch (input) {
            case ':' -> EQUALITY;
            case '!' -> NEGATION;
            case '>' -> GREATER_THAN;
            case '<' -> LESS_THAN;
            case '~' -> LIKE;
            default -> throw new IllegalArgumentException("Unexpected operation input, expected one of :, !, >, <, ~, but got: " + input);
        };
    }
}
