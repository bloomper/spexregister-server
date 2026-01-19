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

package nu.fgv.register.server.spex.category;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nu.fgv.register.server.impex.model.AbstractAuditableImpexDto;
import nu.fgv.register.server.impex.model.excel.ExcelCell;
import nu.fgv.register.server.impex.model.excel.ExcelSheet;
import org.jspecify.annotations.Nullable;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ExcelSheet(name = "spexCategory.impex.sheetName")
public class SpexCategoryImpexDto extends AbstractAuditableImpexDto<SpexCategoryImpexDto> {

    @JsonProperty("id")
    @ExcelCell(header = "spexCategory.impex.id.columnName", position = 1, primaryKey = true)
    private Long id;

    @NotBlank(message = "{spexCategory.name.notEmpty}")
    @Size(max = 255, message = "{spexCategory.name.maxSize}")
    @JsonProperty("name")
    @ExcelCell(header = "spexCategory.impex.name.columnName", position = 2, updatable = true, mandatory = true)
    private String name;

    @NotBlank(message = "{spexCategory.firstYear.notEmpty}")
    @Size(max = 4, message = "{spexCategory.firstYear.maxSize}")
    @Pattern(regexp = "^(19|20|21)\\d{2}$", message = "{spexCategory.firstYear.regexp}")
    @JsonProperty("firstYear")
    @ExcelCell(header = "spexCategory.impex.firstYear.columnName", position = 3, updatable = true, mandatory = true)
    private String firstYear;

    @Nullable
    @JsonProperty("logoUrl")
    @ExcelCell(header = "spexCategory.impex.logoUrl.columnName", position = 4)
    private String logoUrl;

}
