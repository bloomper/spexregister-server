/*
 * Copyright 2026 the original author or authors.
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

package nu.fgv.register.server.permission;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.security.policy.Permission;
import nu.fgv.register.server.util.AbstractAuditableDto;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDate;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PermissionGrantDto extends AbstractAuditableDto<PermissionGrantDto> {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("userId")
    private @Nullable Long userId;

    @JsonProperty("role")
    private @Nullable String role;

    @JsonProperty("permission")
    private Permission permission;

    @JsonProperty("scope")
    private PermissionGrantScope scope;

    @JsonProperty("spexareId")
    private @Nullable Long spexareId;

    @JsonProperty("spexId")
    private @Nullable Long spexId;

    @JsonProperty("validUntil")
    private @Nullable LocalDate validUntil;

    @JsonProperty("expired")
    private boolean expired;

    @Builder
    public PermissionGrantDto(
            final Long id,
            final @Nullable Long userId,
            final @Nullable String role,
            final Permission permission,
            final PermissionGrantScope scope,
            final @Nullable Long spexareId,
            final @Nullable Long spexId,
            final @Nullable LocalDate validUntil,
            final boolean expired,
            final String createdBy,
            final Instant createdAt,
            final String lastModifiedBy,
            final Instant lastModifiedAt
    ) {
        super(createdBy, createdAt, lastModifiedBy, lastModifiedAt);
        this.id = id;
        this.userId = userId;
        this.role = role;
        this.permission = permission;
        this.scope = scope;
        this.spexareId = spexareId;
        this.spexId = spexId;
        this.validUntil = validUntil;
        this.expired = expired;
    }
}
