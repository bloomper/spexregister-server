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

package nu.fgv.register.server.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.hateoas.server.core.Relation;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@Relation(collectionRelation = "countries", itemRelation = "country")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CountryDto {

    @JsonProperty("isoCode")
    private String isoCode;

    @JsonProperty("label")
    private String label;

    @Builder
    public CountryDto(
            final String isoCode,
            final String label
    ) {
        this.isoCode = isoCode;
        this.label = label;
    }
}
