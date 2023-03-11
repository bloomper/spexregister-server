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
