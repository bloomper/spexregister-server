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

/**
 * The change history of the register, kept by Hibernate Envers. Every revision records who made it, from which kind of
 * client, the GraphQL operation or HTTP request and an optional reason. The history of an entity can be read, a
 * revision restored, with or without the records that belong to it, and the history of a removed spexare purged for
 * good.
 */
@org.jspecify.annotations.NullMarked
package nu.fgv.register.server.audit;