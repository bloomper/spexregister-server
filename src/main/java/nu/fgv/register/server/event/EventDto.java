package nu.fgv.register.server.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.hateoas.server.core.Relation;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@Relation(collectionRelation = "events", itemRelation = "events")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventDto {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("event")
    private String event;

    @JsonProperty("source")
    private String source;

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("createdAt")
    private Instant createdAt;

    @Builder
    public EventDto(
            final Long id,
            final String event,
            final String source,
            final String createdBy,
            final Instant createdAt
    ) {
        this.id = id;
        this.event = event;
        this.source = source;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }
}
