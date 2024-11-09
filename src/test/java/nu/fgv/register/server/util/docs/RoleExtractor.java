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

package nu.fgv.register.server.util.docs;

import nu.fgv.register.server.util.security.RequiresAdmin;
import nu.fgv.register.server.util.security.RequiresAdminOrEditor;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class RoleExtractor {

    public static List<String> extractRoles(final Method method) {
        final RequiresAdmin requiresAdmin = method.getAnnotation(RequiresAdmin.class);

        if (requiresAdmin != null) {
            return List.of("ADMIN");
        }

        final RequiresAdminOrEditor requiresAdminOrEditor = method.getAnnotation(RequiresAdminOrEditor.class);

        if (requiresAdminOrEditor != null) {
            return List.of("ADMIN", "EDITOR");
        }

        final RequiresAdminOrEditorOrUser requiresAdminOrEditorOrUser = method.getAnnotation(RequiresAdminOrEditorOrUser.class);

        if (requiresAdminOrEditorOrUser != null) {
            return List.of("ADMIN", "EDITOR", "USER");
        }

        return Collections.emptyList();
    }

}
