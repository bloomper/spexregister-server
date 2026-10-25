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

/**
 * The {@code filter} expressions of list queries, turned into JPA specifications. A filter is a space-separated
 * expression of criteria such as {@code year:2024}, {@code title:*Ali*} or {@code parent!NULL}, combined with
 * {@code AND}, {@code OR} and parentheses. The operators are {@code :} (equals), {@code !} (not equals), {@code >},
 * {@code <} and {@code ~} (SQL {@code LIKE}); an asterisk at the start or end of a value of {@code :} makes it ends
 * with, starts with or contains. {@code NULL}, {@code TRUE} and {@code FALSE} compare as such.
 */
@org.jspecify.annotations.NullMarked
package nu.fgv.register.server.util.filter;