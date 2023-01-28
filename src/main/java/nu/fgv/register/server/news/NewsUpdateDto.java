package nu.fgv.register.server.news;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDate;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@Builder
@Relation(collectionRelation = "news", itemRelation = "news")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsUpdateDto {
    @JsonProperty("id")
    private Long id;

    @NotBlank(message = "{news.subject.notEmpty}")
    @Size(max = 255, message = "{news.subject.size}")
    @JsonProperty("subject")
    private String subject;

    @NotBlank(message = "{news.text.notEmpty}")
    @JsonProperty("text")
    private String text;

    @NotNull(message = "{news.publicationDate.notEmpty}")
    @JsonProperty("publicationDate")
    private LocalDate publicationDate;

}
