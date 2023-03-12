package nu.fgv.register.server.tag;

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
import org.springframework.hateoas.server.core.Relation;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Relation(collectionRelation = "tags", itemRelation = "tag")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TagUpdateDto {
    @JsonProperty("id")
    private Long id;

    @NotBlank(message = "{tag.name.notEmpty}")
    @Size(max = 255, message = "{tag.name.maxSize}")
    @JsonProperty("name")
    private String name;

}
