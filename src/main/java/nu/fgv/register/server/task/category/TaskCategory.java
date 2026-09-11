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

package nu.fgv.register.server.task.category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.util.AbstractAuditable;
import nu.fgv.register.server.util.search.HierarchicalPropertyBinder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.Audited;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyBinding;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import static nu.fgv.register.server.util.search.DefaultOverridingLuceneAnalysisConfigurer.NORMALIZER_LOWERCASE;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Entity
@Table(name = "task_category")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Audited
@RequiredArgsConstructor
@Getter
@Setter
@ToString
public class TaskCategory extends AbstractAuditable implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "{taskCategory.name.notEmpty}")
    @Size(max = 255, message = "{taskCategory.name.maxSize}")
    @Column(name = "name", nullable = false)
    @KeywordField(searchable = Searchable.YES, normalizer = NORMALIZER_LOWERCASE)
    @PropertyBinding(binder = @PropertyBinderRef(type = HierarchicalPropertyBinder.class))
    private String name;

    @Column(name = "actor_present")
    private Boolean actorPresent;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TaskCategory taskCategory = (TaskCategory) o;

        if (taskCategory.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), taskCategory.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass().hashCode());
    }
}
