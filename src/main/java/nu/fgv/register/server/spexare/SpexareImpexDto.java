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
@ExcelSheet(name = "spexare.impex.sheetName")
public class SpexareImpexDto extends AbstractAuditableDto<SpexareImpexDto> {
    @JsonProperty("id")
    @ExcelCell(header = "spexare.impex.id.columnName", position = 0)
    private Long id;

    @JsonProperty("firstName")
    @ExcelCell(header = "spexare.impex.firstName.columnName", position = 1, updatable = true, mandatory = true)
    private String firstName;

    @JsonProperty("lastName")
    @ExcelCell(header = "spexare.impex.lastName.columnName", position = 2, updatable = true, mandatory = true)
    private String lastName;

    @JsonProperty("nickName")
    @ExcelCell(header = "spexare.impex.nickName.columnName", position = 3, updatable = true)
    private String nickName;

    @JsonProperty("socialSecurityNumber")
    @ExcelCell(header = "spexare.impex.socialSecurityNumber.columnName", position = 4, updatable = true)
    private String socialSecurityNumber;

    @JsonProperty("deceased")
    @ExcelCell(header = "spexare.impex.deceased.columnName", position = 5, updatable = true)
    private Boolean deceased;

    @JsonProperty("published")
    @ExcelCell(header = "spexare.impex.published.columnName", position = 6, updatable = true)
    private Boolean published;

    @JsonProperty("graduation")
    @ExcelCell(header = "spexare.impex.graduation.columnName", position = 7, updatable = true)
    private String graduation;

    @JsonProperty("comment")
    @ExcelCell(header = "spexare.impex.comment.columnName", position = 8, updatable = true)
    private String comment;

    @JsonProperty("image")
    @ExcelCell(header = "spexare.impex.imageUrl.columnName", position = 9, updatable = true)
    private String imageUrl;

    @JsonProperty("partnerId")
    @ExcelCell(header = "spexare.impex.partner.id.columnName", position = 10, updatable = true)
    private Long partnerId;

    @JsonProperty("partnerFirstName")
    @ExcelCell(header = "spexare.impex.partner.firstName.columnName", position = 11)
    private String partnerFirstName;

    @JsonProperty("partnerLastName")
    @ExcelCell(header = "spexare.impex.partner.lastName.columnName", position = 12)
    private String partnerLastName;

    @JsonProperty("partnerNickName")
    @ExcelCell(header = "spexare.impex.partner.nickName.columnName", position = 13)
    private String partnerNickName;

}
