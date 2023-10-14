package nu.fgv.register.server.user.state;

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
@Relation(collectionRelation = "states", itemRelation = "states")
@JsonIgnoreProperties(ignoreUnknown = true)
public class StateDto extends AbstractAuditableDto<StateDto> {

    @JsonProperty("id")
    private String id;

    @JsonProperty("label")
    private String label;

    @Builder
    public StateDto(
            final String id,
            final String label,
            final String createdBy,
            final Instant createdAt,
            final String lastModifiedBy,
            final Instant lastModifiedAt
    ) {
        super(createdBy, createdAt, lastModifiedBy, lastModifiedAt);
        this.id = id;
        this.label = label;
    }
}
