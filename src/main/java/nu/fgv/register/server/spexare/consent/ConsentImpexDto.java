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

package nu.fgv.register.server.spexare.consent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nu.fgv.register.server.util.AbstractAuditableDto;
import nu.fgv.register.server.util.impex.model.excel.ExcelCell;
import nu.fgv.register.server.util.impex.model.excel.ExcelSheet;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ExcelSheet(name = "consent.impex.sheetName")
public class ConsentImpexDto extends AbstractAuditableDto<ConsentImpexDto> {
    @JsonProperty("spexareId")
    @ExcelCell(header = "consent.impex.spexareId.columnName", position = 0)
    private Long spexareId;

    @JsonProperty("id")
    @ExcelCell(header = "consent.impex.id.columnName", position = 1)
    private Long id;

    @JsonProperty("value")
    @ExcelCell(header = "consent.impex.value.columnName", position = 2, updatable = true, mandatory = true)
    private Boolean value;

    @JsonProperty("typeId")
    @ExcelCell(header = "common.impex.type.id.columnName", position = 3, updatable = true, mandatory = true)
    private String typeId;

    @JsonProperty("typeLabel")
    @ExcelCell(header = "common.impex.type.label.columnName", position = 4)
    private String typeLabel;

}
