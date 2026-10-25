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

package nu.fgv.register.server.audit;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Binds an {@link AuditedType} to the entity class, lookup and authorization plumbing needed to
 * read its Envers history and restore it.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
public record AuditedEntityDescriptor(
        AuditedType type,
        Class<?> entityClass,
        Function<String, Object> idParser,
        Function<Object, Optional<Object>> findCurrent,
        @Nullable Function<Object, @Nullable Object> accessRoot,
        List<AuditedChildDescriptor> children,
        @Nullable Function<Object, @Nullable String> label,
        List<AuditedReferenceDescriptor> references
) {

    @SuppressWarnings("unchecked")
    public static <E, ID> AuditedEntityDescriptor of(
            final AuditedType type,
            final Class<E> entityClass,
            final Function<String, ID> idParser,
            final Function<ID, Optional<E>> findCurrent,
            final @Nullable Function<E, @Nullable Object> accessRoot,
            final List<AuditedChildDescriptor> children
    ) {
        return new AuditedEntityDescriptor(
                type,
                entityClass,
                (Function<String, Object>) idParser,
                (Function<Object, Optional<Object>>) (Function<?, ?>) findCurrent,
                (Function<Object, @Nullable Object>) accessRoot,
                children,
                null,
                List.of()
        );
    }

    /**
     * @param references entities the root points at whose history belongs in its timeline
     * @return a copy with the references
     */
    public AuditedEntityDescriptor withReferences(final AuditedReferenceDescriptor... references) {
        return new AuditedEntityDescriptor(type, entityClass, idParser, findCurrent, accessRoot, children, label, List.of(references));
    }

    /**
     * @param label how to name an entity of the type in the history
     * @param <E>   the entity type
     * @return a copy with the label
     */
    @SuppressWarnings("unchecked")
    public <E> AuditedEntityDescriptor withLabel(final Function<E, @Nullable String> label) {
        return new AuditedEntityDescriptor(
                type,
                entityClass,
                idParser,
                findCurrent,
                accessRoot,
                children,
                (Function<Object, @Nullable String>) label,
                references
        );
    }

    /**
     * @param id the id as given by a client
     * @return the id as the entity's id type
     * @throws NumberFormatException when it is not one
     */
    public Object parseId(final String id) {
        return idParser.apply(id);
    }
}