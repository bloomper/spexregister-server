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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nu.fgv.register.server.util.impex.model.AbstractAuditableImpexDto;
import nu.fgv.register.server.util.impex.model.excel.ExcelCell;
import nu.fgv.register.server.util.impex.model.excel.ExcelSheet;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Getter
@Setter
@NoArgsConstructor
@ExcelSheet(name = "spex.impex.revival.sheetName")
public class SpexRevivalImpexDto extends AbstractAuditableImpexDto<SpexRevivalImpexDto> {

    @JsonProperty("id")
    @ExcelCell(header = "spex.impex.revival.id.columnName", position = 1, primaryKey = true)
    private Long id;

    @JsonProperty("year")
    @ExcelCell(header = "spex.impex.revival.year.columnName", position = 2, updatable = true, mandatory = true)
    private String year;

    @JsonProperty("parentId")
    @ExcelCell(header = "spex.impex.revival.parent.id.columnName", position = 3, updatable = true)
    private Long parentId;

    @JsonProperty("parentYear")
    @ExcelCell(header = "spex.impex.revival.parent.year.columnName", position = 4)
    private String parentYear;

    @JsonProperty("parentTitle")
    @ExcelCell(header = "spex.impex.revival.parent.title.columnName", position = 5)
    private String parentTitle;

}
