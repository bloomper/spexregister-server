package nu.fgv.register.server.spex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.util.AbstractAuditableDto;
import org.springframework.hateoas.server.core.Relation;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
@Relation(collectionRelation = "spexCategories", itemRelation = "spexCategory")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpexCategoryDto extends AbstractAuditableDto<SpexCategoryDto> {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("firstYear")
    private String firstYear;

    @JsonProperty("logo")
    private String logo;

    @Builder
    public SpexCategoryDto(
            final Long id,
            final String name,
            final String firstYear,
            final String logo,
            final String createdBy,
            final Instant createdAt,
            final String lastModifiedBy,
            final Instant lastModifiedAt
    ) {
        super(createdBy, createdAt, lastModifiedBy, lastModifiedAt);
        this.id = id;
        this.name = name;
        this.firstYear = firstYear;
        this.logo = logo;
    }
}
