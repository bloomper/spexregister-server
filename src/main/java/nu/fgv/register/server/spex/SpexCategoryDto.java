package nu.fgv.register.server.spex;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SpexCategoryDto(
        @JsonProperty("id") Long id,
        @JsonProperty("name") String name,
        @JsonProperty("firstYear") String firstYear,
        //@JsonProperty("logoUrl") String logoUrl,
        @JsonProperty("createdBy") String createdBy,
        @JsonProperty("createdDate") long createdDate,
        @JsonProperty("lastModifiedBy") String lastModifiedBy,
        @JsonProperty("lastModifiedDate") long lastModifiedDate
) {
}
