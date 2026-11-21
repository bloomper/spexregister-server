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
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.image.Image;
import nu.fgv.register.server.security.policy.PermissionService;
import nu.fgv.register.server.spex.character.SpexCharacter;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareSensitiveData;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Reads the history of audited entities, and restores an entity to a revision. Reading needs read access to the entity,
 * or to the spexare it belongs to; restoring needs administration and write access. The social security number in a
 * spexare's history is shown as the birth date to those who may not see it.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuditService {

    private static final int DEFAULT_SINCE_IN_DAYS = 90;
    private static final String EXTERNAL_ID = "uuid";

    private final AuditRegistry registry;
    private final AuditDiffer differ;
    private final EnversAuditSupport envers;
    private final PermissionService permissionService;
    private final EntityManager entityManager;
    private final EntityManagerFactory entityManagerFactory;

    private static void writeField(final Object target, final String name, final @Nullable Object value) {
        final Field field = ReflectionUtils.findField(target.getClass(), name);

        if (field == null) {
            return;
        }

        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, target, value);
    }


    private static Instant startOfDay(final LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private static Instant sinceFrom(final @Nullable LocalDate until, final @Nullable Integer sinceInDays) {
        final int days = sinceInDays == null || sinceInDays == -1 ? DEFAULT_SINCE_IN_DAYS : sinceInDays;

        return startOfDay((until == null ? LocalDate.now() : until).minusDays(days));
    }

    private static String restoreComment(final long revision, final boolean cascade) {
        return "Restored from revision %d%s".formatted(revision, cascade ? " including related records" : "");
    }

    private static @Nullable FieldChangeDto redact(final FieldChangeDto change) {
        final String oldValue = SpexareSensitiveData.birthDateOf(change.oldValue());
        final String newValue = SpexareSensitiveData.birthDateOf(change.newValue());

        return Objects.equals(oldValue, newValue) ? null :
                new FieldChangeDto(change.field(), oldValue, newValue, change.binary(), change.type(), change.entityId());
    }

    private static boolean isSpexareField(final RevisionDto revision, final FieldChangeDto change) {
        return (change.type() == null ? revision.type() : change.type()) == AuditedType.SPEXARE;
    }

    /**
     * The revisions of the entity, newest first.
     *
     * @param type  the entity type
     * @param rawId the entity id
     * @return the revisions
     * @throws org.springframework.security.access.AccessDeniedException when the caller may not read the entity, or it
     *                                                                   is gone and the caller is not an administrator
     * @throws nu.fgv.register.server.util.error.BadRequestException     when the type is base data, whose history
     *                                                                   Spexipedia keeps
     */
    @RequiresAdminOrEditorOrUser
    public List<RevisionDto> findRevisions(final AuditedType type, final String rawId) {
        final AuditedEntityDescriptor descriptor = requireOwnHistory(type);
        final Object id = parseId(descriptor, rawId);

        authorizeRead(descriptor, id);

        final List<RevisionDto> revisions = toRevisionDtos(descriptor, id, triplesFor(descriptor, id)).stream()
                .sorted(Comparator.comparing(RevisionDto::revision).reversed())
                .toList();

        return redactSensitiveData(descriptor, id, revisions);
    }

    private List<RevisionDto> redactSensitiveData(final AuditedEntityDescriptor descriptor, final Object id, final List<RevisionDto> revisions) {
        if (descriptor.type() != AuditedType.SPEXARE || SpexareSensitiveData.isReadableForAll()) {
            return revisions;
        }

        final boolean readable = descriptor.findCurrent().apply(id)
                .filter(Spexare.class::isInstance)
                .map(Spexare.class::cast)
                .map(SpexareSensitiveData::isReadable)
                .orElse(false);

        if (readable) {
            return revisions;
        }

        return revisions.stream()
                .map(revision -> new RevisionDto(revision.revision(), revision.type(), revision.entityId(), revision.entityLabel(),
                        revision.revisionType(), revision.modifiedAt(), revision.modifiedBy(),
                        revision.changes().stream()
                                .map(change -> isSpexareField(revision, change) && SpexareSensitiveData.FIELD.equals(change.field()) ? redact(change) : change)
                                .filter(Objects::nonNull)
                                .toList()))
                .toList();
    }

    /**
     * The revisions of the records of a type that belong to the entity, and of those that belong to them in turn,
     * newest first; for example the activities of a spexare with their spex and task activities.
     *
     * @param type        the entity type
     * @param rawId       the entity id
     * @param relatedType the type of the records
     * @return the revisions
     */
    @RequiresAdminOrEditorOrUser
    public List<RevisionDto> findRelatedRevisions(final AuditedType type, final String rawId, final AuditedType relatedType) {
        final AuditedEntityDescriptor descriptor = requireOwnHistory(type);
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

    /**
     * @param type     the entity type
     * @param rawId    the entity id
     * @param revision the revision number
     * @return the revision as {@link #findRevisions} gives it
     * @throws nu.fgv.register.server.util.error.BadRequestException when the entity has no such revision
     */
    @RequiresAdminOrEditorOrUser
    public RevisionDto findRevision(final AuditedType type, final String rawId, final long revision) {
        return findRevisions(type, rawId).stream()
                .filter(r -> r.revision() == revision)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Unknown revision %d for %s %s".formatted(revision, type, rawId)));
    }

    /**
     * A binary value as it was in the revision, such as a spexare's image.
     *
     * @param type     the entity type
     * @param rawId    the entity id
     * @param revision the revision number
     * @param field    the binary attribute's name
     * @return the bytes and content type
     * @throws nu.fgv.register.server.util.error.BadRequestException when there is no such revision or binary attribute
     */
    @RequiresAdminOrEditorOrUser
    public BinaryValueDto findBinary(final AuditedType type, final String rawId, final long revision, final String field) {
        final AuditedEntityDescriptor descriptor = requireOwnHistory(type);
        final Object id = parseId(descriptor, rawId);

        authorizeRead(descriptor, id);

        if (!differ.isBinaryProperty(descriptor.entityClass(), field)) {
            throw new BadRequestException("%s has no binary property %s".formatted(type, field));
        }

        final Object snapshot = envers.findAtRevision(descriptor.entityClass(), id, revision);

        if (snapshot == null) {
            throw new BadRequestException("Unknown revision %d for %s %s".formatted(revision, type, id));
        }

        return binaryOf(descriptor.entityClass(), snapshot, field, descriptor, id);
    }

    private BinaryValueDto binaryOf(final Class<?> entityClass,
                                    final Object snapshot,
                                    final String field,
                                    final AuditedEntityDescriptor descriptor,
                                    final Object id) {
        final BinaryValueDto value = differ.binaryValueOf(entityClass, field, snapshot);

        if (value == null) {
            throw new ResourceNotFoundException(descriptor.entityClass(), id);
        }

        return value;
    }

    /**
     * The revisions matching the filter, newest first.
     *
     * @param filter the filter
     * @param scroll the window to read
     * @return the window with the total count
     */
    @RequiresAdmin
    public CountedWindow<RevisionFeedEntryDto> findFeed(final RevisionFeedFilter filter, final GraphqlUtil.ScrollRequest scroll) {
        final FeedCriteria criteria = criteriaFor(filter);
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

    /**
     * The revisions matching the filter, newest first, for the REST API.
     *
     * @param filter   the filter
     * @param pageable the page to read
     * @return the page
     */
    @RequiresAdmin
    public Page<RevisionFeedEntryDto> findFeedPaged(final RevisionFeedFilter filter, final Pageable pageable) {
        final FeedCriteria criteria = criteriaFor(filter);
        final long total = countFeed(criteria);

        if (total == 0L) {
            return Page.empty(pageable);
        }

        final List<RevisionFeedEntryDto> content = queryFeed(criteria, (int) pageable.getOffset(), pageable.getPageSize()).stream()
                .map(this::toFeedEntry)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * @return everyone who has made a revision in the change log, by name
     */
    @RequiresAdmin
    public List<String> findAuthors() {
        return entityManager.createQuery("""
                        SELECT DISTINCT r.modifiedBy FROM AuditRevisionEntity r JOIN r.modifiedEntityNames n
                        WHERE r.modifiedBy IS NOT NULL AND n NOT IN :baseData ORDER BY r.modifiedBy
                        """, String.class)
                .setParameter("baseData", baseDataEntityNames())
                .getResultList();
    }

    /**
     * The entities whose changes are left out of the change log: base data and characters, which Spexipedia keeps and
     * only the sync writes here.
     */
    private Set<String> baseDataEntityNames() {
        final Set<String> names = new HashSet<>();

        AuditedType.BASE_DATA.forEach(type -> names.add(registry.require(type).entityClass().getName()));
        names.add(SpexCharacter.class.getName());

        return Set.copyOf(names);
    }

    private AuditedEntityDescriptor requireOwnHistory(final AuditedType type) {
        if (type.isBaseData()) {
            throw new BadRequestException("%s is kept in Spexipedia, which has its history".formatted(type));
        }

        return registry.require(type);
    }

    private FeedCriteria criteriaFor(final RevisionFeedFilter filter) {
        final List<String> clauses = new ArrayList<>();
        final Map<String, Object> parameters = new LinkedHashMap<>();

        clauses.add("r.modifiedAt >= :from");
        parameters.put("from", (filter.from() == null
                ? sinceFrom(filter.to(), filter.sinceInDays())
                : startOfDay(filter.from())).toEpochMilli());

        if (filter.to() != null) {
            // Inclusive of the whole day the caller named, hence the exclusive upper bound.
            clauses.add("r.modifiedAt < :to");
            parameters.put("to", startOfDay(filter.to().plusDays(1)).toEpochMilli());
        }

        // Revisions that changed nothing but base data are Spexipedia's history, not this change log's
        clauses.add("EXISTS (SELECT 1 FROM AuditRevisionEntity o JOIN o.modifiedEntityNames n WHERE o = r AND n NOT IN :baseData)");
        parameters.put("baseData", baseDataEntityNames());

        if (filter.type() != null) {
            if (filter.type().isBaseData()) {
                throw new BadRequestException("%s is kept in Spexipedia, which has its history".formatted(filter.type()));
            }
            clauses.add(":entityName MEMBER OF r.modifiedEntityNames");
            parameters.put("entityName", registry.require(filter.type()).entityClass().getName());
        }

        if (!filter.modifiedBy().isEmpty()) {
            clauses.add("r.modifiedBy IN :modifiedBy");
            parameters.put("modifiedBy", filter.modifiedBy());
        }

        if (!filter.sources().isEmpty()) {
            clauses.add("r.source IN :sources");
            parameters.put("sources", filter.sources());
        }

        return new FeedCriteria("WHERE " + String.join(" AND ", clauses), Map.copyOf(parameters));
    }

    private long countFeed(final FeedCriteria criteria) {
        final var query = entityManager.createQuery("SELECT COUNT(r) FROM AuditRevisionEntity r " + criteria.where(), Long.class);

        criteria.parameters().forEach(query::setParameter);

        return query.getSingleResult();
    }

    private List<AuditRevisionEntity> queryFeed(final FeedCriteria criteria, final int offset, final int limit) {
        final var query = entityManager.createQuery("SELECT r FROM AuditRevisionEntity r " + criteria.where() + " ORDER BY r.id DESC", AuditRevisionEntity.class)
                .setFirstResult(offset)
                .setMaxResults(limit);

        criteria.parameters().forEach(query::setParameter);

        return query.getResultList();
    }

    /**
     * Everything changed in a revision, with how many entities of each type changed.
     *
     * @param revision     the revision number
     * @param limitPerType the most entities of each type to return
     * @return the revision's changes
     * @throws nu.fgv.register.server.util.error.BadRequestException when limitPerType is less than one
     */
    @RequiresAdmin
    public RevisionDetailDto findDetail(final long revision, final int limitPerType) {
        if (limitPerType < 1) {
            throw new BadRequestException("limitPerType must be at least 1");
        }

        final AuditRevisionEntity revisionEntity = entityManager.find(AuditRevisionEntity.class, revision);

        if (revisionEntity == null) {
            throw new ResourceNotFoundException(AuditRevisionEntity.class, revision);
        }

        final List<RevisionTypeSummaryDto> summary = new ArrayList<>();
        final List<RevisionEntityChangeDto> entities = new ArrayList<>();

        for (final String entityName : revisionEntity.getModifiedEntityNames()) {
            final AuditedType type = registry.byEntityName(entityName);

            if (type != null && !type.isBaseData()) {
                final AuditedEntityDescriptor descriptor = registry.require(type);
                final List<Object[]> triples = envers.revisionTriples(descriptor.entityClass(), false, true,
                        q -> q.add(AuditEntity.revisionNumber().eq(revision)).setMaxResults(limitPerType + 1));
                final List<Object[]> returned = triples.subList(0, Math.min(triples.size(), limitPerType));
                final long count = triples.size() > limitPerType
                        ? envers.countRevisionsOfEntity(descriptor.entityClass(), q -> q.add(AuditEntity.revisionNumber().eq(revision)))
                        : triples.size();

                summary.add(RevisionTypeSummaryDto.builder()
                        .type(type)
                        .count(count)
                        .returned(returned.size())
                        .build());
                entities.addAll(entityChangesAt(descriptor, revision, returned));
            }
        }

        summary.sort(Comparator.comparingInt(entry -> entry.type().ordinal()));
        entities.sort(Comparator.comparingInt(entity -> entity.type().ordinal()));

        return RevisionDetailDto.builder()
                .revision(revisionEntity.getId())
                .modifiedAt(revisionEntity.getModifiedAt().toInstant(java.time.ZoneOffset.UTC))
                .modifiedBy(revisionEntity.getModifiedBy())
                .source(revisionEntity.getSource())
                .operation(revisionEntity.getOperation())
                .comment(revisionEntity.getComment())
                .summary(List.copyOf(summary))
                .entities(List.copyOf(entities))
                .build();
    }

    private List<RevisionEntityChangeDto> entityChangesAt(final AuditedEntityDescriptor descriptor,
                                                          final long revision,
                                                          final List<Object[]> triples) {
        final List<RevisionEntityChangeDto> changes = new ArrayList<>(triples.size());

        for (final Object[] triple : triples) {
            final Object snapshot = triple[0];
            final RevisionType revisionType = (RevisionType) triple[2];
            final Object id = snapshot == null ? null : identifierOf(snapshot);

            changes.add(RevisionEntityChangeDto.builder()
                    .type(descriptor.type())
                    .entityId(id == null ? null : externalIdOf(descriptor.entityClass(), id, snapshot))
                    .entityLabel(snapshot == null ? null : registry.labelFor(snapshot))
                    .revisionType(revisionType)
                    .changes(id == null || revisionType == RevisionType.DEL
                            ? List.of()
                            : differ.diff(descriptor.entityClass(),
                                    revisionType == RevisionType.ADD ? null : previousSnapshot(descriptor, id, revision), snapshot).stream()
                            .map(change -> change.ownedBy(descriptor.type(), externalIdOf(descriptor.entityClass(), id, snapshot)))
                            .toList())
                    .target(id == null ? null : targetOf(descriptor, id))
                    .build());
        }

        return changes;
    }

    private @Nullable Object previousSnapshot(final AuditedEntityDescriptor descriptor, final Object id, final long revision) {
        final List<?> previous = envers.revisionEntities(descriptor.entityClass(), false,
                q -> q.add(AuditEntity.id().eq(id))
                        .add(AuditEntity.revisionNumber().lt(revision))
                        .addOrder(AuditEntity.revisionNumber().desc())
                        .setMaxResults(1));

        return previous.isEmpty() ? null : previous.getFirst();
    }

    private @Nullable RevisionTargetDto targetOf(final AuditedEntityDescriptor descriptor, final Object id) {
        final Function<Object, @Nullable Object> accessRoot = descriptor.accessRoot();

        if (accessRoot == null) {
            return null;
        }

        final Object root = descriptor.findCurrent().apply(id).map(accessRoot).orElse(null);

        if (root == null) {
            return null;
        }

        final Object unwrapped = Hibernate.unproxy(root);
        final AuditedType type = registry.byEntityName(Hibernate.getClass(unwrapped).getName());
        final String rootId = externalIdOf(Hibernate.getClass(unwrapped), identifierOf(unwrapped), unwrapped);

        if (type == null || rootId == null) {
            return null;
        }

        return RevisionTargetDto.builder()
                .type(type)
                .id(rootId)
                .label(registry.labelFor(unwrapped))
                .build();
    }

    /**
     * What {@link #restore} would change, without changing anything.
     *
     * @param type     the entity type
     * @param rawId    the entity id
     * @param revision the revision to restore
     * @param cascade  whether to restore the records that belong to the entity too
     * @return the changes and warnings
     */
    @RequiresAdmin
    public RestorePreviewDto preview(final AuditedType type, final String rawId, final long revision, final boolean cascade) {
        final AuditedEntityDescriptor descriptor = requireOwnHistory(type);
        final Object id = parseId(descriptor, rawId);
        final Object current = authorizeWrite(descriptor, id);
        final Object snapshot = requireSnapshot(descriptor, id, revision);

        final List<RestorePreviewEntryDto> entries = new ArrayList<>();
        final List<RestoreWarningDto> warnings = new ArrayList<>();

        entries.add(RestorePreviewEntryDto.builder()
                .type(type)
                .id(externalIdOf(descriptor.entityClass(), id, current))
                .action(RestoreAction.UPDATE)
                .changes(differ.diff(descriptor.entityClass(), current, snapshot))
                .build());

        if (current instanceof final Spexare spexare && snapshot instanceof final Spexare then) {
            final Spexare wanted = then.getPartner() == null ? null : entityManager.find(Spexare.class, identifierOf(then.getPartner()));

            // A partner who no longer exists is left out with a warning by the restore, which keeps the present one
            partnerLinks(spexare, spexare.getPartner(), then.getPartner() != null && wanted == null ? spexare.getPartner() : wanted)
                    .forEach((other, partner) -> warnings.add(partnerWarning(other, partner)));
        }

        if (cascade) {
            previewChildren(descriptor.children(), current, id, revision, entries, warnings);
        }

        return RestorePreviewDto.builder()
                .entries(List.copyOf(entries))
                .warnings(List.copyOf(warnings))
                .build();
    }

    /**
     * Restores the entity's audited fields to the revision. With cascade, the
     * records that belong to it are restored too: those changed are put back, those added since are removed and those
     * removed since are re-created with the same external ids, except one that must refer to something that no longer
     * exists, which is left out with a warning. The entity itself must still exist. A spexare's partner
     * is linked back from the other side and whoever lost a partner by it is freed, each with a warning. Recorded as a
     * {@link AuditSource#RESTORE} revision.
     *
     * @param type     the entity type
     * @param rawId    the entity id
     * @param revision the revision to restore
     * @param cascade  whether to restore the records that belong to the entity too
     * @return how many entities were updated, created and deleted, and warnings such as references that no longer exist
     * @throws nu.fgv.register.server.util.error.ResourceNotFoundException when the entity no longer exists
     */
    @RequiresAdmin
    public RestoreResultDto restore(final AuditedType type, final String rawId, final long revision, final boolean cascade) {
        AuditContext.stamp(AuditContext.currentOrSystem()
                .withSource(AuditSource.RESTORE)
                .withCommentIfAbsent(restoreComment(revision, cascade)));

        final AuditedEntityDescriptor descriptor = requireOwnHistory(type);
        final Object id = parseId(descriptor, rawId);
        final Object current = authorizeWrite(descriptor, id);
        final Object snapshot = requireSnapshot(descriptor, id, revision);

        final RestoreContext context = new RestoreContext();
        final Spexare partnerBefore = current instanceof final Spexare spexare ? spexare.getPartner() : null;

        applyProperties(descriptor.entityClass(), snapshot, current, context);
        context.updated++;

        if (current instanceof final Spexare spexare) {
            partnerLinks(spexare, partnerBefore, spexare.getPartner()).forEach((other, partner) -> {
                other.setPartner(partner);
                context.updated++;
                context.warnings.add(partnerWarning(other, partner));
            });
        }

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

    /**
     * The other spexare a restored partner link reaches, each with the partner it is to have: a partner is linked from
     * both sides, so the one left is freed, the one taken is linked back, and whoever that one had is freed.
     */
    private Map<Spexare, @Nullable Spexare> partnerLinks(final Spexare spexare, final @Nullable Spexare before, final @Nullable Spexare after) {
        final Map<Spexare, @Nullable Spexare> links = new LinkedHashMap<>();

        if (sameSpexare(before, after)) {
            return links;
        }
        if (before != null && sameSpexare(before.getPartner(), spexare)) {
            links.put(before, null);
        }
        if (after != null) {
            final Spexare theirs = after.getPartner();

            if (theirs != null && !sameSpexare(theirs, spexare)) {
                links.put(theirs, null);
            }
            links.put(after, spexare);
        }

        return links;
    }

    private boolean sameSpexare(final @Nullable Spexare one, final @Nullable Spexare other) {
        return one == null || other == null ? one == other : Objects.equals(identifierOf(one), identifierOf(other));
    }

    private RestoreWarningDto partnerWarning(final Spexare other, final @Nullable Spexare partner) {
        final String name = Objects.requireNonNullElse(registry.labelFor(other), String.valueOf(other.getUuid()));

        return RestoreWarningDto.builder()
                .code(partner == null ? "PARTNER_UNLINKED" : "PARTNER_LINKED")
                .message(partner == null ? "%s no longer has a partner".formatted(name) : "%s is linked back as partner".formatted(name))
                .build();
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
                                .id(externalIdOf(child.entityClass(), entry.getKey(), existing))
                                .action(RestoreAction.UPDATE)
                                .changes(changes)
                                .build());
                    }

                    previewChildren(child.children(), existing, entry.getKey(), revision, entries, warnings);
                } else {
                    final RestoreWarningDto missing = missingRequiredReference(child.entityClass(), entry.getValue());

                    if (missing != null) {
                        warnings.add(missing);
                        continue;
                    }

                    entries.add(RestorePreviewEntryDto.builder()
                            .type(child.type())
                            .id(externalIdOf(child.entityClass(), entry.getKey(), entry.getValue()))
                            .action(RestoreAction.CREATE)
                            .changes(differ.diff(child.entityClass(), null, entry.getValue()))
                            .build());
                }
            }

            for (final Map.Entry<Object, Object> entry : currently.entrySet()) {
                if (!atRevision.containsKey(entry.getKey())) {
                    entries.add(RestorePreviewEntryDto.builder()
                            .type(child.type())
                            .id(externalIdOf(child.entityClass(), entry.getKey(), entry.getValue()))
                            .action(RestoreAction.DELETE)
                            .changes(List.of())
                            .build());
                }
            }
        }
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
                    final RestoreWarningDto missing = missingRequiredReference(child.entityClass(), entry.getValue());

                    // Without what it must refer to the record cannot exist again, and nothing below it either
                    if (missing != null) {
                        context.warnings.add(missing);
                        continue;
                    }

                    final Object created = instantiate(child.entityClass());

                    applyProperties(child.entityClass(), entry.getValue(), created, context);
                    child.attachToParent().accept(created, managedParent);
                    entityManager.persist(created);
                    entityManager.flush();

                    context.created++;

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

            if (AuditDiffer.isAuditedBinary(entityClass, attribute)) {
                restoreImage(unwrapped, attribute, value);
                continue;
            }

            if (value != null && attribute.isAssociation()) {
                final Object referenceId = entityManagerFactory.getPersistenceUnitUtil().getIdentifier(value);
                final Object reference = referenceId == null ? null : entityManager.find(attribute.getJavaType(), referenceId);

                if (reference == null) {
                    context.warnings.add(referenceMissing(entityClass, attribute, referenceId));
                    continue;
                }

                value = reference;
            }

            writeField(unwrapped, attribute.getName(), value);
        }
    }

    /**
     * A warning for the first reference the snapshot must have that no longer exists, null when all are there. A record
     * to re-create cannot be saved without them.
     */
    private @Nullable RestoreWarningDto missingRequiredReference(final Class<?> entityClass, final Object snapshot) {
        for (final SingularAttribute<?, ?> attribute : differ.attributesOf(entityClass)) {
            final Object value = attribute.isAssociation() && !attribute.isOptional() && !AuditDiffer.isAuditedBinary(entityClass, attribute)
                    ? differ.read(attribute, snapshot) : null;

            if (value != null) {
                final Object referenceId = entityManagerFactory.getPersistenceUnitUtil().getIdentifier(value);

                if (referenceId == null || entityManager.find(attribute.getJavaType(), referenceId) == null) {
                    return referenceMissing(entityClass, attribute, referenceId);
                }
            }
        }

        return null;
    }

    private static RestoreWarningDto referenceMissing(final Class<?> entityClass, final SingularAttribute<?, ?> attribute, final @Nullable Object referenceId) {
        return RestoreWarningDto.builder()
                .code("REFERENCE_MISSING")
                .message("%s.%s referenced %s which no longer exists".formatted(entityClass.getSimpleName(), attribute.getName(), referenceId))
                .build();
    }

    private void restoreImage(final Object target, final SingularAttribute<?, ?> attribute, final @Nullable Object snapshotValue) {
        final PersistenceUnitUtil util = entityManagerFactory.getPersistenceUnitUtil();
        final Object current = differ.read(attribute, target);

        if (snapshotValue == null) {
            writeField(target, attribute.getName(), null);
            return;
        }

        if (current != null && Objects.equals(util.getIdentifier(current), util.getIdentifier(snapshotValue))) {
            return;
        }

        final Image image = (Image) Hibernate.unproxy(snapshotValue);

        writeField(target, attribute.getName(), Image.of(image.getData().clone(), image.getContentType()));
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
        final String externalId = externalIdOf(descriptor.entityClass(), id);
        Object previous = null;

        for (final Object[] triple : triples) {
            final Object snapshot = triple[0];
            final AuditRevisionEntity revisionEntity = (AuditRevisionEntity) triple[1];
            final RevisionType revisionType = (RevisionType) triple[2];

            final List<FieldChangeDto> changes = revisionType == RevisionType.DEL
                    ? List.of()
                    : differ.diff(descriptor.entityClass(), previous, snapshot).stream()
                    .map(change -> change.ownedBy(descriptor.type(), externalId))
                    .toList();

            revisions.add(RevisionDto.builder()
                    .revision(revisionEntity.getId())
                    .type(descriptor.type())
                    .entityId(externalId)
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

            if (type != null && !type.isBaseData()) {
                types.add(type);
            }
        }

        return RevisionFeedEntryDto.builder()
                .revision(revisionEntity.getId())
                .modifiedAt(revisionEntity.getModifiedAt().toInstant(java.time.ZoneOffset.UTC))
                .modifiedBy(revisionEntity.getModifiedBy())
                .types(types.stream().sorted().toList())
                .source(revisionEntity.getSource())
                .operation(revisionEntity.getOperation())
                .comment(revisionEntity.getComment())
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
        if (descriptor.accessRoot() == null) {
            return;
        }

        final Optional<Object> current = descriptor.findCurrent().apply(id);

        if (current.isEmpty()) {
            if (!SecurityUtil.isAdministrator()) {
                throw new AccessDeniedException("Access denied");
            }

            return;
        }

        final Object root = descriptor.accessRoot().apply(current.get());

        if (root != null) {
            permissionService.checkReadPermission(root);
        }
    }

    private Object authorizeWrite(final AuditedEntityDescriptor descriptor, final Object id) {
        final Object current = descriptor.findCurrent().apply(id)
                .orElseThrow(() -> new ResourceNotFoundException(descriptor.entityClass(), id));

        if (descriptor.accessRoot() != null) {
            final Object root = descriptor.accessRoot().apply(current);

            if (root != null) {
                permissionService.checkWritePermission(root);
            }
        }

        return current;
    }

    private Object parseId(final AuditedEntityDescriptor descriptor, final String rawId) {
        if (!hasExternalId(descriptor.entityClass())) {
            return descriptor.parseId(rawId);
        }

        final UUID uuid;

        try {
            uuid = UUID.fromString(rawId);
        } catch (final IllegalArgumentException e) {
            throw new BadRequestException("Malformed identifier %s for %s".formatted(rawId, descriptor.type()));
        }

        final List<?> snapshots = envers.revisionEntities(descriptor.entityClass(), true,
                q -> q.add(AuditEntity.property(EXTERNAL_ID).eq(uuid))
                        .addOrder(AuditEntity.revisionNumber().desc())
                        .setMaxResults(1));

        if (snapshots.isEmpty()) {
            throw new ResourceNotFoundException(descriptor.entityClass(), rawId);
        }

        return identifierOf(snapshots.getFirst());
    }

    private boolean hasExternalId(final Class<?> entityClass) {
        return entityManagerFactory.getMetamodel().entity(entityClass).getSingularAttributes().stream()
                .anyMatch(attribute -> EXTERNAL_ID.equals(attribute.getName()));
    }

    private @Nullable String externalIdOf(final Class<?> entityClass, final @Nullable Object id) {
        if (id == null) {
            return null;
        }
        if (!hasExternalId(entityClass)) {
            return id.toString();
        }

        final List<?> snapshots = envers.revisionEntities(entityClass, true,
                q -> q.add(AuditEntity.id().eq(id))
                        .addOrder(AuditEntity.revisionNumber().desc())
                        .setMaxResults(1));

        return snapshots.isEmpty() ? null : externalIdOf(entityClass, id, snapshots.getFirst());
    }

    private @Nullable String externalIdOf(final Class<?> entityClass, final Object id, final @Nullable Object snapshot) {
        if (!hasExternalId(entityClass)) {
            return id.toString();
        }

        final Object uuid = snapshot == null ? null : differ.readProperty(entityClass, EXTERNAL_ID, snapshot);

        return uuid == null ? externalIdOf(entityClass, id) : uuid.toString();
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

    private record FeedCriteria(String where, Map<String, Object> parameters) {
    }

    private static final class RestoreContext {
        private final List<RestoreWarningDto> warnings = new ArrayList<>();
        private int updated;
        private int created;
        private int deleted;
    }
}