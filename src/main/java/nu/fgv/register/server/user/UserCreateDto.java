package nu.fgv.register.server.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Jacksonized // Needed due to this class having only one attribute
public class UserCreateDto {
    @NotBlank(message = "{user.username.notEmpty}")
    @Size(max = 255, message = "{user.username.size}")
    @Email(message = "{user.username.valid}")
    @JsonProperty("username")
    private String username;

}