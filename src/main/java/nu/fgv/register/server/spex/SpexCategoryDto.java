package nu.fgv.register.server.spex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.util.AbstractAuditableDto;
import nu.fgv.register.server.util.impex.model.ExcelCell;
import nu.fgv.register.server.util.impex.model.ExcelSheet;
import org.springframework.hateoas.server.core.Relation;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
@Relation(collectionRelation = "spexCategories", itemRelation = "spexCategory")
@JsonIgnoreProperties(ignoreUnknown = true)
@ExcelSheet(name = "Spex categories")
public class SpexCategoryDto extends AbstractAuditableDto<SpexCategoryDto> {
    @JsonProperty("id")
    @ExcelCell(header = "Id", position = 0)
    private Long id;

    @JsonProperty("name")
    @ExcelCell(header = "Name", position = 1)
    private String name;

    @JsonProperty("firstYear")
    @ExcelCell(header = "First year", position = 2)
    private String firstYear;

    @JsonProperty("logo")
    @ExcelCell(header = "Logo", position = 3)
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
