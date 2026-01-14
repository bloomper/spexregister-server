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

package nu.fgv.register.server.spexare.activity;

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
@ExcelSheet(name = "activity.impex.sheetName")
public class ActivityImpexDto extends AbstractAuditableDto<ActivityImpexDto> {
    @JsonProperty("spexareId")
    @ExcelCell(header = "activity.impex.spexareId.columnName", position = 0)
    private Long spexareId;

    @JsonProperty("id")
    @ExcelCell(header = "activity.impex.id.columnName", position = 1)
    private Long id;

    @JsonProperty("spexActivityId")
    @ExcelCell(header = "activity.impex.spexActivityId.columnName", position = 2)
    private Long spexActivityId;

    @JsonProperty("spexId")
    @ExcelCell(header = "activity.impex.spexId.columnName", position = 3, updatable = true)
    private Long spexId;

    @JsonProperty("spexYear")
    @ExcelCell(header = "activity.impex.spexYear.columnName", position = 4)
    private String spexYear;

    @JsonProperty("spexTitle")
    @ExcelCell(header = "activity.impex.spexTitle.columnName", position = 5)
    private String spexTitle;

    @JsonProperty("spexRevival")
    @ExcelCell(header = "activity.impex.spexRevival.columnName", position = 6)
    private Boolean spexRevival;

    @JsonProperty("spexCategoryName")
    @ExcelCell(header = "activity.impex.spexCategoryName.columnName", position = 7)
    private String spexCategoryName;

    @JsonProperty("taskActivityId")
    @ExcelCell(header = "activity.impex.taskActivityId.columnName", position = 8)
    private Long taskActivityId;

    @JsonProperty("taskId")
    @ExcelCell(header = "activity.impex.taskId.columnName", position = 9, updatable = true)
    private Long taskId;

    @JsonProperty("taskName")
    @ExcelCell(header = "activity.impex.taskName.columnName", position = 10)
    private String taskName;

    @JsonProperty("taskCategoryName")
    @ExcelCell(header = "activity.impex.taskCategoryName.columnName", position = 11)
    private String taskCategoryName;

    @JsonProperty("actorId")
    @ExcelCell(header = "activity.impex.actorId.columnName", position = 12)
    private Long actorId;

    @JsonProperty("actorRole")
    @ExcelCell(header = "activity.impex.actorRole.columnName", position = 13, updatable = true)
    private String actorRole;

    @JsonProperty("typeId")
    @ExcelCell(header = "common.impex.type.id.columnName", position = 14, updatable = true)
    private String typeId;

    @JsonProperty("typeLabel")
    @ExcelCell(header = "common.impex.type.label.columnName", position = 15)
    private String typeLabel;
}
