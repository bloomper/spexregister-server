package nu.fgv.register.server.spexare;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.util.AbstractAuditableDto;
import nu.fgv.register.server.util.impex.model.ExcelCell;
import nu.fgv.register.server.util.impex.model.ExcelSheet;
import org.springframework.hateoas.server.core.Relation;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
@Relation(collectionRelation = "spexare", itemRelation = "spexare")
@JsonIgnoreProperties(ignoreUnknown = true)
@ExcelSheet(name = "Spexare")
public class SpexareDto extends AbstractAuditableDto<SpexareDto> {
    @JsonProperty("id")
    @ExcelCell(header = "Id", position = 0)
    private Long id;

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("nickName")
    private String nickName;

    @JsonProperty("birthDate")
    private LocalDate birthDate;

    @JsonProperty("socialSecurityNumber")
    private String socialSecurityNumber;

    @JsonProperty("graduation")
    private String graduation;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("image")
    private String image;

    @Builder
    public SpexareDto(
            final Long id,
            final String firstName,
            final String lastName,
            final String nickName,
            final LocalDate birthDate,
            final String socialSecurityNumber,
            final String graduation,
            final String comment,
            final String createdBy,
            final Instant createdAt,
            final String lastModifiedBy,
            final Instant lastModifiedAt
    ) {
        super(createdBy, createdAt, lastModifiedBy, lastModifiedAt);
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.nickName = nickName;
        this.birthDate = birthDate;
        this.socialSecurityNumber = socialSecurityNumber;
        this.graduation = graduation;
        this.comment = comment;
    }
}
