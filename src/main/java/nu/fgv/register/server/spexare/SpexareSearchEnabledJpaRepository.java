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

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import nu.fgv.register.server.spex.SpexDetails_;
import nu.fgv.register.server.spex.Spex_;
import nu.fgv.register.server.spexare.activity.Activity_;
import nu.fgv.register.server.spexare.activity.spex.SpexActivity_;
import nu.fgv.register.server.spexare.activity.task.TaskActivity_;
import nu.fgv.register.server.spexare.activity.task.actor.Actor_;
import nu.fgv.register.server.spexare.address.Address_;
import nu.fgv.register.server.tag.Tag_;
import nu.fgv.register.server.task.Task_;
import nu.fgv.register.server.util.search.AbstractSearchEnabledJpaRepository;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.jspecify.annotations.Nullable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;

import java.util.List;
import java.util.regex.Pattern;

import static nu.fgv.register.server.util.Constants.AGGREGATION_COMPOSITE_DELIMITER;
import static nu.fgv.register.server.util.Constants.AGGREGATION_HIERARCHICAL_MARKER;
import static nu.fgv.register.server.util.security.SecurityUtil.isAdministrator;
import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Transactional
public class SpexareSearchEnabledJpaRepository extends AbstractSearchEnabledJpaRepository<Spexare, Long> {

    private static final String ATTRIBUTE_DELIMITER = ".";

    private static final String[] PRIMARY_FUZZY_FIELDS = new String[]{
            Spexare_.FIRST_NAME, Spexare_.LAST_NAME, Spexare_.NICK_NAME
    };

