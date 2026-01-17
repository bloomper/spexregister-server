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
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
@ExcelSheet(name = "user.impex.sheetName")
public class UserImpexDto extends AbstractAuditableImpexDto<UserImpexDto> {

    @JsonProperty("id")
    @ExcelCell(header = "user.impex.id.columnName", position = 1, primaryKey = true)
    private Long id;

    @Nullable
    @JsonProperty("externalId")
    @ExcelCell(header = "user.impex.externalId.columnName", position = 2)
    private String externalId;

    @NotBlank(message = "{user.email.notEmpty}")
    @Size(max = 255, message = "{user.email.size}")
    @Email(message = "{user.email.valid}")
    @JsonProperty("email")
    @ExcelCell(header = "user.impex.email.columnName", position = 3)
    private String email;

    @NotBlank(message = "{user.state.notEmpty}")
    @JsonProperty("stateId")
    @ExcelCell(header = "user.impex.state.id.columnName", position = 4, updatable = true, mandatory = true)
    private String stateId;

    @Nullable
    @JsonProperty("stateLabel")
    @ExcelCell(header = "user.impex.state.label.columnName", position = 5)
    private String stateLabel;

    @Nullable
    @JsonProperty("spexareId")
    @ExcelCell(header = "user.impex.spexare.id.columnName", position = 6, updatable = true)
    private Long spexareId;

    @Nullable
    @JsonProperty("spexareFirstName")
    @ExcelCell(header = "user.impex.spexare.firstName.columnName", position = 7)
    private String spexareFirstName;

    @Nullable
    @JsonProperty("spexareLastName")
    @ExcelCell(header = "user.impex.spexare.lastName.columnName", position = 8)
    private String spexareLastName;

    @Nullable
    @JsonProperty("spexareNickName")
    @ExcelCell(header = "user.impex.spexare.nickName.columnName", position = 9)
    private String spexareNickName;

}
