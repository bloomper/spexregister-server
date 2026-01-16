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

package nu.fgv.register.server.spexare.address;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
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
@JsonIgnoreProperties(ignoreUnknown = true)
@ExcelSheet(name = "address.impex.sheetName")
public class AddressImpexDto extends AbstractAuditableImpexDto<AddressImpexDto> {

    @NotEmpty(message = "{address.spexare.notEmpty}")
    @JsonProperty("spexareId")
    @ExcelCell(header = "address.impex.spexareId.columnName", position = 1)
    private Long spexareId;

    @JsonProperty("id")
    @ExcelCell(header = "address.impex.id.columnName", position = 2, primaryKey = true)
    private Long id;

    @JsonProperty("streetAddress")
    @ExcelCell(header = "address.impex.streetAddress.columnName", position = 3, updatable = true)
    private String streetAddress;

    @JsonProperty("postalCode")
    @ExcelCell(header = "address.impex.postalCode.columnName", position = 4, updatable = true)
    private String postalCode;

    @JsonProperty("city")
    @ExcelCell(header = "address.impex.city.columnName", position = 5, updatable = true)
    private String city;

    @JsonProperty("country")
    @ExcelCell(header = "address.impex.country.columnName", position = 6, updatable = true)
    private String country;

    @JsonProperty("countryName")
    @ExcelCell(header = "address.impex.countryName.columnName", position = 7)
    private String countryName;

    @JsonProperty("phone")
    @ExcelCell(header = "address.impex.phone.columnName", position = 8, updatable = true)
    private String phone;

    @JsonProperty("phoneMobile")
    @ExcelCell(header = "address.impex.phoneMobile.columnName", position = 9, updatable = true)
    private String phoneMobile;

    @JsonProperty("emailAddress")
    @ExcelCell(header = "address.impex.emailAddress.columnName", position = 10, updatable = true)
    private String emailAddress;

    @JsonProperty("typeId")
    @ExcelCell(header = "common.impex.type.id.columnName", position = 11, updatable = true, mandatory = true)
    private String typeId;

    @JsonProperty("typeLabel")
    @ExcelCell(header = "common.impex.type.label.columnName", position = 12)
    private String typeLabel;

}
