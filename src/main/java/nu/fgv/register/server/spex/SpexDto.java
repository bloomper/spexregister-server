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

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
@Relation(collectionRelation = "spex", itemRelation = "spex")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpexDto extends AbstractAuditableDto<SpexDto> {
    @JsonProperty("id")
    private Long id;

    @NotBlank(message = "{spex.year.notBlank}")
    @Size(max = 4, message = "{spex.year.size}")
    @Pattern(regexp = "^(19|20|21)\\d{2}$", message = "{spex.year.pattern}")
    @JsonProperty("year")
    private String year;

    @NotBlank(message = "{spex.title.notBlank}")
    @Size(max = 255, message = "{spex.title.size}")
    @JsonProperty("title")
    private String title;

    @JsonProperty("poster")
    private String poster;

    @NotNull(message = "{spex.category.notBlank}")
    @JsonProperty("category")
    private SpexCategoryDto category;

    @JsonProperty("parent")
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
