package nu.fgv.register.server.tag;

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
@Relation(collectionRelation = "tags", itemRelation = "tag")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TagDto extends AbstractAuditableDto<TagDto> {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @Builder
    public TagDto(
            final Long id,
            final String name,
            final String createdBy,
            final Instant createdAt,
            final String lastModifiedBy,
            final Instant lastModifiedAt
    ) {
        super(createdBy, createdAt, lastModifiedBy, lastModifiedAt);
        this.id = id;
        this.name = name;
    }
}