    private static final String[] SECONDARY_FUZZY_FIELDS = new String[]{
            Spexare_.SOCIAL_SECURITY_NUMBER,
            String.join(ATTRIBUTE_DELIMITER, Spexare_.TAGS, Tag_.NAME),
            String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.SPEX_ACTIVITY, SpexActivity_.SPEX, Spex_.DETAILS, SpexDetails_.TITLE),
            String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.TASK_ACTIVITIES, TaskActivity_.ACTORS, Actor_.ROLE),
    };

    private static final String[] FUZZY_FIELDS = new String[]{
            Spexare_.COMMENT,
            String.join(ATTRIBUTE_DELIMITER, Spexare_.ADDRESSES, Address_.STREET_ADDRESS),
            String.join(ATTRIBUTE_DELIMITER, Spexare_.ADDRESSES, Address_.CITY),
            String.join(ATTRIBUTE_DELIMITER, Spexare_.ADDRESSES, "countryName")
    };

    private static final String[] EXACT_FIELDS = new String[]{
            Spexare_.SOCIAL_SECURITY_NUMBER, Spexare_.GRADUATION,
            String.join(ATTRIBUTE_DELIMITER, Spexare_.ADDRESSES, Address_.POSTAL_CODE),
            String.join(ATTRIBUTE_DELIMITER, Spexare_.ADDRESSES, Address_.COUNTRY),
            String.join(ATTRIBUTE_DELIMITER, Spexare_.ADDRESSES, Address_.PHONE),
            String.join(ATTRIBUTE_DELIMITER, Spexare_.ADDRESSES, Address_.PHONE_MOBILE),
            String.join(ATTRIBUTE_DELIMITER, Spexare_.ADDRESSES, Address_.EMAIL_ADDRESS)
    };

    private static final String AGGREGATION_DECEASED = Spexare_.DECEASED;
    private static final String AGGREGATION_PUBLISHED = Spexare_.PUBLISHED;
    private static final String AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_YEAR = String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.SPEX_ACTIVITY, SpexActivity_.SPEX, Spex_.YEAR) + AGGREGATION_COMPOSITE_DELIMITER + "spexYears";
    private static final String AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_TITLE = String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.SPEX_ACTIVITY, SpexActivity_.SPEX, Spex_.DETAILS, AGGREGATION_HIERARCHICAL_MARKER) + AGGREGATION_COMPOSITE_DELIMITER + "spexTitles";
    private static final String AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_CATEGORY_NAME = String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.SPEX_ACTIVITY, SpexActivity_.SPEX, Spex_.DETAILS, SpexDetails_.CATEGORY, AGGREGATION_HIERARCHICAL_MARKER) + AGGREGATION_COMPOSITE_DELIMITER + "spexCategoryNames";
    private static final String AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_NAME = String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.TASK_ACTIVITIES, TaskActivity_.TASK, AGGREGATION_HIERARCHICAL_MARKER) + AGGREGATION_COMPOSITE_DELIMITER + "taskNames";
    private static final String AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_CATEGORY_NAME = String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.TASK_ACTIVITIES, TaskActivity_.TASK, Task_.CATEGORY, AGGREGATION_HIERARCHICAL_MARKER) + AGGREGATION_COMPOSITE_DELIMITER + "taskCategoryNames";
    private static final String AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_ACTORS_VOCAL = String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.TASK_ACTIVITIES, TaskActivity_.ACTORS, AGGREGATION_HIERARCHICAL_MARKER) + AGGREGATION_COMPOSITE_DELIMITER + "actorVocals";
    private static final String AGGREGATION_TAGS_NAME = String.join(ATTRIBUTE_DELIMITER, Spexare_.TAGS, AGGREGATION_HIERARCHICAL_MARKER) + AGGREGATION_COMPOSITE_DELIMITER + "tags";
    private static final String AGGREGATION_MEMBERSHIPS = String.join(ATTRIBUTE_DELIMITER, Spexare_.MEMBERSHIPS, AGGREGATION_HIERARCHICAL_MARKER) + AGGREGATION_COMPOSITE_DELIMITER + "memberships";
    private static final String AGGREGATION_CONSENTS = String.join(ATTRIBUTE_DELIMITER, Spexare_.CONSENTS, AGGREGATION_HIERARCHICAL_MARKER) + AGGREGATION_COMPOSITE_DELIMITER + "consents";
    private static final String AGGREGATION_TOGGLES = String.join(ATTRIBUTE_DELIMITER, Spexare_.TOGGLES, AGGREGATION_HIERARCHICAL_MARKER) + AGGREGATION_COMPOSITE_DELIMITER + "toggles";
    private static final List<String> HIERARCHICAL_AGGREGATIONS = List.of(
            AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_TITLE,
            AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_CATEGORY_NAME,
            AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_NAME,
            AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_CATEGORY_NAME,
            AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_ACTORS_VOCAL,
            AGGREGATION_TAGS_NAME,
            AGGREGATION_MEMBERSHIPS,
            AGGREGATION_CONSENTS,
            AGGREGATION_TOGGLES
    );
    static final List<String> AGGREGATIONS = List.of(
            AGGREGATION_DECEASED,
            AGGREGATION_PUBLISHED,
            AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_YEAR,
            AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_TITLE,
            AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_CATEGORY_NAME,
            AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_NAME,
            AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_CATEGORY_NAME,
            AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_ACTORS_VOCAL,
            AGGREGATION_TAGS_NAME,
            AGGREGATION_MEMBERSHIPS,
            AGGREGATION_CONSENTS,
            AGGREGATION_TOGGLES
    );
    private static final List<String> BOOLEAN_AGGREGATIONS = List.of(
            AGGREGATION_DECEASED,
            AGGREGATION_PUBLISHED
    );

    public SpexareSearchEnabledJpaRepository(final Class<Spexare> domainClass, final EntityManager entityManager) {
        super(domainClass, entityManager);
    }

    public SpexareSearchEnabledJpaRepository(final JpaEntityInformation<Spexare, Long> entityInformation, final EntityManager entityManager) {
        super(entityInformation, entityManager);
    }

    @Override
    public SearchResult<Spexare> search(final SearchSession searchSession,
                                        final SearchQuery query,
                                        @Nullable final List<Long> ids,
                                        final int offset,
                                        final int limit,
                                        final Sort sort) {
        final boolean isAdmin = isAdministrator();

        var search = searchSession
                .search(Spexare.class)
                .where(f -> f.bool().with(b -> {
                            if (hasText(query.freeTextQuery())) {
                                b.must(f.bool()
                                        .should(f.match().fields(PRIMARY_FUZZY_FIELDS).matching(query.freeTextQuery()).fuzzy().boost(10.0f))
                                        .should(f.match().fields(SECONDARY_FUZZY_FIELDS).matching(query.freeTextQuery()).fuzzy().boost(5.0f))
                                        .should(f.match().fields(EXACT_FIELDS).matching(query.freeTextQuery()).boost(5.0f))
                                        .should(f.match().fields(FUZZY_FIELDS).matching(query.freeTextQuery()).fuzzy().boost(1.0f))
                                );
                            } else if (ids != null && !ids.isEmpty()) {
                                b.must(f.id().matchingAny(ids));
                            } else {
                                b.must(f.matchAll());
                            }
                            if (!isAdmin) {
                                b.must(f.match().field(Spexare_.PUBLISHED).matching(true));
                            }
                            if (query.aggregationFilters() != null && !query.aggregationFilters().isEmpty()) {
                                b.must(f.bool().with(fb -> {
                                    query.aggregationFilters().forEach(a -> {
                                        final String fullKey = AGGREGATIONS.stream()
                                                .filter(key -> key.endsWith(AGGREGATION_COMPOSITE_DELIMITER + a.name()) || key.equals(a.name()))
                                                .findFirst()
                                                .orElse(a.name());

                                        String fieldPath = fullKey.split(Pattern.quote(AGGREGATION_COMPOSITE_DELIMITER))[0];

                                        if (fieldPath.endsWith(AGGREGATION_HIERARCHICAL_MARKER)) {
                                            fieldPath = fieldPath.substring(0, fieldPath.length() - AGGREGATION_HIERARCHICAL_MARKER.length() - 1);
                                            final String localizedField = fieldPath + ATTRIBUTE_DELIMITER + AGGREGATION_HIERARCHICAL_MARKER + LocaleContextHolder.getLocale().getLanguage();
                                            final String filterValue = a.value().toLowerCase();

                                            fb.must(f.wildcard().field(localizedField).matching(filterValue + AGGREGATION_COMPOSITE_DELIMITER + "*"));
                                        } else if (fieldPath.endsWith("_")) {
                                            final String localizedField = fieldPath + LocaleContextHolder.getLocale().getLanguage();
                                            final String filterValue = a.value().toLowerCase();

                                            fb.must(f.wildcard().field(localizedField).matching(filterValue + AGGREGATION_COMPOSITE_DELIMITER + "*"));
                                        } else if (BOOLEAN_AGGREGATIONS.contains(fieldPath)) {
                                            fb.must(f.match().field(fieldPath).matching(Boolean.valueOf(a.value())));
                                        } else {
                                            fb.must(f.match().field(fieldPath).matching(a.value()));
                                        }
                                    });
                                }));
                            }
                        })
                )
                .aggregation(AggregationKey.of(AGGREGATION_DECEASED), f -> f.terms().field(AGGREGATION_DECEASED, Boolean.class))
                .aggregation(AggregationKey.of(AGGREGATION_PUBLISHED), f -> isAdmin ? f.terms().field(AGGREGATION_PUBLISHED, Boolean.class) : f.terms().field(AGGREGATION_DECEASED, Boolean.class))
                .aggregation(AggregationKey.of(AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_YEAR), f -> {
                    final String fieldName = AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_YEAR.split(Pattern.quote(AGGREGATION_COMPOSITE_DELIMITER))[0];
                    return f.terms().field(fieldName, String.class);
                });

        for (final String key : HIERARCHICAL_AGGREGATIONS) {
            final String fieldPrefix = key.split(Pattern.quote(AGGREGATION_COMPOSITE_DELIMITER))[0];
            search = search.aggregation(AggregationKey.of(key), f -> f.terms().field(fieldPrefix + LocaleContextHolder.getLocale(), String.class));
        }

        return search.sort(f -> determineSort(Spexare.class, f, sort))
                .fetch(offset, limit);
    }

    @Override
    public SearchResult<Spexare> search(final SearchSession searchSession,
                                        final SearchQuery query,
                                        final int offset,
                                        final int limit,
                                        final Sort sort) {
        return search(searchSession, query, null, offset, limit, sort);
    }

    @Override
    public SearchResult<Spexare> search(final SearchSession searchSession,
                                        final SearchQuery query,
                                        final Pageable pageable) {
        return search(searchSession, query, null, pageable);
    }

}
