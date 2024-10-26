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

package nu.fgv.register.server.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.hateoas.server.core.Relation;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@Relation(collectionRelation = "events", itemRelation = "events")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventDto {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("event")
    private String event;

    @JsonProperty("source")
    private String source;

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("createdAt")
    private Instant createdAt;

    @Builder
    public EventDto(
            final Long id,
            final String event,
            final String source,
            final String createdBy,
            final Instant createdAt
    ) {
        this.id = id;
        this.event = event;
        this.source = source;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }
}
