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


package nu.fgv.register.server.map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Where a spexare lives, as geocoded from their preferred address. Not audited: it is derived data, recalculated by the
 * {@code spexregister.jobs.geocode-addresses} job whenever the address it came from changes. A lookup that found nothing
 * is kept without coordinates, so the same address is not looked up again.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Entity
@Table(name = "spexare_location")
@NoArgsConstructor
@Getter
@Setter
public class SpexareLocation {

    @Id
    @Column(name = "spexare_id")
    private Long spexareId;

    @Column(name = "query", nullable = false, length = 1024)
    private String query;

    @Column(name = "latitude")
    @Nullable
    private Double latitude;

    @Column(name = "longitude")
    @Nullable
    private Double longitude;

    @Column(name = "geocoded_at", nullable = false)
    private Instant geocodedAt;

    SpexareLocation(final Long spexareId) {
        this.spexareId = spexareId;
    }
}
