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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.util.AbstractAuditableDto;
import nu.fgv.register.server.util.impex.model.ExcelCell;
import nu.fgv.register.server.util.impex.model.ExcelSheet;
import org.springframework.hateoas.server.core.Relation;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
@Relation(collectionRelation = "spex-categories", itemRelation = "spex-category")
@JsonIgnoreProperties(ignoreUnknown = true)
@ExcelSheet(name = "Spex categories")
public class SpexCategoryDto extends AbstractAuditableDto<SpexCategoryDto> {
    @JsonProperty("id")
    @ExcelCell(header = "Id", position = 0)
    private Long id;

    @JsonProperty("name")
    @ExcelCell(header = "Name", position = 1, updatable = true, mandatory = true)
    private String name;

    @JsonProperty("firstYear")
    @ExcelCell(header = "First year", position = 2, updatable = true, mandatory = true)
    private String firstYear;

    @Builder
    public SpexCategoryDto(
            final Long id,
            final String name,
            final String firstYear,
            final String createdBy,
            final Instant createdAt,
            final String lastModifiedBy,
            final Instant lastModifiedAt
    ) {
        super(createdBy, createdAt, lastModifiedBy, lastModifiedAt);
        this.id = id;
        this.name = name;
        this.firstYear = firstYear;
    }
}
