package nu.fgv.register.server.spexare.membership;

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
@Relation(collectionRelation = "memberships", itemRelation = "membership")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MembershipDto extends AbstractAuditableDto<MembershipDto> {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("year")
    private String year;

    @JsonProperty("type")
    private TypeDto type;

    @Builder
    public MembershipDto(
            final Long id,
            final String year,
            final TypeDto type,
            final String createdBy,
            final Instant createdAt,
            final String lastModifiedBy,
            final Instant lastModifiedAt
    ) {
        super(createdBy, createdAt, lastModifiedBy, lastModifiedAt);
        this.id = id;
        this.year = year;
        this.type = type;
    }
}
