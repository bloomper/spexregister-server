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

import graphql.schema.DataFetchingFieldSelectionSet;
import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Controller
@RequiredArgsConstructor
public class AnalyticsGraphqlApi {

    private final AnalyticsService service;

    @QueryMapping("analytics")
    @RequiresAdminOrEditorOrUser
    public AnalyticsDto getAnalytics(final DataFetchingFieldSelectionSet selection) {
        return service.getAnalytics(AnalyticsSection.from(
                selection.getImmediateFields().stream()
                        .map(graphql.schema.SelectedField::getName)
                        .collect(Collectors.toSet())));
    }

    @SchemaMapping(typeName = "Analytics", field = "totals")
    public AnalyticsDto.TotalsDto getTotals(final AnalyticsDto analytics) {
        return Objects.requireNonNull(analytics.totals());
    }

    @SchemaMapping(typeName = "Analytics", field = "participation")
    public AnalyticsDto.ParticipationDto getParticipation(final AnalyticsDto analytics) {
        return Objects.requireNonNull(analytics.participation());
    }

    @SchemaMapping(typeName = "Analytics", field = "demographics")
    public AnalyticsDto.DemographicsDto getDemographics(final AnalyticsDto analytics) {
        return Objects.requireNonNull(analytics.demographics());
    }

    @SchemaMapping(typeName = "Analytics", field = "lifecycle")
    public AnalyticsDto.LifecycleDto getLifecycle(final AnalyticsDto analytics) {
        return Objects.requireNonNull(analytics.lifecycle());
    }

}
