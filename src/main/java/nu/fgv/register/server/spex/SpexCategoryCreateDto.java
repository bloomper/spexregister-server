package nu.fgv.register.server.spex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

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
    private String name;

    @NotBlank(message = "{spexCategory.firstYear.notBlank}")
    @Size(max = 4, message = "{spexCategory.firstYear.maxSize}")
    @Pattern(regexp = "^(19|20|21)\\d{2}$", message = "{spexCategory.firstYear.pattern}")
    @JsonProperty("firstYear")
    private String firstYear;

}