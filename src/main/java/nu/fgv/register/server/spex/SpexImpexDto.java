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

package nu.fgv.register.server.spex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nu.fgv.register.server.util.impex.model.AbstractAuditableImpexDto;
import nu.fgv.register.server.util.impex.model.excel.ExcelCell;
import nu.fgv.register.server.util.impex.model.excel.ExcelSheet;
import org.jspecify.annotations.Nullable;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ExcelSheet(name = "spex.impex.sheetName")
public class SpexImpexDto extends AbstractAuditableImpexDto<SpexImpexDto> {

    @JsonProperty("id")
    @ExcelCell(header = "spex.impex.id.columnName", position = 1, primaryKey = true)
    private Long id;

    @NotBlank(message = "{spex.year.notEmpty}")
    @Size(max = 4, message = "{spex.year.size}")
    @Pattern(regexp = "^(19|20|21)\\d{2}$", message = "{spex.year.regexp}")
    @JsonProperty("year")
    @ExcelCell(header = "spex.impex.year.columnName", position = 2, updatable = true, mandatory = true)
    private String year;

    @NotBlank(message = "{spex.title.notEmpty}")
    @Size(max = 255, message = "{spex.title.size}")
    @Column(name = "title", nullable = false)
    @JsonProperty("title")
    @ExcelCell(header = "spex.impex.title.columnName", position = 3, updatable = true, mandatory = true)
    private String title;

    @Nullable
    @JsonProperty("posterUrl")
    @ExcelCell(header = "spex.impex.posterUrl.columnName", position = 4, updatable = true)
    private String posterUrl;

    @NotBlank(message = "{spex.category.notEmpty}")
    @JsonProperty("categoryId")
    @ExcelCell(header = "spex.impex.category.id.columnName", position = 5, updatable = true)
    private Long categoryId;

    @Nullable
    @JsonProperty("categoryName")
    @ExcelCell(header = "spex.impex.category.name.columnName", position = 6)
    private String categoryName;

}
