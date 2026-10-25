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
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;


/**
 * Thin access to the Envers {@link AuditReader} of the current entity manager.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
@RequiredArgsConstructor
public class EnversAuditSupport {

    private final EntityManager entityManager;

    public AuditReader auditReader() {
        return AuditReaderFactory.get(entityManager);
    }

    /**
     * @param entityType            the entity type
     * @param selectEntitiesOnly    whether to return the snapshots only
     * @param selectDeletedEntities whether to include removals
     * @param customize             adds criteria and order to the query
     * @param <E>                   the entity type
     * @return Envers' rows of snapshot, revision entity and revision type, or the snapshots only
     */
    @SuppressWarnings("unchecked")
    public <E> List<Object[]> revisionTriples(
            final Class<E> entityType,
            final boolean selectEntitiesOnly,
            final boolean selectDeletedEntities,
            final Consumer<AuditQuery> customize
    ) {
        final AuditQuery q = auditReader()
                .createQuery()
                .forRevisionsOfEntity(entityType, selectEntitiesOnly, selectDeletedEntities);

        customize.accept(q);

        return (List<Object[]>) q.getResultList();
    }

    /**
     * @param entityType the entity type
     * @param customize  adds criteria to the query
     * @param <E>        the entity type
     * @return how many revisions match, removals included
     */
    public <E> long countRevisionsOfEntity(final Class<E> entityType, final Consumer<AuditQuery> customize) {
        final AuditQuery q = auditReader()
                .createQuery()
                .forRevisionsOfEntity(entityType, false, true)
                .addProjection(AuditEntity.revisionNumber().count());

        customize.accept(q);

        return ((Number) q.getSingleResult()).longValue();
    }

    /**
     * @param entityType            the entity type
     * @param selectDeletedEntities whether to include removals
     * @param customize             adds criteria and order to the query
     * @param <E>                   the entity type
     * @return the snapshots
     */
    @SuppressWarnings("unchecked")
    public <E> List<E> revisionEntities(
            final Class<E> entityType,
            final boolean selectDeletedEntities,
            final Consumer<AuditQuery> customize
    ) {
        final AuditQuery q = auditReader().createQuery().forRevisionsOfEntity(entityType, true, selectDeletedEntities);

        customize.accept(q);

        return (List<E>) q.getResultList();
    }

    /**
     * @param entityType the entity type
     * @param id         the entity id
     * @param <E>        the entity type
     * @param <ID>       the id type
     * @return the entity's revision numbers, oldest first
     */
    public <E, ID> List<Number> revisionNumbers(final Class<E> entityType, final ID id) {
        return auditReader().getRevisions(entityType, id);
    }

    /**
     * @param entityType the entity type
     * @param id         the entity id
     * @param revision   the revision number
     * @param <E>        the entity type
     * @param <ID>       the id type
     * @return the entity as it was at the revision, or null when it did not exist
     */
    public <E, ID> @Nullable E findAtRevision(final Class<E> entityType, final ID id, final Number revision) {
        return auditReader().find(entityType, id, revision);
    }
}
