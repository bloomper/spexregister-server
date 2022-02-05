package nu.fgv.register.server.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public abstract class AbstractAuditableDto<T extends RepresentationModel<? extends T>> extends RepresentationModel<T> {

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("createdDate")
    private long createdDate;

    @JsonProperty("lastModifiedBy")
    private String lastModifiedBy;

    @JsonProperty("lastModifiedDate")
    private long lastModifiedDate;

}
