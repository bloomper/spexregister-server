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

package nu.fgv.register.server.task.category;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nu.fgv.register.server.impex.model.AbstractAuditableImpexDto;
import nu.fgv.register.server.impex.model.excel.ExcelCell;
import nu.fgv.register.server.impex.model.excel.ExcelSheet;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ExcelSheet(name = "taskCategory.impex.sheetName")
public class TaskCategoryImpexDto extends AbstractAuditableImpexDto<TaskCategoryImpexDto> {

    @JsonProperty("id")
    @ExcelCell(header = "taskCategory.impex.id.columnName", position = 1, primaryKey = true)
    private Long id;

    @NotBlank(message = "{taskCategory.name.notEmpty}")
    @Size(max = 255, message = "{taskCategory.name.maxSize}")
    @Column(name = "name", nullable = false)
    @JsonProperty("name")
    @ExcelCell(header = "taskCategory.impex.name.columnName", position = 2, updatable = true, mandatory = true)
    private String name;

    @JsonProperty("actorPresent")
    @ExcelCell(header = "taskCategory.impex.actorPresent.columnName", position = 3, updatable = true, mandatory = true)
    private Boolean actorPresent;

}
