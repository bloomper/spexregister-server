package nu.fgv.register.server.spex;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.util.AbstractAuditableDto;
import org.springframework.hateoas.server.core.Relation;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
@Builder
@Relation(collectionRelation = "spexCategories", itemRelation = "spexCategory")
public class SpexCategoryDto extends AbstractAuditableDto<SpexCategoryDto> {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("firstYear")
    private String firstYear;

    @JsonProperty("logo")
    private String logo;

}
