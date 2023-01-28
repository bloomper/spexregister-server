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
import org.springframework.hateoas.server.core.Relation;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@Builder
@Relation(collectionRelation = "tasks", itemRelation = "task")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskUpdateDto {
    @JsonProperty("id")
    @ExcelImportCell(position = 0, primaryKey = true)
    private Long id;

    @NotBlank(message = "{task.name.notEmpty}")
    @Size(max = 255, message = "{task.name.size}")
    @JsonProperty("name")
    @ExcelImportCell(position = 1)
    private String name;

}
