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

package nu.fgv.register.server.user.authority;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.event.JpaEntityListener;
import nu.fgv.register.server.util.AbstractAuditable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Entity
@Table(name = "authority")
@EntityListeners(JpaEntityListener.class)
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@RequiredArgsConstructor
@Getter
@Setter
@ToString
public class Authority extends AbstractAuditable implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    private String id;

    @org.hibernate.annotations.Type(JsonType.class)
    @Column(name = "labels", columnDefinition = "json")
    private Map<String, String> labels;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Authority authority = (Authority) o;

        return !(authority.getId() == null || getId() == null) && Objects.equals(getId(), authority.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass().hashCode());
    }

}
