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

package nu.fgv.register.server.spex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SpexUpdateDto(
        @JsonProperty("id")
        Long id,

        @NotBlank(message = "{spex.year.notEmpty}")
        @Size(max = 4, message = "{spex.year.size}")
        @Pattern(regexp = "^(19|20|21)\\d{2}$", message = "{spex.year.regexp}")
        @JsonProperty("year")
        String year,

        @NotBlank(message = "{spex.title.notEmpty}")
        @Size(max = 255, message = "{spex.title.size}")
        @JsonProperty("title")
        String title
) {
}
