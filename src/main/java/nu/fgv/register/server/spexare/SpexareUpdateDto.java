/*
 * Copyright 2024 the original author or authors.
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
import lombok.Builder;
import nu.fgv.register.server.util.impex.model.excel.ExcelImportCell;
import nu.fgv.register.server.util.validation.Luhn;

import static nu.fgv.register.server.spexare.Spexare.SOCIAL_SECURITY_NUMBER_PATTERN;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SpexareUpdateDto(
        @JsonProperty("id")
        @ExcelImportCell(position = 0, primaryKey = true)
        Long id,

        @NotEmpty(message = "{spexare.firstName.notEmpty}")
        @Size(max = 255, message = "{spexare.firstName.size}")
        @JsonProperty("firstName")
        String firstName,

        @NotEmpty(message = "{spexare.lastName.notEmpty}")
        @Size(max = 255, message = "{spexare.lastName.size}")
        @JsonProperty("lastName")
        String lastName,

        @Size(max = 255, message = "{spexare.nickName.size}")
        @JsonProperty("nickName")
        String nickName,

        @Pattern(regexp = SOCIAL_SECURITY_NUMBER_PATTERN, message = "{spexare.socialSecurityNumber.regexp}")
        @Luhn(regexp = SOCIAL_SECURITY_NUMBER_PATTERN, existenceGroup = 10, inputGroups = {2, 3, 6, 11}, controlGroup = 12, message = "{spexare.socialSecurityNumber.luhn}")
        @JsonProperty("socialSecurityNumber")
        String socialSecurityNumber,

        @JsonProperty("deceased")
        Boolean deceased,

        @JsonProperty("published")
        Boolean published,

        @Size(max = 255, message = "{spexare.graduation.size}")
        @JsonProperty("graduation")
        String graduation,

        @JsonProperty("comment")
        String comment
) {
}
