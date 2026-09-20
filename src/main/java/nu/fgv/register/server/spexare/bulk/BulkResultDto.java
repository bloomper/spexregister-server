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

package nu.fgv.register.server.spexare.bulk;

import lombok.Builder;
import org.springframework.hateoas.server.core.Relation;

import java.util.List;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Builder
@Relation(collectionRelation = "bulkResults", itemRelation = "bulkResult")
public record BulkResultDto(
        SpexareBulkOperation operation,
        int requested,
        int applied,
        int unchanged,
        int blocked,
        List<BulkEntryDto> entries
) {

    public static BulkResultDto of(final SpexareBulkOperation operation, final List<BulkEntryDto> entries) {
        return BulkResultDto.builder()
                .operation(operation)
                .requested(entries.size())
                .applied(count(entries, BulkOutcome.APPLIED))
                .unchanged(count(entries, BulkOutcome.UNCHANGED))
                .blocked(count(entries, BulkOutcome.NOT_PERMITTED))
                .entries(entries)
                .build();
    }

    private static int count(final List<BulkEntryDto> entries, final BulkOutcome outcome) {
        return (int) entries.stream().filter(e -> e.outcome() == outcome).count();
    }
}
