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

import lombok.Getter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Getter
public enum AnalyticsSection {

    TOTALS("totals"),
    PARTICIPATION("participation"),
    DEMOGRAPHICS("demographics"),
    LIFECYCLE("lifecycle"),
    DATA_QUALITY("dataQuality"),
    OPERATIONS("operations");

    public static final Set<AnalyticsSection> ALL = EnumSet.allOf(AnalyticsSection.class);

    public static final Set<AnalyticsSection> FACET_BACKED =
            EnumSet.of(PARTICIPATION, DEMOGRAPHICS, LIFECYCLE, DATA_QUALITY);

    private final String field;

    AnalyticsSection(final String field) {
        this.field = field;
    }

    public static Set<AnalyticsSection> from(final Set<String> selectedFields) {
        final Set<AnalyticsSection> sections = EnumSet.noneOf(AnalyticsSection.class);

        Arrays.stream(values())
                .filter(section -> selectedFields.contains(section.field))
                .forEach(sections::add);

        return sections;
    }
}
