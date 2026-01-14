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

package nu.fgv.register.server.user;

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
@ExcelSheet(name = "user.impex.sheetName")
public class UserImpexDto extends AbstractAuditableDto<UserImpexDto> {
    @JsonProperty("id")
    @ExcelCell(header = "user.impex.id.columnName", position = 0)
    private Long id;

    @JsonProperty("externalId")
    @ExcelCell(header = "user.impex.externalId.columnName", position = 1)
    private String externalId;

    @JsonProperty("email")
    @ExcelCell(header = "user.impex.email.columnName", position = 2)
    private String email;

    @JsonProperty("stateId")
    @ExcelCell(header = "user.impex.state.id.columnName", position = 3, updatable = true, mandatory = true)
    private String stateId;

    @JsonProperty("stateLabel")
    @ExcelCell(header = "user.impex.state.label.columnName", position = 4)
    private String stateLabel;

    @JsonProperty("spexareId")
    @ExcelCell(header = "user.impex.spexare.id.columnName", position = 5, updatable = true)
    private Long spexareId;

}
