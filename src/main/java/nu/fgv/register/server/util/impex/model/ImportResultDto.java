package nu.fgv.register.server.util.impex.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@Builder
@ToString
public class ImportResultDto {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("messages")
    private List<String> messages;

}
