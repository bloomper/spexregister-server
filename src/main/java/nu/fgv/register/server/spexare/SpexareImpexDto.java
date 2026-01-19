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

package nu.fgv.register.server.spexare;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nu.fgv.register.server.impex.model.AbstractAuditableImpexDto;
import nu.fgv.register.server.impex.model.excel.ExcelCell;
import nu.fgv.register.server.impex.model.excel.ExcelSheet;
import nu.fgv.register.server.util.validation.Luhn;
import org.jspecify.annotations.Nullable;

import static nu.fgv.register.server.spexare.Spexare.SOCIAL_SECURITY_NUMBER_PATTERN;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ExcelSheet(name = "spexare.impex.sheetName")
public class SpexareImpexDto extends AbstractAuditableImpexDto<SpexareImpexDto> {

    @JsonProperty("id")
    @ExcelCell(header = "spexare.impex.id.columnName", position = 1, primaryKey = true)
    private Long id;

    @NotEmpty(message = "{spexare.firstName.notEmpty}")
    @Size(max = 255, message = "{spexare.firstName.size}")
    @JsonProperty("firstName")
    @ExcelCell(header = "spexare.impex.firstName.columnName", position = 2, updatable = true, mandatory = true)
    private String firstName;

    @NotEmpty(message = "{spexare.lastName.notEmpty}")
    @Size(max = 255, message = "{spexare.lastName.size}")
    @JsonProperty("lastName")
    @ExcelCell(header = "spexare.impex.lastName.columnName", position = 3, updatable = true, mandatory = true)
    private String lastName;

    @Nullable
    @Size(max = 255, message = "{spexare.nickName.size}")
    @JsonProperty("nickName")
    @ExcelCell(header = "spexare.impex.nickName.columnName", position = 4, updatable = true)
    private String nickName;

    @Nullable
    @Pattern(regexp = SOCIAL_SECURITY_NUMBER_PATTERN, message = "{spexare.socialSecurityNumber.regexp}")
    @Luhn(regexp = SOCIAL_SECURITY_NUMBER_PATTERN, existenceGroup = 10, inputGroups = {2, 3, 6, 11}, controlGroup = 12, message = "{spexare.socialSecurityNumber.luhn}")
    @JsonProperty("socialSecurityNumber")
    @ExcelCell(header = "spexare.impex.socialSecurityNumber.columnName", position = 5, updatable = true)
    private String socialSecurityNumber;

    @JsonProperty("deceased")
    @ExcelCell(header = "spexare.impex.deceased.columnName", position = 6, updatable = true)
    private Boolean deceased;

    @JsonProperty("published")
    @ExcelCell(header = "spexare.impex.published.columnName", position = 7, updatable = true)
    private Boolean published;

    @Nullable
    @Size(max = 255, message = "{spexare.graduation.size}")
    @JsonProperty("graduation")
    @ExcelCell(header = "spexare.impex.graduation.columnName", position = 8, updatable = true)
    private String graduation;

    @Nullable
    @Size(max = 10000, message = "{spexare.comment.size}")
    @JsonProperty("comment")
    @ExcelCell(header = "spexare.impex.comment.columnName", position = 9, updatable = true)
    private String comment;

    @Nullable
    @JsonProperty("image")
    @ExcelCell(header = "spexare.impex.imageUrl.columnName", position = 10, updatable = true)
    private String imageUrl;

    @Nullable
    @JsonProperty("partnerId")
    @ExcelCell(header = "spexare.impex.partner.id.columnName", position = 11, updatable = true)
    private Long partnerId;

    @Nullable
    @JsonProperty("partnerFirstName")
    @ExcelCell(header = "spexare.impex.partner.firstName.columnName", position = 12)
    private String partnerFirstName;

    @Nullable
    @JsonProperty("partnerLastName")
    @ExcelCell(header = "spexare.impex.partner.lastName.columnName", position = 13)
    private String partnerLastName;

    @Nullable
    @JsonProperty("partnerNickName")
    @ExcelCell(header = "spexare.impex.partner.nickName.columnName", position = 14)
    private String partnerNickName;

}
