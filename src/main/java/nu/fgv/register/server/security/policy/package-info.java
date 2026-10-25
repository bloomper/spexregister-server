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
 * Who may do what with which entity. Every protected entity type has one
 * {@link nu.fgv.register.server.security.policy.AccessPolicy} that answers both as a JPA {@code Specification}, so
 * lists only ever contain what the caller may see, and for a single loaded entity, so service methods and
 * {@code @PreAuthorize} expressions agree with the lists. Roles come from Keycloak;
 * {@link nu.fgv.register.server.permission.PermissionGrant}s add rights on single spexare or spex on top of them.
 */
@org.jspecify.annotations.NullMarked
package nu.fgv.register.server.security.policy;
