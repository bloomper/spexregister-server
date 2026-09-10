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

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.ModifiedEntityNames;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Entity
@Table(name = "revinfo")
@RevisionEntity(AuditRevisionListener.class)
@NoArgsConstructor
@Getter
@Setter
public class AuditRevisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    private Long id;

    @RevisionTimestamp
    @Getter(AccessLevel.NONE)
    @Column(name = "modified_at", nullable = false)
    private Long modifiedAt;

    @Column(name = "modified_by")
    private String modifiedBy;

    @ElementCollection(
            fetch = FetchType.EAGER
    )
    @JoinTable(
            name = "revchanges",
            joinColumns = {@JoinColumn(
                    name = "rev"
            )}
    )
    @Column(
            name = "entityname"
    )
    @Fetch(FetchMode.JOIN)
    @ModifiedEntityNames
    private Set<String> modifiedEntityNames = new HashSet<>();

    @Transient
    public LocalDateTime getModifiedAt() {
        return Instant.ofEpochMilli(modifiedAt).atOffset(ZoneOffset.UTC).toLocalDateTime();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final AuditRevisionEntity o = (AuditRevisionEntity) obj;

        if (o.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), o.getId());

    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getClass().hashCode());
    }
}