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
 * @author Anders Jacobsson
 * @since 2.0
 */
@RequiredArgsConstructor
@Service
public class PermissionService {

    private final AccessPolicies accessPolicies;

    public <T> boolean hasReadPermission(final T object) {
        return hasPermission(object, Permission.READ) || hasAdministrationPermission(object);
    }

    public <T> T checkReadPermission(final T object) {
        return check(hasReadPermission(object), object);
    }

    public <T> boolean hasWritePermission(final T object) {
        return hasPermission(object, Permission.WRITE) || hasAdministrationPermission(object);
    }

    public <T> T checkWritePermission(final T object) {
        return check(hasWritePermission(object), object);
    }

    public <T> boolean hasDeletePermission(final T object) {
        return hasPermission(object, Permission.DELETE) || hasAdministrationPermission(object);
    }

    public <T> T checkDeletePermission(final T object) {
        return check(hasDeletePermission(object), object);
    }

    public <T> boolean hasAdministrationPermission(final T object) {
        return hasPermission(object, Permission.ADMINISTRATION);
    }

    public <T> T checkAdministrationPermission(final T object) {
        return check(hasAdministrationPermission(object), object);
    }

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
