package nu.fgv.register.server.spexare.toggle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.settings.TypeDto;
import nu.fgv.register.server.util.AbstractAuditableDto;
import org.springframework.hateoas.server.core.Relation;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
@Relation(collectionRelation = "toggles", itemRelation = "toggle")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ToggleDto extends AbstractAuditableDto<ToggleDto> {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("value")
    private Boolean value;

    @JsonProperty("type")
    private TypeDto type;

    @Builder
    public ToggleDto(
            final Long id,
            final Boolean value,
            final TypeDto type,
            final String createdBy,
            final Instant createdAt,
            final String lastModifiedBy,
            final Instant lastModifiedAt
    ) {
        super(createdBy, createdAt, lastModifiedBy, lastModifiedAt);
        this.id = id;
        this.value = value;
        this.type = type;
    }
}
