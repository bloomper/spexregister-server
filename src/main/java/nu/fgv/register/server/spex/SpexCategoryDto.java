package nu.fgv.register.server.spex;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.util.AbstractAuditableDto;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
public class SpexCategoryDto extends AbstractAuditableDto<SpexCategoryDto> {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("firstYear")
    private String firstYear;

    //@JsonProperty("logoUrl")
    // private String logoUrl;

}
