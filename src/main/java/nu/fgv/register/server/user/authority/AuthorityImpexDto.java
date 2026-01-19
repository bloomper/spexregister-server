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

package nu.fgv.register.server.user.authority;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nu.fgv.register.server.impex.model.AbstractImpexDto;
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
@ExcelSheet(name = "authority.impex.sheetName")
public class AuthorityImpexDto extends AbstractImpexDto {

    @NotEmpty(message = "{authority.user.notEmpty}")
    @JsonProperty("userId")
    @ExcelCell(header = "authority.impex.user.id.columnName", position = 1, ignoreIfNull = true)
    private Long userId;

    @JsonProperty("id")
    @ExcelCell(header = "authority.impex.id.columnName", position = 2, primaryKey = true, updatable = true)
    private String id;

    @Nullable
    @JsonProperty("label")
    @ExcelCell(header = "authority.impex.label.columnName", position = 3)
    private String label;

}
