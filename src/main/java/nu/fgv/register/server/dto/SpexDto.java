package nu.fgv.register.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SpexDto(
        @JsonProperty("id") Long id,
        @JsonProperty("year") String year,
        @JsonProperty("category") SpexCategoryDto category,
        @JsonProperty("parent") SpexDto parent,
        @JsonProperty("details") SpexDetailsDto details,
        @JsonProperty("createdBy") String createdBy,
        @JsonProperty("createdDate") long createdDate,
        @JsonProperty("lastModifiedBy") String lastModifiedBy,
        @JsonProperty("lastModifiedDate") long lastModifiedDate
) {
}
