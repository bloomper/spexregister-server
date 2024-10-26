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
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddressCreateDto {
    @Size(max = 255, message = "{address.streetAddress.size}")
    @JsonProperty("streetAddress")
    private String streetAddress;

    @Size(max = 255, message = "{address.postalCode.size}")
    @JsonProperty("postalCode")
    private String postalCode;

    @Size(max = 255, message = "{address.city.size}")
    @JsonProperty("city")
    private String city;

    @Size(max = 255, message = "{address.country.size}")
    @JsonProperty("country")
    private String country;

    @Size(max = 255, message = "{address.phone.size}")
    @JsonProperty("phone")
    private String phone;

    @Size(max = 255, message = "{address.phoneMobile.size}")
    @JsonProperty("phoneMobile")
    private String phoneMobile;

    @Size(max = 255, message = "{address.emailAddress.size}")
    @Email(message = "{address.emailAddress.valid}")
    @JsonProperty("emailAddress")
    private String emailAddress;

}
