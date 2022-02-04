package nu.fgv.register.server.spex;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SpexDetailsDto(
        @JsonProperty("id") Long id,
        @JsonProperty("title") String title,
        //@JsonProperty("posterUrl") String posterUrl,
        @JsonProperty("createdBy") String createdBy,
        @JsonProperty("createdDate") long createdDate,
        @JsonProperty("lastModifiedBy") String lastModifiedBy,
        @JsonProperty("lastModifiedDate") long lastModifiedDate
) {
}
