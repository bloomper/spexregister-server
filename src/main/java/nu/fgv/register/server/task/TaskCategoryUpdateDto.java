package nu.fgv.register.server.task;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.util.impex.model.ExcelImportCell;
import org.springframework.hateoas.server.core.Relation;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Relation(collectionRelation = "taskCategories", itemRelation = "taskCategory")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskCategoryUpdateDto {
    @JsonProperty("id")
    @ExcelImportCell(position = 0, primaryKey = true)
    private Long id;

    @NotBlank(message = "{taskCategory.name.notEmpty}")
    @Size(max = 255, message = "{taskCategory.name.maxSize}")
    @JsonProperty("name")
    @ExcelImportCell(position = 1)
    private String name;

    @JsonProperty("hasActor")
    @ExcelImportCell(position = 2)
    private boolean hasActor;

}
