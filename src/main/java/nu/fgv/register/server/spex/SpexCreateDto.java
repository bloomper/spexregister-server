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
public class SpexCreateDto {
    @NotBlank(message = "{spex.year.notBlank}")
    @Size(max = 4, message = "{spex.year.size}")
    @Pattern(regexp = "^(19|20|21)\\d{2}$", message = "{spex.year.pattern}")
    @JsonProperty("year")
    private String year;

    @NotBlank(message = "{spex.title.notBlank}")
    @Size(max = 255, message = "{spex.title.size}")
    @JsonProperty("title")
    private String title;

}
