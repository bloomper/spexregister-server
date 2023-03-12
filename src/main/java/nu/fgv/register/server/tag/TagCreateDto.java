package nu.fgv.register.server.tag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TagCreateDto {
    @NotBlank(message = "{tag.name.notEmpty}")
    @Size(max = 255, message = "{tag.name.maxSize}")
    @JsonProperty("name")
    private String name;

}
