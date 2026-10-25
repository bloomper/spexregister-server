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

package nu.fgv.register.server.security.policy;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Checks of the current caller's permissions on a loaded entity, as used by services and GraphQL resolvers.
 * {@link Permission#ADMINISTRATION} satisfies every check.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@RequiredArgsConstructor
@Service
public class PermissionService {

    private final AccessPolicies accessPolicies;

    /**
     * @param object the entity
     * @param <T>    the entity type
     * @return whether the caller may read it
     */
    public <T> boolean hasReadPermission(final T object) {
        return hasPermission(object, Permission.READ) || hasAdministrationPermission(object);
    }

    /**
     * Lets the entity through when the caller may read it, for use in {@code Optional.map}.
     *
     * @param object the entity
     * @param <T>    the entity type
     * @return the entity
     * @throws org.springframework.security.access.AccessDeniedException when the caller may not
     */
    public <T> T checkReadPermission(final T object) {
        return check(hasReadPermission(object), object);
    }

    /**
     * @param object the entity
     * @param <T>    the entity type
     * @return whether the caller may change it
     */
    public <T> boolean hasWritePermission(final T object) {
        return hasPermission(object, Permission.WRITE) || hasAdministrationPermission(object);
    }

    /**
     * Lets the entity through when the caller may change it, for use in {@code Optional.map}.
     *
     * @param object the entity
     * @param <T>    the entity type
     * @return the entity
     * @throws org.springframework.security.access.AccessDeniedException when the caller may not
     */
    public <T> T checkWritePermission(final T object) {
        return check(hasWritePermission(object), object);
    }

    /**
     * @param object the entity
     * @param <T>    the entity type
     * @return whether the caller may delete it
     */
    public <T> boolean hasDeletePermission(final T object) {
        return hasPermission(object, Permission.DELETE) || hasAdministrationPermission(object);
    }

    /**
     * Lets the entity through when the caller may delete it, for use in {@code Optional.map}.
     *
     * @param object the entity
     * @param <T>    the entity type
     * @return the entity
     * @throws org.springframework.security.access.AccessDeniedException when the caller may not
     */
    public <T> T checkDeletePermission(final T object) {
        return check(hasDeletePermission(object), object);
    }

    /**
     * @param object the entity
     * @param <T>    the entity type
     * @return whether the caller may administer it
     */
    public <T> boolean hasAdministrationPermission(final T object) {
        return hasPermission(object, Permission.ADMINISTRATION);
    }

    /**
     * Lets the entity through when the caller may administer it, for use in {@code Optional.map}.
     *
     * @param object the entity
     * @param <T>    the entity type
     * @return the entity
     * @throws org.springframework.security.access.AccessDeniedException when the caller may not
     */
    public <T> T checkAdministrationPermission(final T object) {
        return check(hasAdministrationPermission(object), object);
    }

    /**
     * Whether the entity's policy gives the caller the permission. The other checks also accept
     * {@link Permission#ADMINISTRATION}.
     *
     * @param object     the entity
     * @param permission the permission asked for
     * @param <T>        the entity type
     * @return whether the caller has it
     */
    public <T> boolean hasPermission(final T object, final Permission permission) {
        return accessPolicies.permits(object, permission);
    }

    private static <T> T check(final boolean permitted, final T object) {
        if (!permitted) {
            throw new AccessDeniedException("Access denied");
        }

        return object;
    }
}
