/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nu.fgv.register.server.spexare;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.util.AbstractAuditableDto;
import org.springframework.hateoas.server.core.Relation;

import java.time.Instant;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString
@Relation(collectionRelation = "spexare", itemRelation = "spexare")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpexareDto extends AbstractAuditableDto<SpexareDto> {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("nickName")
    private String nickName;

    @JsonProperty("socialSecurityNumber")
    private String socialSecurityNumber;

    @JsonProperty("deceased")
    private Boolean deceased;

    @JsonProperty("published")
    private Boolean published;

    @JsonProperty("graduation")
    private String graduation;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("image")
    private String imageUrl;

    @Builder
    public SpexareDto(
            final Long id,
            final String firstName,
            final String lastName,
            final String nickName,
            final String socialSecurityNumber,
            final Boolean deceased,
            final Boolean published,
            final String graduation,
            final String comment,
            final String imageUrl,
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
        this.socialSecurityNumber = socialSecurityNumber;
        this.deceased = deceased;
        this.published = published;
        this.graduation = graduation;
        this.comment = comment;
        this.imageUrl = imageUrl;
    }
}
