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

package nu.fgv.register.server.spexare;

import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.util.search.Facet;
import nu.fgv.register.server.util.search.FacetGroup;
import nu.fgv.register.server.util.search.FacetValue;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.util.common.SearchException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository.AGGREGATIONS;
import static nu.fgv.register.server.util.Constants.AGGREGATION_COMPOSITE_DELIMITER;
import static nu.fgv.register.server.util.Constants.AGGREGATION_HIERARCHICAL_MARKER;
import static nu.fgv.register.server.util.security.SecurityUtil.isAdministrator;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@RequiredArgsConstructor
@Service
public class SpexareFacetService {

    private final MessageSource messageSource;

    public List<Facet> getFacets(final SearchResult<Spexare> searchResult) {
        final boolean isAdmin = isAdministrator();

        return AGGREGATIONS.stream()
                .filter(a -> {
                    if (Spexare_.PUBLISHED.equals(a) && !isAdmin) {
                        return false;
                    }
                    try {
                        searchResult.aggregation(AggregationKey.of(a));
                        return true;
                    } catch (final SearchException _) {
                        return false;
                    }
                })
                .map(a -> {
                    String baseFieldName = a;
                    String logicalKey = a;

                    if (a.contains(AGGREGATION_COMPOSITE_DELIMITER)) {
                        final String[] parts = a.split(Pattern.quote(AGGREGATION_COMPOSITE_DELIMITER));
                        baseFieldName = parts[0];
                        logicalKey = parts[1];
                    }

                    final Map<Object, Long> values = searchResult.aggregation(AggregationKey.of(a));
                    final String facetLabel = messageSource.getMessage("facet." + logicalKey, null, logicalKey, LocaleContextHolder.getLocale());

                    if (baseFieldName.contains(AGGREGATION_HIERARCHICAL_MARKER)) {
                        return Facet.builder()
                                .id(logicalKey)
                                .label(facetLabel)
                                .groups(createNestedGroups(logicalKey, values))
                                .build();
                    }

                    return Facet.builder()
                            .id(logicalKey)
                            .label(facetLabel)
                            .groups(List.of(FacetGroup.builder()
                                    .id(logicalKey)
                                    .label("")
                                    .values(createStandardValues(values))
                                    .build()))
                            .build();
                })
                .toList();
    }

    private List<FacetValue> createStandardValues(final Map<Object, Long> values) {
        return values.entrySet().stream()
                .map(entry -> {
                    final String rawValue = String.valueOf(entry.getKey());
                    String id = rawValue;
                    String displayValue = rawValue;

                    if (entry.getKey() instanceof final Boolean b) {
                        displayValue = messageSource.getMessage("boolean.%s".formatted(b), null, LocaleContextHolder.getLocale());
                    } else if (rawValue.contains(AGGREGATION_COMPOSITE_DELIMITER)) {
                        final String[] parts = rawValue.split(Pattern.quote(AGGREGATION_COMPOSITE_DELIMITER));
                        id = parts[0];
                        displayValue = parts[1];
                    }
                    return new FacetValue(id, displayValue, entry.getValue());
                })
                .toList();
    }

    private List<FacetGroup> createNestedGroups(final String logicalKey, final Map<Object, Long> values) {
        final Map<String, List<FacetValue>> groups = new HashMap<>();
        final Map<String, String> groupLabelLookup = new HashMap<>();

        values.forEach((key, count) -> {
            final String rawValue = String.valueOf(key);
            if (rawValue.contains(AGGREGATION_COMPOSITE_DELIMITER)) {
                final String[] parts = rawValue.split(Pattern.quote(AGGREGATION_COMPOSITE_DELIMITER));
                final String technicalValueId = parts[0];
                final String displayPart = parts[1];

                if (displayPart.contains(":")) {
                    final String groupId = technicalValueId.contains(":") ? technicalValueId.split(":")[0] : technicalValueId;
                    final String groupLabel = displayPart.split(":")[0].trim();
                    final String valueLabel = displayPart.split(":")[1].trim();

                    groupLabelLookup.put(groupId, groupLabel);
                    groups.computeIfAbsent(groupId, _ -> new ArrayList<>())
                            .add(new FacetValue(technicalValueId, valueLabel, count));
                } else {
                    groupLabelLookup.put(technicalValueId, "");
                    groups.computeIfAbsent(technicalValueId, _ -> new ArrayList<>())
                            .add(new FacetValue(technicalValueId, displayPart, count));
                }
            }
        });

        return groupLabelLookup.entrySet().stream()
                .map(entry -> FacetGroup.builder()
                        .id(entry.getKey())
                        .label(entry.getValue())
                        .values(groups.get(entry.getKey()))
                        .build())
                .toList();
    }
}
