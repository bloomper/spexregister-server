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
 * The spex, the productions Chalmersspexet has staged, and their revivals. A revival is a
 * {@link nu.fgv.register.server.spex.Spex} with a parent; it shares the parent's
 * {@link nu.fgv.register.server.spex.SpexDetails}, so title, poster and category are kept once for the original and all
 * its revivals.
 */
@org.jspecify.annotations.NullMarked
package nu.fgv.register.server.spex;