package nu.fgv.register.server.task.category;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.util.AbstractAuditableDto;
import nu.fgv.register.server.util.impex.model.ExcelCell;
import nu.fgv.register.server.util.impex.model.ExcelSheet;
import org.springframework.hateoas.server.core.Relation;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
@Relation(collectionRelation = "task-categories", itemRelation = "task-category")
@JsonIgnoreProperties(ignoreUnknown = true)
@ExcelSheet(name = "Task categories")
public class TaskCategoryDto extends AbstractAuditableDto<TaskCategoryDto> {
    @JsonProperty("id")
    @ExcelCell(header = "Id", position = 0)
    private Long id;

    @JsonProperty("name")
    @ExcelCell(header = "Name", position = 1, updatable = true, mandatory = true)
    private String name;

    @JsonProperty("hasActor")
    @ExcelCell(header = "Has actor", position = 2, updatable = true, mandatory = true)
    private boolean hasActor;

    @Builder
    public TaskCategoryDto(
            final Long id,
            final String name,
            final boolean hasActor,
            final String createdBy,
            final Instant createdAt,
            final String lastModifiedBy,
            final Instant lastModifiedAt
    ) {
        super(createdBy, createdAt, lastModifiedBy, lastModifiedAt);
        this.id = id;
        this.name = name;
        this.hasActor = hasActor;
    }
}
