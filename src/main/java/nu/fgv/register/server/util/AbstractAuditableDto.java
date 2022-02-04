package nu.fgv.register.server.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.hateoas.RepresentationModel;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import java.time.Instant;

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
