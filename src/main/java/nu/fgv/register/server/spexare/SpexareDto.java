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

    @JsonProperty("streetAddress")
    private String streetAddress;

    @JsonProperty("postalCode")
    private String postalCode;

    @JsonProperty("postalAddress")
    private String postalAddress;

    @JsonProperty("country")
    private String country;

    @JsonProperty("phoneHome")
    private String phoneHome;

    @JsonProperty("phoneWork")
    private String phoneWork;

    @JsonProperty("phoneMobile")
    private String phoneMobile;

    @JsonProperty("phoneOther")
    private String phoneOther;

    @JsonProperty("emailAddress")
    private String emailAddress;

    @JsonProperty("birthDate")
    private LocalDate birthDate;

    @JsonProperty("socialSecurityNumber")
    private String socialSecurityNumber;

    @JsonProperty("chalmersStudent")
    private Boolean chalmersStudent;

    @JsonProperty("graduation")
    private String graduation;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("deceased")
    private Boolean deceased;

    @JsonProperty("publishApproval")
    private Boolean publishApproval;

    @JsonProperty("wantCirculars")
    private Boolean wantCirculars;

    @JsonProperty("wantEmailCirculars")
    private Boolean wantEmailCirculars;

    @JsonProperty("image")
    private String image;

    @Builder
    public SpexareDto(
            final Long id,
            final String firstName,
            final String lastName,
            final String nickName,
            final String streetAddress,
            final String postalCode,
            final String postalAddress,
            final String country,
            final String phoneHome,
            final String phoneWork,
            final String phoneMobile,
            final String phoneOther,
            final String emailAddress,
            final LocalDate birthDate,
            final String socialSecurityNumber,
            final Boolean chalmersStudent,
            final String graduation,
            final String comment,
            final boolean deceased,
            final boolean publishApproval,
            final boolean wantCirculars,
            final boolean wantEmailCirculars,
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
        this.streetAddress = streetAddress;
        this.postalCode = postalCode;
        this.postalAddress = postalAddress;
        this.country = country;
        this.phoneHome = phoneHome;
        this.phoneWork = phoneWork;
        this.phoneMobile = phoneMobile;
        this.phoneOther = phoneOther;
        this.emailAddress = emailAddress;
        this.birthDate = birthDate;
        this.socialSecurityNumber = socialSecurityNumber;
        this.chalmersStudent = chalmersStudent;
        this.graduation = graduation;
        this.comment = comment;
        this.deceased = deceased;
        this.publishApproval = publishApproval;
        this.wantCirculars = wantCirculars;
        this.wantEmailCirculars = wantEmailCirculars;
    }
}
