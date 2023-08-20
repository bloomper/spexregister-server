package nu.fgv.register.server.user;

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
@Relation(collectionRelation = "users", itemRelation = "users")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDto extends AbstractAuditableDto<UserDto> {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("username")
    private String username;

    @JsonProperty("state")
    private String state;

    @JsonProperty("federated")
    private boolean federated;

    @Builder
    public UserDto(
            final Long id,
            final String username,
            final User.State state,
            final boolean federated,
            final String createdBy,
            final Instant createdAt,
            final String lastModifiedBy,
            final Instant lastModifiedAt
    ) {
        super(createdBy, createdAt, lastModifiedBy, lastModifiedAt);
        this.id = id;
        this.username = username;
        this.state = state.toString();
        this.federated = federated;
    }
}
