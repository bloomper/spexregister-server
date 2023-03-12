package nu.fgv.register.server.news;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.util.AbstractAuditableDto;
import org.springframework.hateoas.server.core.Relation;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
@Relation(collectionRelation = "news", itemRelation = "news")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsDto extends AbstractAuditableDto<NewsDto> {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("text")
    private String text;

    @JsonProperty("visibleFrom")
    private LocalDate visibleFrom;

    @JsonProperty("visibleTo")
    private LocalDate visibleTo;

    @JsonProperty("published")
    private Boolean published;

    @Builder
    public NewsDto(
            final Long id,
            final String subject,
            final String text,
            final LocalDate visibleFrom,
            final LocalDate visibleTo,
            final Boolean published,
            final String createdBy,
            final Instant createdAt,
            final String lastModifiedBy,
            final Instant lastModifiedAt
    ) {
        super(createdBy, createdAt, lastModifiedBy, lastModifiedAt);
        this.id = id;
        this.subject = subject;
        this.text = text;
        this.visibleFrom = visibleFrom;
        this.visibleTo = visibleTo;
        this.published = published;
    }
}
