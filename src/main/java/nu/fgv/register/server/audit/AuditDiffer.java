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

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.NotAudited;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Computes the differences between two Envers snapshots of the same entity type, driven by the
 * JPA metamodel so that no per-entity code is required.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
@RequiredArgsConstructor
public class AuditDiffer {

    private final EntityManagerFactory entityManagerFactory;
    private final AuditRegistry registry;

    private final Map<Class<?>, List<SingularAttribute<?, ?>>> auditedAttributes = new ConcurrentHashMap<>();

    public List<FieldChangeDto> diff(final Class<?> entityClass, final @Nullable Object before, final @Nullable Object after) {
        if (before == null && after == null) {
            return List.of();
        }

        final PersistenceUnitUtil util = entityManagerFactory.getPersistenceUnitUtil();
        final List<FieldChangeDto> changes = new ArrayList<>();

        for (final SingularAttribute<?, ?> attribute : attributesOf(entityClass)) {
            final Object oldValue = read(attribute, before);
            final Object newValue = read(attribute, after);

            if (isBinary(attribute)) {
                if (!java.util.Arrays.equals(asBytes(oldValue), asBytes(newValue))) {
                    changes.add(FieldChangeDto.builder()
                            .field(attribute.getName())
                            .oldValue(oldValue == null ? null : contentTypeOf(entityClass, attribute, before))
                            .newValue(newValue == null ? null : contentTypeOf(entityClass, attribute, after))
                            .binary(true)
                            .build());
                }
                continue;
            }

            if (!Objects.equals(identityOf(attribute, oldValue, util), identityOf(attribute, newValue, util))) {
                changes.add(FieldChangeDto.builder()
                        .field(attribute.getName())
                        .oldValue(display(attribute, oldValue, util))
                        .newValue(display(attribute, newValue, util))
                        .binary(false)
                        .build());
            }
        }

        changes.sort(Comparator.comparing(FieldChangeDto::field));

        return List.copyOf(changes);
    }

    public List<SingularAttribute<?, ?>> attributesOf(final Class<?> entityClass) {
        return auditedAttributes.computeIfAbsent(entityClass, this::resolveAttributes);
    }

    private List<SingularAttribute<?, ?>> resolveAttributes(final Class<?> entityClass) {
        final EntityType<?> entityType = entityManagerFactory.getMetamodel().entity(entityClass);

        return entityType.getSingularAttributes().stream()
                .filter(a -> !a.isId())
                .filter(a -> !a.isVersion())
                .filter(a -> !isNotAudited(entityClass, a))
                .sorted(Comparator.comparing(Attribute::getName))
                .<SingularAttribute<?, ?>>map(a -> a)
                .toList();
    }

    private static boolean isNotAudited(final Class<?> entityClass, final Attribute<?, ?> attribute) {
        final Field field = ReflectionUtils.findField(entityClass, attribute.getName());

        return field == null || field.isAnnotationPresent(NotAudited.class);
    }

    public boolean isBinaryProperty(final Class<?> entityClass, final String name) {
        return attributesOf(entityClass).stream()
                .filter(a -> a.getName().equals(name))
                .anyMatch(AuditDiffer::isBinary);
    }

    private @Nullable String contentTypeOf(final Class<?> entityClass,
                                           final SingularAttribute<?, ?> attribute,
                                           final @Nullable Object entity) {
        final Object contentType = readProperty(entityClass, attribute.getName() + "ContentType", entity);

        return contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : String.valueOf(contentType);
    }

    private static boolean isBinary(final SingularAttribute<?, ?> attribute) {
        return byte[].class.equals(attribute.getJavaType());
    }

    private static byte @Nullable [] asBytes(final @Nullable Object value) {
        return value instanceof final byte[] bytes ? bytes : null;
    }

    public @Nullable Object readProperty(final Class<?> entityClass, final String name, final @Nullable Object entity) {
        return attributesOf(entityClass).stream()
                .filter(a -> a.getName().equals(name))
                .findFirst()
                .map(a -> read(a, entity))
                .orElse(null);
    }

    public @Nullable Object read(final SingularAttribute<?, ?> attribute, final @Nullable Object entity) {
        if (entity == null) {
            return null;
        }

        final Field field = ReflectionUtils.findField(entity.getClass(), attribute.getName());

        if (field == null) {
            return null;
        }

        ReflectionUtils.makeAccessible(field);

        return ReflectionUtils.getField(field, entity);
    }

    private static @Nullable String identityOf(final SingularAttribute<?, ?> attribute, final @Nullable Object value, final PersistenceUnitUtil util) {
        if (value == null) {
            return null;
        }

        if (attribute.isAssociation()) {
            final Object identifier = util.getIdentifier(value);

            return identifier == null ? null : String.valueOf(identifier);
        }

        return String.valueOf(value);
    }

    private @Nullable String display(final SingularAttribute<?, ?> attribute, final @Nullable Object value, final PersistenceUnitUtil util) {
        if (value == null) {
            return null;
        }

        if (attribute.isAssociation()) {
            final String label = registry.labelFor(value);

            return StringUtils.hasText(label) ? label : identityOf(attribute, value, util);
        }

        return String.valueOf(value);
    }
}