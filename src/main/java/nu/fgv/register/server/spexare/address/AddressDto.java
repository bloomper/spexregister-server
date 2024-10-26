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

package nu.fgv.register.server.spexare.address;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.settings.TypeDto;
import nu.fgv.register.server.util.AbstractAuditableDto;
import org.springframework.hateoas.server.core.Relation;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
@Relation(collectionRelation = "addresses", itemRelation = "address")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddressDto extends AbstractAuditableDto<AddressDto> {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("streetAddress")
    private String streetAddress;

    @JsonProperty("postalCode")
    private String postalCode;

    @JsonProperty("city")
    private String city;

    @JsonProperty("country")
    private String country;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("phoneMobile")
    private String phoneMobile;

    @JsonProperty("emailAddress")
    private String emailAddress;

    @JsonProperty("type")
    private TypeDto type;

    @Builder
    public AddressDto(
            final Long id,
            final String streetAddress,
            final String postalCode,
            final String city,
            final String country,
            final String phone,
            final String phoneMobile,
            final String emailAddress,
            final TypeDto type,
            final String createdBy,
            final Instant createdAt,
            final String lastModifiedBy,
            final Instant lastModifiedAt
    ) {
        super(createdBy, createdAt, lastModifiedBy, lastModifiedAt);
        this.id = id;
        this.streetAddress = streetAddress;
        this.postalCode = postalCode;
        this.city = city;
        this.country = country;
        this.phone = phone;
        this.phoneMobile = phoneMobile;
        this.emailAddress = emailAddress;
        this.type = type;
    }
}
