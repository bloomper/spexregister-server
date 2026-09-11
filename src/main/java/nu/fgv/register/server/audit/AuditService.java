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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.util.error.BadRequestException;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.graphql.GraphqlUtil;
import nu.fgv.register.server.util.security.RequiresAdmin;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import nu.fgv.register.server.util.security.SecurityUtil;
import org.hibernate.Hibernate;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.OffsetScrollPosition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Window;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reads the Envers history of any audited entity and restores an entity, optionally together with
 * the children of its aggregate, to a previous revision.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuditService {

    private static final int DEFAULT_SINCE_IN_DAYS = 90;

    private final AuditRegistry registry;
    private final AuditDiffer differ;
    private final EnversAuditSupport envers;
    private final PermissionService permissionService;
    private final EntityManager entityManager;
    private final EntityManagerFactory entityManagerFactory;

    @RequiresAdminOrEditorOrUser
    public List<RevisionDto> findRevisions(final AuditedType type, final String rawId) {
        final AuditedEntityDescriptor descriptor = registry.require(type);
        final Object id = parseId(descriptor, rawId);

        authorizeRead(descriptor, id);

        final List<RevisionDto> revisions = new ArrayList<>(toRevisionDtos(descriptor, id, triplesFor(descriptor, id)));

        descriptor.references().forEach(reference -> revisions.addAll(referenceRevisions(descriptor, id, reference)));

        return mergeByRevision(descriptor, revisions);
    }

    private List<RevisionDto> mergeByRevision(final AuditedEntityDescriptor descriptor, final List<RevisionDto> revisions) {
        final Map<Long, List<RevisionDto>> byRevision = new LinkedHashMap<>();

        revisions.forEach(revision -> byRevision.computeIfAbsent(revision.revision(), _ -> new ArrayList<>()).add(revision));

        return byRevision.values().stream()
                .map(group -> group.size() == 1 ? group.getFirst() : merge(descriptor, group))
                .sorted(Comparator.comparing(RevisionDto::revision).reversed())
                .toList();
    }

    private RevisionDto merge(final AuditedEntityDescriptor descriptor, final List<RevisionDto> group) {
        final RevisionDto anchor = group.stream()
                .filter(revision -> revision.type() == descriptor.type())
                .findFirst()
                .orElseGet(group::getFirst);

        final List<FieldChangeDto> changes = group.stream()
                .flatMap(revision -> revision.changes().stream())
                .sorted(Comparator.comparing(FieldChangeDto::field))
                .toList();

        return RevisionDto.builder()
                .revision(anchor.revision())
                .type(anchor.type())
                .entityId(anchor.entityId())
                .entityLabel(anchor.entityLabel())
                .revisionType(anchor.revisionType())
                .modifiedAt(anchor.modifiedAt())
                .modifiedBy(anchor.modifiedBy())
                .changes(changes)
                .build();
    }

    private List<RevisionDto> referenceRevisions(final AuditedEntityDescriptor descriptor,
                                                 final Object id,
                                                 final AuditedReferenceDescriptor reference) {
        final AuditedEntityDescriptor target = registry.require(reference.type());
        final Set<Object> referenced = new LinkedHashSet<>();

        for (final Object[] triple : triplesFor(descriptor, id)) {
            final Object value = differ.readProperty(descriptor.entityClass(), reference.property(), triple[0]);

            if (value != null) {
                final Object identifier = entityManagerFactory.getPersistenceUnitUtil().getIdentifier(value);

                if (identifier != null) {
                    referenced.add(identifier);
                }
            }
        }

        return referenced.stream()
                .map(referencedId -> toRevisionDtos(target, referencedId, triplesFor(target, referencedId)))
                .flatMap(List::stream)
                .toList();
    }

    @RequiresAdminOrEditorOrUser
    public List<RevisionDto> findRelatedRevisions(final AuditedType type, final String rawId, final AuditedType relatedType) {
        final AuditedEntityDescriptor descriptor = registry.require(type);
        final Object id = parseId(descriptor, rawId);

        authorizeRead(descriptor, id);

        final List<RevisionDto> revisions = new ArrayList<>();

        collectRelated(descriptor.children(), List.of(id), relatedType, false, revisions);

        return revisions.stream()
                .sorted(Comparator.comparing(RevisionDto::revision).reversed())
                .toList();
    }

    private void collectRelated(final List<AuditedChildDescriptor> children,
                                final List<Object> parentIds,
                                final AuditedType relatedType,
                                final boolean withinTarget,
                                final List<RevisionDto> collected) {
        if (parentIds.isEmpty()) {
            return;
        }

        for (final AuditedChildDescriptor child : children) {
            final boolean isTarget = withinTarget || child.type() == relatedType;
            final Map<Object, List<Object[]>> byId = revisionsByChildId(child, parentIds);

            if (isTarget) {
                final AuditedEntityDescriptor target = registry.require(child.type());

                byId.forEach((childId, triples) -> collected.addAll(toRevisionDtos(target, childId, triples)));
            }

            collectRelated(child.children(), List.copyOf(byId.keySet()), relatedType, isTarget, collected);
        }
    }

    private Map<Object, List<Object[]>> revisionsByChildId(final AuditedChildDescriptor child, final List<Object> parentIds) {
        final List<Object[]> triples = envers.revisionTriples(child.entityClass(), false, true,
                q -> q.add(AuditEntity.relatedId(child.parentProperty()).in(parentIds.toArray()))
                        .addOrder(AuditEntity.revisionNumber().asc()));

        final Map<Object, List<Object[]>> byId = new LinkedHashMap<>();

        triples.forEach(triple -> byId.computeIfAbsent(identifierOf(triple[0]), _ -> new ArrayList<>()).add(triple));

        return byId;
    }

    @RequiresAdminOrEditorOrUser
    public RevisionDto findRevision(final AuditedType type, final String rawId, final long revision) {
        return findRevisions(type, rawId).stream()
                .filter(r -> r.revision() == revision)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Unknown revision %d for %s %s".formatted(revision, type, rawId)));
    }

    @RequiresAdminOrEditorOrUser
    public BinaryValueDto findBinary(final AuditedType type, final String rawId, final long revision, final String field) {
        final AuditedEntityDescriptor descriptor = registry.require(type);
        final Object id = parseId(descriptor, rawId);

        authorizeRead(descriptor, id);

        final Object snapshot = envers.findAtRevision(descriptor.entityClass(), id, revision);

        if (differ.isBinaryProperty(descriptor.entityClass(), field)) {
            if (snapshot == null) {
                throw new BadRequestException("Unknown revision %d for %s %s".formatted(revision, type, id));
            }

            return binaryOf(descriptor.entityClass(), snapshot, field, descriptor, id);
        }

        for (final AuditedReferenceDescriptor reference : descriptor.references()) {
            if (!differ.isBinaryProperty(reference.entityClass(), field)) {
                continue;
            }

            final Object holder = snapshot != null ? snapshot : descriptor.findCurrent().apply(id).orElse(null);

            if (holder == null) {
                continue;
            }

            final Optional<Object> referencedId = referencedIdAt(descriptor, reference, holder);

            if (referencedId.isPresent()) {
                final AuditedEntityDescriptor target = registry.require(reference.type());
                final Object referencedSnapshot = requireSnapshot(target, referencedId.get(), revision);

                return binaryOf(target.entityClass(), referencedSnapshot, field, target, referencedId.get());
            }
        }

        throw new BadRequestException("%s has no binary property %s".formatted(type, field));
    }

    private BinaryValueDto binaryOf(final Class<?> entityClass,
                                    final Object snapshot,
                                    final String field,
                                    final AuditedEntityDescriptor descriptor,
                                    final Object id) {
        final Object content = differ.readProperty(entityClass, field, snapshot);

        if (!(content instanceof final byte[] bytes) || bytes.length == 0) {
            throw new ResourceNotFoundException(descriptor.entityClass(), id);
        }

        final Object contentType = differ.readProperty(entityClass, field + "ContentType", snapshot);

        return new BinaryValueDto(bytes, contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : String.valueOf(contentType));
    }

    @RequiresAdmin
    public CountedWindow<RevisionFeedEntryDto> findFeed(final @Nullable AuditedType type, final @Nullable Integer sinceInDays, final GraphqlUtil.ScrollRequest scroll) {
        final FeedCriteria criteria = criteriaFor(type, sinceInDays);
        final long total = countFeed(criteria);

        if (total == 0L) {
            return GraphqlUtil.emptyWindow();
        }

        final long offset = scroll.offsetFor(total);
        final List<AuditRevisionEntity> rows = queryFeed(criteria, (int) offset, scroll.limit() + 1);
        final boolean hasNext = rows.size() > scroll.limit();
        final List<RevisionFeedEntryDto> content = rows.stream()
                .limit(scroll.limit())
                .map(this::toFeedEntry)
                .toList();

        return CountedWindow.of(Window.from(content, OffsetScrollPosition.positionFunction(offset), hasNext), total);
    }

    @RequiresAdmin
    public Page<RevisionFeedEntryDto> findFeedPaged(final @Nullable AuditedType type, final @Nullable Integer sinceInDays, final Pageable pageable) {
        final FeedCriteria criteria = criteriaFor(type, sinceInDays);
        final long total = countFeed(criteria);

        if (total == 0L) {
            return Page.empty(pageable);
        }

        final List<RevisionFeedEntryDto> content = queryFeed(criteria, (int) pageable.getOffset(), pageable.getPageSize()).stream()
                .map(this::toFeedEntry)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    private FeedCriteria criteriaFor(final @Nullable AuditedType type, final @Nullable Integer sinceInDays) {
        return new FeedCriteria(
                sinceFrom(sinceInDays).toEpochMilli(),
                type == null ? null : registry.require(type).entityClass().getName()
        );
    }

    private long countFeed(final FeedCriteria criteria) {
        final var query = entityManager.createQuery("SELECT COUNT(r) FROM AuditRevisionEntity r " + criteria.where(), Long.class)
                .setParameter("since", criteria.since());

        if (criteria.entityName() != null) {
            query.setParameter("entityName", criteria.entityName());
        }

        return Optional.ofNullable(query.getSingleResult()).orElse(0L);
    }

    private List<AuditRevisionEntity> queryFeed(final FeedCriteria criteria, final int offset, final int limit) {
        final var query = entityManager.createQuery("SELECT r FROM AuditRevisionEntity r " + criteria.where() + " ORDER BY r.id DESC", AuditRevisionEntity.class)
                .setParameter("since", criteria.since())
                .setFirstResult(offset)
                .setMaxResults(limit);

        if (criteria.entityName() != null) {
            query.setParameter("entityName", criteria.entityName());
        }

        return query.getResultList();
    }

    @RequiresAdmin
    public RestorePreviewDto preview(final AuditedType type, final String rawId, final long revision, final boolean cascade) {
        final AuditedEntityDescriptor descriptor = registry.require(type);
        final Object id = parseId(descriptor, rawId);
        final Object current = authorizeWrite(descriptor, id);
        final Object snapshot = requireSnapshot(descriptor, id, revision);

        final List<RestorePreviewEntryDto> entries = new ArrayList<>();
        final List<RestoreWarningDto> warnings = new ArrayList<>();

        entries.add(RestorePreviewEntryDto.builder()
                .type(type)
                .id(asLong(id))
                .action(RestoreAction.UPDATE)
                .changes(differ.diff(descriptor.entityClass(), current, snapshot))
                .build());

        previewReferences(descriptor, snapshot, revision, entries);

        if (cascade) {
            previewChildren(descriptor.children(), current, id, revision, entries, warnings);
        }

        return RestorePreviewDto.builder()
                .entries(List.copyOf(entries))
                .warnings(List.copyOf(warnings))
                .build();
    }

    @RequiresAdmin
    public RestoreResultDto restore(final AuditedType type, final String rawId, final long revision, final boolean cascade) {
        final AuditedEntityDescriptor descriptor = registry.require(type);
        final Object id = parseId(descriptor, rawId);
        final Object current = authorizeWrite(descriptor, id);
        final Object snapshot = requireSnapshot(descriptor, id, revision);

        final RestoreContext context = new RestoreContext();

        applyProperties(descriptor.entityClass(), snapshot, current, context);
        context.updated++;

        restoreReferences(descriptor, snapshot, revision, context);

        if (cascade) {
            restoreChildren(descriptor.children(), current, id, revision, context);
        }

        entityManager.flush();

        return RestoreResultDto.builder()
                .revision(revision)
                .updated(context.updated)
                .created(context.created)
                .deleted(context.deleted)
                .warnings(List.copyOf(context.warnings))
                .build();
    }

    private void previewReferences(final AuditedEntityDescriptor descriptor,
                                   final Object snapshot,
                                   final long revision,
                                   final List<RestorePreviewEntryDto> entries) {
        for (final AuditedReferenceDescriptor reference : descriptor.references()) {
            referencedIdAt(descriptor, reference, snapshot).ifPresent(referencedId -> {
                final AuditedEntityDescriptor target = registry.require(reference.type());
                final Object referencedSnapshot = envers.findAtRevision(target.entityClass(), referencedId, revision);
                final Object referencedCurrent = target.findCurrent().apply(referencedId).orElse(null);

                if (referencedSnapshot != null && referencedCurrent != null) {
                    final List<FieldChangeDto> changes = differ.diff(target.entityClass(), referencedCurrent, referencedSnapshot);

                    if (!changes.isEmpty()) {
                        entries.add(RestorePreviewEntryDto.builder()
                                .type(target.type())
                                .id(asLong(referencedId))
                                .action(RestoreAction.UPDATE)
                                .changes(changes)
                                .build());
                    }
                }
            });
        }
    }

    private void previewChildren(final List<AuditedChildDescriptor> children,
                                 final Object managedParent,
                                 final Object historicalParentId,
                                 final long revision,
                                 final List<RestorePreviewEntryDto> entries,
                                 final List<RestoreWarningDto> warnings) {
        for (final AuditedChildDescriptor child : children) {
            final Map<Object, Object> atRevision = indexById(childrenAtRevision(child, historicalParentId, revision));
            final Map<Object, Object> currently = indexById(child.childrenOf().apply(managedParent));

            for (final Map.Entry<Object, Object> entry : atRevision.entrySet()) {
                final Object existing = currently.get(entry.getKey());

                if (existing != null) {
                    final List<FieldChangeDto> changes = differ.diff(child.entityClass(), existing, entry.getValue());

                    if (!changes.isEmpty()) {
                        entries.add(RestorePreviewEntryDto.builder()
                                .type(child.type())
                                .id(asLong(entry.getKey()))
                                .action(RestoreAction.UPDATE)
                                .changes(changes)
                                .build());
                    }

                    previewChildren(child.children(), existing, entry.getKey(), revision, entries, warnings);
                } else {
                    entries.add(RestorePreviewEntryDto.builder()
                            .type(child.type())
                            .id(asLong(entry.getKey()))
                            .action(RestoreAction.CREATE)
                            .changes(differ.diff(child.entityClass(), null, entry.getValue()))
                            .build());
                    warnings.add(RestoreWarningDto.idReassigned(child.type(), asLong(entry.getKey()), null));
                }
            }

            for (final Map.Entry<Object, Object> entry : currently.entrySet()) {
                if (!atRevision.containsKey(entry.getKey())) {
                    entries.add(RestorePreviewEntryDto.builder()
                            .type(child.type())
                            .id(asLong(entry.getKey()))
                            .action(RestoreAction.DELETE)
                            .changes(List.of())
                            .build());
                }
            }
        }
    }

    private void restoreReferences(final AuditedEntityDescriptor descriptor,
                                   final Object snapshot,
                                   final long revision,
                                   final RestoreContext context) {
        for (final AuditedReferenceDescriptor reference : descriptor.references()) {
            referencedIdAt(descriptor, reference, snapshot).ifPresent(referencedId -> {
                final AuditedEntityDescriptor target = registry.require(reference.type());
                final Object referencedSnapshot = envers.findAtRevision(target.entityClass(), referencedId, revision);
                final Object referencedCurrent = target.findCurrent().apply(referencedId).orElse(null);

                if (referencedSnapshot != null && referencedCurrent != null) {
                    applyProperties(target.entityClass(), referencedSnapshot, referencedCurrent, context);
                    context.updated++;
                }
            });
        }
    }

    private Optional<Object> referencedIdAt(final AuditedEntityDescriptor descriptor,
                                            final AuditedReferenceDescriptor reference,
                                            final Object snapshot) {
        final Object value = differ.readProperty(descriptor.entityClass(), reference.property(), snapshot);

        return value == null
                ? Optional.empty()
                : Optional.ofNullable(entityManagerFactory.getPersistenceUnitUtil().getIdentifier(value));
    }

    private void restoreChildren(final List<AuditedChildDescriptor> children,
                                 final Object managedParent,
                                 final Object historicalParentId,
                                 final long revision,
                                 final RestoreContext context) {
        for (final AuditedChildDescriptor child : children) {
            final Map<Object, Object> atRevision = indexById(childrenAtRevision(child, historicalParentId, revision));
            final Map<Object, Object> currently = indexById(child.childrenOf().apply(managedParent));

            for (final Map.Entry<Object, Object> entry : atRevision.entrySet()) {
                final Object existing = currently.get(entry.getKey());

                if (existing != null) {
                    applyProperties(child.entityClass(), entry.getValue(), existing, context);
                    context.updated++;
                    restoreChildren(child.children(), existing, entry.getKey(), revision, context);
                } else {
                    final Object created = instantiate(child.entityClass());

                    applyProperties(child.entityClass(), entry.getValue(), created, context);
                    child.attachToParent().accept(created, managedParent);
                    entityManager.persist(created);
                    entityManager.flush();

                    context.created++;
                    context.warnings.add(RestoreWarningDto.idReassigned(child.type(), asLong(entry.getKey()), asLong(identifierOf(created))));

                    restoreChildren(child.children(), created, entry.getKey(), revision, context);
                }
            }

            for (final Object current : List.copyOf(currently.values())) {
                if (!atRevision.containsKey(identifierOf(current))) {
                    child.detachFromParent().accept(current, managedParent);
                    entityManager.remove(current);
                    context.deleted++;
                }
            }
        }
    }

    private List<Object> childrenAtRevision(final AuditedChildDescriptor child, final Object parentId, final long revision) {
        @SuppressWarnings("unchecked") final List<Object> result = envers.auditReader().createQuery()
                .forEntitiesAtRevision(child.entityClass(), revision)
                .add(AuditEntity.relatedId(child.parentProperty()).eq(parentId))
                .getResultList();

        return result;
    }

    private void applyProperties(final Class<?> entityClass, final Object snapshot, final Object target, final RestoreContext context) {
        final Object unwrapped = Hibernate.unproxy(target);

        for (final SingularAttribute<?, ?> attribute : differ.attributesOf(entityClass)) {
            Object value = differ.read(attribute, snapshot);

            if (value != null && attribute.isAssociation()) {
                final Object referenceId = entityManagerFactory.getPersistenceUnitUtil().getIdentifier(value);
                final Object reference = referenceId == null ? null : entityManager.find(attribute.getJavaType(), referenceId);

                if (reference == null) {
                    context.warnings.add(RestoreWarningDto.builder()
                            .code("REFERENCE_MISSING")
                            .message("%s.%s referenced %s which no longer exists".formatted(entityClass.getSimpleName(), attribute.getName(), referenceId))
                            .build());
                    continue;
                }

                value = reference;
            }

            writeField(unwrapped, attribute.getName(), value);
        }
    }

    private static void writeField(final Object target, final String name, final @Nullable Object value) {
        final Field field = ReflectionUtils.findField(target.getClass(), name);

        if (field == null) {
            return;
        }

        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, target, value);
    }

    private Object instantiate(final Class<?> entityClass) {
        try {
            final var constructor = entityClass.getDeclaredConstructor();

            ReflectionUtils.makeAccessible(constructor);

            return constructor.newInstance();
        } catch (final ReflectiveOperationException e) {
            throw new BadRequestException("Cannot re-create %s: %s".formatted(entityClass.getSimpleName(), e.getMessage()));
        }
    }

    private List<Object[]> triplesFor(final AuditedEntityDescriptor descriptor, final Object id) {
        return envers.revisionTriples(descriptor.entityClass(), false, true,
                q -> q.add(AuditEntity.id().eq(id)).addOrder(AuditEntity.revisionNumber().asc()));
    }

    private List<RevisionDto> toRevisionDtos(final AuditedEntityDescriptor descriptor, final Object id, final List<Object[]> triples) {
        final List<RevisionDto> revisions = new ArrayList<>(triples.size());
        Object previous = null;

        for (final Object[] triple : triples) {
            final Object snapshot = triple[0];
            final AuditRevisionEntity revisionEntity = (AuditRevisionEntity) triple[1];
            final RevisionType revisionType = (RevisionType) triple[2];

            final List<FieldChangeDto> changes = revisionType == RevisionType.DEL
                    ? List.of()
                    : differ.diff(descriptor.entityClass(), previous, snapshot).stream()
                    .map(change -> change.ownedBy(descriptor.type(), asLong(id)))
                    .toList();

            revisions.add(RevisionDto.builder()
                    .revision(revisionEntity.getId())
                    .type(descriptor.type())
                    .entityId(asLong(id))
                    .entityLabel(snapshot == null ? null : registry.labelFor(snapshot))
                    .revisionType(revisionType)
                    .modifiedAt(revisionEntity.getModifiedAt().toInstant(java.time.ZoneOffset.UTC))
                    .modifiedBy(revisionEntity.getModifiedBy())
                    .changes(changes)
                    .build());

            if (revisionType != RevisionType.DEL) {
                previous = snapshot;
            }
        }

        revisions.sort(Comparator.comparing(RevisionDto::revision).reversed());

        return List.copyOf(revisions);
    }

    private RevisionFeedEntryDto toFeedEntry(final AuditRevisionEntity revisionEntity) {
        final Set<AuditedType> types = new HashSet<>();

        for (final String entityName : revisionEntity.getModifiedEntityNames()) {
            final AuditedType type = registry.byEntityName(entityName);

            if (type != null) {
                types.add(type);
            }
        }

        return RevisionFeedEntryDto.builder()
                .revision(revisionEntity.getId())
                .modifiedAt(revisionEntity.getModifiedAt().toInstant(java.time.ZoneOffset.UTC))
                .modifiedBy(revisionEntity.getModifiedBy())
                .types(types.stream().sorted().toList())
                .build();
    }

    private Object requireSnapshot(final AuditedEntityDescriptor descriptor, final Object id, final long revision) {
        final Object snapshot = envers.findAtRevision(descriptor.entityClass(), id, revision);

        if (snapshot == null) {
            throw new BadRequestException("Unknown revision %d for %s %s".formatted(revision, descriptor.type(), id));
        }

        return snapshot;
    }

    private void authorizeRead(final AuditedEntityDescriptor descriptor, final Object id) {
        if (descriptor.aclRoot() == null) {
            return;
        }

        final Optional<Object> current = descriptor.findCurrent().apply(id);

        if (current.isEmpty()) {
            if (!SecurityUtil.isAdministrator()) {
                throw new AccessDeniedException("Access denied");
            }

            return;
        }

        final Object root = descriptor.aclRoot().apply(current.get());

        if (root != null) {
            permissionService.checkReadPermission(root);
        }
    }

    private Object authorizeWrite(final AuditedEntityDescriptor descriptor, final Object id) {
        final Object current = descriptor.findCurrent().apply(id)
                .orElseThrow(() -> new ResourceNotFoundException(descriptor.entityClass(), id));

        if (descriptor.aclRoot() != null) {
            final Object root = descriptor.aclRoot().apply(current);

            if (root != null) {
                permissionService.checkWritePermission(root);
            }
        }

        return current;
    }

    private Object parseId(final AuditedEntityDescriptor descriptor, final String rawId) {
        try {
            return descriptor.parseId(rawId);
        } catch (final NumberFormatException e) {
            throw new BadRequestException("Malformed identifier %s for %s".formatted(rawId, descriptor.type()));
        }
    }

    private Map<Object, Object> indexById(final Collection<Object> entities) {
        final Map<Object, Object> byId = new LinkedHashMap<>();

        for (final Object entity : entities) {
            byId.put(identifierOf(entity), entity);
        }

        return byId;
    }

    private Object identifierOf(final Object entity) {
        return entityManagerFactory.getPersistenceUnitUtil().getIdentifier(entity);
    }

    private static @Nullable Long asLong(final @Nullable Object id) {
        return switch (id) {
            case null -> null;
            case final Number number -> number.longValue();
            default -> null;
        };
    }

    private static Instant sinceFrom(final @Nullable Integer sinceInDays) {
        final int days = sinceInDays == null || sinceInDays == -1 ? DEFAULT_SINCE_IN_DAYS : sinceInDays;

        return LocalDate.now().minusDays(days).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private record FeedCriteria(long since, @Nullable String entityName) {
        private String where() {
            return "WHERE r.modifiedAt >= :since" + (entityName == null ? "" : " AND :entityName MEMBER OF r.modifiedEntityNames");
        }
    }

    private static final class RestoreContext {
        private int updated;
        private int created;
        private int deleted;
        private final List<RestoreWarningDto> warnings = new ArrayList<>();
    }
}