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

package nu.fgv.register.server.spex.category;

import jakarta.persistence.EntityListeners;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.event.JpaEntityListener;
import nu.fgv.register.server.util.AbstractAuditable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.springframework.lang.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import static nu.fgv.register.server.util.search.DefaultOverridingLuceneAnalysisConfigurer.NORMALIZER_LOWERCASE;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Entity
@Table(name = "spex_category")
@EntityListeners(JpaEntityListener.class)
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@RequiredArgsConstructor
@Getter
@Setter
@ToString
public class SpexCategory extends AbstractAuditable implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "{spexCategory.name.notEmpty}")
    @Size(max = 255, message = "{spexCategory.name.maxSize}")
    @Column(name = "name", nullable = false)
    @KeywordField(aggregable = Aggregable.YES, searchable = Searchable.YES, normalizer = NORMALIZER_LOWERCASE)
    private String name;

    @NotBlank(message = "{spexCategory.firstYear.notEmpty}")
    @Size(max = 4, message = "{spexCategory.firstYear.maxSize}")
    @Pattern(regexp = "^(19|20|21)\\d{2}$", message = "{spexCategory.firstYear.regexp}")
    @Column(name = "first_year", length = 4, nullable = false)
    private String firstYear;

    @Lob
    @Column(name = "logo", columnDefinition = "MEDIUMBLOB")
    @Basic(fetch = FetchType.LAZY)
    @ToString.Exclude
    @Nullable
    private byte[] logo;

    @Column(name = "logo_content_type")
    @Nullable
    private String logoContentType;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SpexCategory spexCategory = (SpexCategory) o;

        if (spexCategory.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), spexCategory.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass().hashCode());
    }
}
