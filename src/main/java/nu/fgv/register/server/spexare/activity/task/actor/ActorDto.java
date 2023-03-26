package nu.fgv.register.server.spexare.activity.task.actor;

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
@Relation(collectionRelation = "actors", itemRelation = "actor")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActorDto extends AbstractAuditableDto<ActorDto> {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("role")
    private String role;

    @JsonProperty("vocal")
    private TypeDto vocal;

    @Builder
    public ActorDto(
            final Long id,
            final String role,
            final TypeDto vocal,
            final String createdBy,
            final Instant createdAt,
            final String lastModifiedBy,
            final Instant lastModifiedAt
    ) {
        super(createdBy, createdAt, lastModifiedBy, lastModifiedAt);
        this.id = id;
        this.role = role;
        this.vocal = vocal;
    }
}
