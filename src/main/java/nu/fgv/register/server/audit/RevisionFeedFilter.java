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

package nu.fgv.register.server.audit;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public record RevisionFeedFilter(
        @Nullable AuditedType type,
        List<String> modifiedBy,
        List<AuditSource> sources,
        @Nullable LocalDate from,
        @Nullable LocalDate to,
        @Nullable Integer sinceInDays
) {

    public RevisionFeedFilter {
        modifiedBy = modifiedBy == null ? List.of() : List.copyOf(modifiedBy);
        sources = sources == null ? List.of() : List.copyOf(sources);
    }

    public static RevisionFeedFilter of(final @Nullable AuditedType type,
                                        final @Nullable List<String> modifiedBy,
                                        final @Nullable List<AuditSource> sources,
                                        final @Nullable LocalDate from,
                                        final @Nullable LocalDate to,
                                        final @Nullable Integer sinceInDays) {
        return new RevisionFeedFilter(type, modifiedBy, sources, from, to, sinceInDays);
    }
}
