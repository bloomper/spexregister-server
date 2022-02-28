package nu.fgv.register.server.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public abstract class AbstractAuditableDto<T extends RepresentationModel<? extends T>> extends RepresentationModel<T> {

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("lastModifiedBy")
    private String lastModifiedBy;

    @JsonProperty("lastModifiedAt")
    private Instant lastModifiedAt;

}
