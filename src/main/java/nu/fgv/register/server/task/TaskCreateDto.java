package nu.fgv.register.server.task;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import nu.fgv.register.server.util.impex.model.ExcelImportCell;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Jacksonized // Needed due to this class having only one attribute
public class TaskCreateDto {

    @NotBlank(message = "{task.name.notEmpty}")
    @Size(max = 255, message = "{task.name.size}")
    @JsonProperty("name")
    @ExcelImportCell(position = 1)
    private String name;

}
