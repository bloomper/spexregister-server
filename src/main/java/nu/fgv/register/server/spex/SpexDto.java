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
@Relation(collectionRelation = "spex", itemRelation = "spex")
@JsonIgnoreProperties(ignoreUnknown = true)
@ExcelSheet(name = "Spex")
public class SpexDto extends AbstractAuditableDto<SpexDto> {
    @JsonProperty("id")
    @ExcelCell(header = "Id", position = 0)
    private Long id;

    @JsonProperty("year")
    @ExcelCell(header = "Year", position = 1)
    private String year;

    @JsonProperty("title")
    @ExcelCell(header = "Title", position = 2)
    private String title;

    @JsonProperty("poster")
    @ExcelCell(header = "Poster", position = 3)
    private String poster;

    @JsonProperty("category")
    @ExcelCell(header = "Spex category", position = 4, transform = "#this.id")
    private SpexCategoryDto category;

    @JsonProperty("parent")
    @ExcelCell(header = "Parent", position = 5, transform = "#this.id")
    private SpexDto parent;

    @JsonProperty("revival")
    private boolean revival;

    @Builder
    public SpexDto(
            final Long id,
            final String year,
            final String title,
            final String poster,
            final SpexCategoryDto category,
            final SpexDto parent,
            final boolean revival,
            final String createdBy,
            final Instant createdAt,
            final String lastModifiedBy,
            final Instant lastModifiedAt
    ) {
        super(createdBy, createdAt, lastModifiedBy, lastModifiedAt);
        this.id = id;
        this.year = year;
        this.title = title;
        this.poster = poster;
        this.category = category;
        this.parent = parent;
        this.revival = revival;
    }
}
