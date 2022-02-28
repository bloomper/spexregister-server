package nu.fgv.register.server.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import nu.fgv.register.server.util.export.model.ExcelCell;
import org.springframework.hateoas.RepresentationModel;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public abstract class AbstractAuditableDto<T extends RepresentationModel<? extends T>> extends RepresentationModel<T> {

    @JsonProperty("createdBy")
    @ExcelCell(header = "Created by", position = 0)
    private String createdBy;

    @JsonProperty("createdAt")
    @ExcelCell(header = "Created at", position = 1)
    private Instant createdAt;

    @JsonProperty("lastModifiedBy")
    @ExcelCell(header = "Last modified by", position = 2)
    private String lastModifiedBy;

    @JsonProperty("lastModifiedAt")
    @ExcelCell(header = "Last modified at", position = 3)
    private Instant lastModifiedAt;

}
