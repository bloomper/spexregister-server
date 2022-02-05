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
@Relation(collectionRelation = "spex", itemRelation = "spex")
public class SpexDto extends AbstractAuditableDto<SpexDto> {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("year")
    private String year;

    @JsonProperty("category")
    private SpexCategoryDto category;

    @JsonProperty("parent")
    private SpexDto parent;

    @JsonProperty("details")
    private SpexDetailsDto details;
}
