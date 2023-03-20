package nu.fgv.register.server.news;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@Builder
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

    @JsonProperty("visibleFrom")
    private LocalDate visibleFrom;

    @JsonProperty("visibleTo")
    private LocalDate visibleTo;

}
