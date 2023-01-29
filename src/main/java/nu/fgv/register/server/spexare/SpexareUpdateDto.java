package nu.fgv.register.server.spexare;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.util.impex.model.ExcelImportCell;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDate;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@Builder
@Relation(collectionRelation = "spex", itemRelation = "spex")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpexareUpdateDto {
    @JsonProperty("id")
    @ExcelImportCell(position = 0, primaryKey = true)
    private Long id;

    @NotEmpty(message = "{spexare.firstName.notEmpty}")
    @Size(max = 255, message = "{spexare.firstName.size}")
    @JsonProperty("firstName")
    private String firstName;

    @NotEmpty(message = "{spexare.lastName.notEmpty}")
    @Size(max = 255, message = "{spexare.lastName.size}")
    @JsonProperty("lastName")
    private String lastName;

    @Size(max = 255, message = "{spexare.nickName.size}")
    @Column(name = "nick_name")
    @JsonProperty("nickName")
    private String nickName;

    @Size(max = 255, message = "{spexare.streetAddress.size}")
    @JsonProperty("streetAddress")
    private String streetAddress;

    @Size(max = 255, message = "{spexare.postalCode.size}")
    @JsonProperty("postalCode")
    private String postalCode;

    @Size(max = 255, message = "{spexare.postalAddress.size}")
    @JsonProperty("postalAddress")
    private String postalAddress;

    @Size(max = 255, message = "{spexare.country.size}")
    @JsonProperty("country")
    private String country;

    @Size(max = 255, message = "{spexare.phoneHome.size}")
    @JsonProperty("phoneHome")
    private String phoneHome;

    @Size(max = 255, message = "{spexare.phoneWork.size}")
    @JsonProperty("phoneWork")
    private String phoneWork;

    @Size(max = 255, message = "{spexare.phoneMobile.size}")
    @JsonProperty("phoneMobile")
    private String phoneMobile;

    @Size(max = 255, message = "{spexare.phoneOther.size}")
    @JsonProperty("phoneOther")
    private String phoneOther;

    @Size(max = 255, message = "{spexare.emailAddress.size}")
    @Email(message = "{spexare.emailAddress.valid}")
    @JsonProperty("emailAddress")
    private String emailAddress;

    @JsonProperty("birthDate")
    private LocalDate birthDate;

    @Size(max = 4, message = "{spexare.socialSecurityNumber.size}")
    @Pattern(regexp = "^\\d{4}$", message = "{spexare.socialSecurityNumber.size}")
    @JsonProperty("socialSecurityNumber")
    private String socialSecurityNumber;

    @JsonProperty("chalmersStudent")
    private Boolean chalmersStudent;

    @Size(max = 255, message = "{spexare.graduation.size}")
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

}
