package nu.fgv.register.server.settings;

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
@Relation(collectionRelation = "types", itemRelation = "type")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TypeDto extends AbstractAuditableDto<TypeDto> {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("value")
    private String value;

    @JsonProperty("label")
    private String label;

    @JsonProperty("type")
    private TypeType type;

    @Builder
    public TypeDto(
            final Long id,
            final String value,
            final String label,
            final TypeType type,
            final String createdBy,
            final Instant createdAt,
            final String lastModifiedBy,
            final Instant lastModifiedAt
    ) {
        super(createdBy, createdAt, lastModifiedBy, lastModifiedAt);
        this.id = id;
        this.value = value;
        this.label = label;
        this.type = type;
    }
}
