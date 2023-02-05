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
