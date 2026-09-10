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

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Describes a child entity owned by an aggregate root, so that a cascading restore can
 * reconcile it against a historical revision.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
public record AuditedChildDescriptor(
        AuditedType type,
        Class<?> entityClass,
        String parentProperty,
        Function<Object, Collection<Object>> childrenOf,
        BiConsumer<Object, Object> attachToParent,
        BiConsumer<Object, Object> detachFromParent,
        List<AuditedChildDescriptor> children
) {

    @SuppressWarnings("unchecked")
    public static <P, C> AuditedChildDescriptor of(
            final AuditedType type,
            final Class<C> entityClass,
            final String parentProperty,
            final Function<P, Collection<C>> childrenOf,
            final BiConsumer<C, P> attachToParent,
            final BiConsumer<C, P> detachFromParent,
            final List<AuditedChildDescriptor> children
    ) {
        return new AuditedChildDescriptor(
                type,
                entityClass,
                parentProperty,
                (Function<Object, Collection<Object>>) (Function<?, ?>) childrenOf,
                (BiConsumer<Object, Object>) (BiConsumer<?, ?>) attachToParent,
                (BiConsumer<Object, Object>) (BiConsumer<?, ?>) detachFromParent,
                children
        );
    }
}