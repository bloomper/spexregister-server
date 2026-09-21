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

package nu.fgv.register.server.analytics;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The dashboard, in sections that stack by role: everyone sees the register's own story, editors
 * additionally see what needs fixing, administrators additionally see how the system is being run.
 * A section a caller may not read is {@code null} rather than empty, so the client can tell
 * "nothing to show" from "not for you".
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Builder
public record AnalyticsDto(
        @Nullable TotalsDto totals,
        @Nullable ParticipationDto participation,
        @Nullable DemographicsDto demographics,
        @Nullable LifecycleDto lifecycle,
        @Nullable DataQualityDto dataQuality,
        @Nullable OperationsDto operations
) {

    @Builder
    public record TotalsDto(
            Long spexareCount,
            List<HistoryDto> spexareCountHistory,
            Long userCount,
            List<HistoryDto> userCountHistory,
            Long spexCount,
            List<HistoryDto> spexCountHistory,
            Long spexRevivalCount,
            List<HistoryDto> spexRevivalCountHistory,
            Long taskCount,
            List<HistoryDto> taskCountHistory
    ) {
    }

    @Builder
    public record ParticipationDto(
            List<BucketDto> bySpexYear,
            List<BucketDto> bySpexCategory,
            List<BucketDto> topSpex,
            List<BucketDto> byTaskCategory,
            List<BucketDto> topTask,
            List<BucketDto> byVocal
    ) {
    }

    @Builder
    public record DemographicsDto(
            List<BucketDto> byCountry,
            List<BucketDto> byAddressType,
            List<BucketDto> byMembership,
            List<BucketDto> byTag,
            List<BucketDto> byToggle,
            List<BucketDto> byStatus,
            List<ConsentCompletionDto> consentCompletion
    ) {
    }

    @Builder
    public record LifecycleDto(
            List<BucketDto> newcomersByYear,
            List<BucketDto> lastActiveByYear,
            List<BucketDto> byEngagement,
            List<BucketDto> byDormancy,
            Long oneTimers,
            Long returning,
            Long veterans,
            Long neverActive
    ) {
    }

    @Builder
    public record DataQualityDto(
            Long total,
            List<BucketDto> issues,
            Long complete
    ) {
    }

    @Builder
    public record OperationsDto(
            List<BucketDto> usersByState,
            Long usersWithoutSpexare,
            Long spexareWithoutUser,
            List<BucketDto> revisionsByMonth,
            List<BucketDto> revisionsBySource,
            List<BucketDto> topEditors
    ) {
    }
}
