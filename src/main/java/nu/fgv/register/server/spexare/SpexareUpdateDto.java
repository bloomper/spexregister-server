package nu.fgv.register.server.spexare;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
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

    @JsonProperty("birthDate")
    private LocalDate birthDate;

    @Size(max = 4, message = "{spexare.socialSecurityNumber.size}")
    @Pattern(regexp = "^\\d{4}$", message = "{spexare.socialSecurityNumber.size}")
    @JsonProperty("socialSecurityNumber")
    private String socialSecurityNumber;

    @Size(max = 255, message = "{spexare.graduation.size}")
    @JsonProperty("graduation")
    private String graduation;

    @JsonProperty("comment")
    private String comment;

}
