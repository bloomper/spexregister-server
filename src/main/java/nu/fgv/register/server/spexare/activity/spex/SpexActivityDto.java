package nu.fgv.register.server.spexare.activity.spex;

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
@Relation(collectionRelation = "spex-activities", itemRelation = "spex-activity")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpexActivityDto extends AbstractAuditableDto<SpexActivityDto> {
    @JsonProperty("id")
    private Long id;

    @Builder
    public SpexActivityDto(
            final Long id,
            final String createdBy,
            final Instant createdAt,
            final String lastModifiedBy,
            final Instant lastModifiedAt
    ) {
        super(createdBy, createdAt, lastModifiedBy, lastModifiedAt);
        this.id = id;
    }
}
