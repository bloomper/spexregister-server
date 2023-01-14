package nu.fgv.register.server.spex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.util.impex.model.ExcelImportCell;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpexCategoryCreateDto {
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
