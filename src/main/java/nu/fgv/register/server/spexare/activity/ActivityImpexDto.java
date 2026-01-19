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
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nu.fgv.register.server.impex.model.AbstractAuditableImpexDto;
import nu.fgv.register.server.impex.model.ImpexAction;
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
@ExcelSheet(name = "activity.impex.sheetName")
public class ActivityImpexDto extends AbstractAuditableImpexDto<ActivityImpexDto> {

    @NotEmpty(message = "{activity.spexare.notEmpty}")
    @JsonProperty("spexareId")
    @ExcelCell(header = "activity.impex.spexareId.columnName", position = 1)
    private Long spexareId;

    @JsonProperty("id")
    @ExcelCell(header = "activity.impex.id.columnName", position = 2, primaryKey = true)
    private Long id;

    @JsonProperty("spexActivityAction")
    @ExcelCell(header = "common.impex.action.columnName", position = 3)
    private ImpexAction spexActivityAction;

    @NotEmpty(message = "{activity.spexActivity.notEmpty}")
    @JsonProperty("spexActivityId")
    @ExcelCell(header = "activity.impex.spexActivityId.columnName", position = 4)
    private Long spexActivityId;

    @NotEmpty(message = "{activity.spex.notEmpty}")
    @JsonProperty("spexId")
    @ExcelCell(header = "activity.impex.spexId.columnName", position = 5, updatable = true)
    private Long spexId;

    @Nullable
    @JsonProperty("spexYear")
    @ExcelCell(header = "activity.impex.spexYear.columnName", position = 6)
    private String spexYear;

    @Nullable
    @JsonProperty("spexTitle")
    @ExcelCell(header = "activity.impex.spexTitle.columnName", position = 7)
    private String spexTitle;

    @Nullable
    @JsonProperty("spexRevival")
    @ExcelCell(header = "activity.impex.spexRevival.columnName", position = 8)
    private Boolean spexRevival;

    @Nullable
    @JsonProperty("spexCategoryName")
    @ExcelCell(header = "activity.impex.spexCategoryName.columnName", position = 9)
    private String spexCategoryName;

    @JsonProperty("taskActivityAction")
    @ExcelCell(header = "common.impex.action.columnName", position = 10)
    private ImpexAction taskActivityAction;

    @NotEmpty(message = "{activity.taskActivity.notEmpty}")
    @JsonProperty("taskActivityId")
    @ExcelCell(header = "activity.impex.taskActivityId.columnName", position = 11)
    private Long taskActivityId;

    @NotEmpty(message = "{activity.task.notEmpty}")
    @JsonProperty("taskId")
    @ExcelCell(header = "activity.impex.taskId.columnName", position = 12, updatable = true)
    private Long taskId;

    @Nullable
    @JsonProperty("taskName")
    @ExcelCell(header = "activity.impex.taskName.columnName", position = 13)
    private String taskName;

    @Nullable
    @JsonProperty("taskCategoryName")
    @ExcelCell(header = "activity.impex.taskCategoryName.columnName", position = 14)
    private String taskCategoryName;

    @Nullable
    @JsonProperty("actorAction")
    @ExcelCell(header = "common.impex.action.columnName", position = 15)
    private ImpexAction actorAction;

    @Nullable
    @JsonProperty("actorId")
    @ExcelCell(header = "activity.impex.actorId.columnName", position = 16)
    private Long actorId;

    @Nullable
    @JsonProperty("actorRole")
    @ExcelCell(header = "activity.impex.actorRole.columnName", position = 17, updatable = true)
    private String actorRole;

    @Nullable
    @NotEmpty(message = "{activity.type.notEmpty}")
    @JsonProperty("typeId")
    @ExcelCell(header = "common.impex.type.id.columnName", position = 18, updatable = true)
    private String typeId;

    @Nullable
    @JsonProperty("typeLabel")
    @ExcelCell(header = "common.impex.type.label.columnName", position = 19)
    private String typeLabel;
}
