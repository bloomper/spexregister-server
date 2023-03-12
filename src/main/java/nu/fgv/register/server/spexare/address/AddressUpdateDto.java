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
import org.springframework.hateoas.server.core.Relation;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@Builder
@Relation(collectionRelation = "addresses", itemRelation = "address")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddressUpdateDto {
    @JsonProperty("id")
    private Long id;

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
