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
public class SpexCreateDto {
    @NotBlank(message = "{spex.year.notEmpty}")
    @Size(max = 4, message = "{spex.year.size}")
    @Pattern(regexp = "^(19|20|21)\\d{2}$", message = "{spex.year.regexp}")
    @JsonProperty("year")
    @ExcelImportCell(position = 1)
    private String year;

    @NotBlank(message = "{spex.title.notEmpty}")
    @Size(max = 255, message = "{spex.title.size}")
    @JsonProperty("title")
    @ExcelImportCell(position = 2)
    private String title;

}
