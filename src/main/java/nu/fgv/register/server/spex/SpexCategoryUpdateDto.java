package nu.fgv.register.server.spex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.util.impex.model.ExcelImportCell;
import org.springframework.hateoas.server.core.Relation;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Relation(collectionRelation = "spexCategories", itemRelation = "spexCategory")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpexCategoryUpdateDto {
    @JsonProperty("id")
    @ExcelImportCell(position = 0, primaryKey = true)
    private Long id;

    @NotBlank(message = "{spexCategory.name.notBlank}")
    @Size(max = 255, message = "{spexCategory.name.maxSize}")
    @JsonProperty("name")
    @ExcelImportCell(position = 1)
    private String name;

    @NotBlank(message = "{spexCategory.firstYear.notBlank}")
    @Size(max = 4, message = "{spexCategory.firstYear.maxSize}")
    @Pattern(regexp = "^(19|20|21)\\d{2}$", message = "{spexCategory.firstYear.regexp}")
    @JsonProperty("firstYear")
    @ExcelImportCell(position = 2)
    private String firstYear;

}
