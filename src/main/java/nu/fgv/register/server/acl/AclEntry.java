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

package nu.fgv.register.server.acl;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Immutable;

import java.util.Objects;

@Entity
@Immutable
@Table(name = "acl_entry", uniqueConstraints = {
        @UniqueConstraint(name = "UC_ACL_OBJECT_IDENTITY_ACE_ORDER", columnNames = {"acl_object_identity", "ace_order"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public final class AclEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "acl_object_identity", referencedColumnName = "id", nullable = false)
    private AclObjectIdentity aclObjectIdentity;

    @Column(name = "ace_order", nullable = false)
    private int aceOrder;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sid", referencedColumnName = "id", nullable = false)
    private AclSid aclSid;

    @Column(name = "mask", nullable = false)
    private int mask;

    @Column(name = "granting", nullable = false)
    private boolean granting;

    @Column(name = "audit_success", nullable = false)
    private boolean auditSuccess;

    @Column(name = "audit_failure", nullable = false)
    private boolean auditFailure;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AclEntry aclEntry = (AclEntry) o;
        if (aclEntry.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), aclEntry.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getClass().hashCode());
    }
}
