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
import nu.fgv.register.server.util.impex.model.ExcelImportCell;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskCategoryCreateDto {
    @NotBlank(message = "{taskCategory.name.notBlank}")
    @Size(max = 255, message = "{taskCategory.name.maxSize}")
    @JsonProperty("name")
    @ExcelImportCell(position = 1)
    private String name;

    @JsonProperty("hasActor")
    @ExcelImportCell(position = 2)
    private boolean hasActor;

}
