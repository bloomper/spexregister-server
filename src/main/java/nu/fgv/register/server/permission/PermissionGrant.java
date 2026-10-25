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

package nu.fgv.register.server.permission;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import nu.fgv.register.server.security.policy.Permission;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.util.AbstractAuditable;
import org.hibernate.envers.Audited;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A permission given to one user or to one role, on the target its {@link PermissionGrantScope} names, valid through
 * {@code validUntil} when set.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Entity
@Table(name = "permission_grant")
@Audited
@RequiredArgsConstructor
@Getter
@Setter
public class PermissionGrant extends AbstractAuditable implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private @Nullable User user;

    @Column(name = "role", length = 16)
    private @Nullable String role;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", length = 16, nullable = false)
    private Permission permission;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", length = 32, nullable = false)
    private PermissionGrantScope scope;

    @ManyToOne
    private @Nullable Spexare spexare;

    @ManyToOne
    private @Nullable Spex spex;

    @Column(name = "valid_until")
    private @Nullable LocalDate validUntil;

    /**
     * @param today the current date
     * @return whether the last valid day has passed
     */
    public boolean isExpired(final LocalDate today) {
        return validUntil != null && validUntil.isBefore(today);
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final PermissionGrant grant = (PermissionGrant) o;

        if (grant.getId() == null || getId() == null) {
            return false;
        }

        return Objects.equals(getId(), grant.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass().hashCode());
    }
}
