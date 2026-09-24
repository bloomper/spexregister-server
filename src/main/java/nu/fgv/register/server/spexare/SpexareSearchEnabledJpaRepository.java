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
import nu.fgv.register.server.util.error.BadRequestException;
import nu.fgv.register.server.util.search.AbstractSearchEnabledJpaRepository;
import nu.fgv.register.server.util.security.SocialSecurityNumberHasher;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateOptionsCollector;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
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
            Spexare_.GRADUATION,
            String.join(ATTRIBUTE_DELIMITER, Spexare_.ADDRESSES, Address_.POSTAL_CODE),
            String.join(ATTRIBUTE_DELIMITER, Spexare_.ADDRESSES, Address_.COUNTRY),
            String.join(ATTRIBUTE_DELIMITER, Spexare_.ADDRESSES, Address_.PHONE),
            String.join(ATTRIBUTE_DELIMITER, Spexare_.ADDRESSES, Address_.PHONE_MOBILE),
            String.join(ATTRIBUTE_DELIMITER, Spexare_.ADDRESSES, Address_.EMAIL_ADDRESS)
    };

    static final String AGGREGATION_DATA_QUALITY = "quality";

    public static final String FACET_DECEASED = Spexare_.DECEASED;
    public static final String FACET_PUBLISHED = Spexare_.PUBLISHED;
    public static final String FACET_SPEX_YEARS = "spexYears";
    public static final String FACET_SPEX_TITLES = "spexTitles";
    public static final String FACET_SPEX_CATEGORY_NAMES = "spexCategoryNames";
    public static final String FACET_TASK_NAMES = "taskNames";
    public static final String FACET_TASK_CATEGORY_NAMES = "taskCategoryNames";
    public static final String FACET_ACTOR_VOCALS = "actorVocals";
    public static final String FACET_TAGS = "tags";
    public static final String FACET_MEMBERSHIPS = "memberships";
    public static final String FACET_CONSENTS = "consents";
    public static final String FACET_TOGGLES = "toggles";
    public static final String FACET_ADDRESS_TYPES = "addressTypes";
    public static final String FACET_COUNTRIES = "countries";
    public static final String FACET_DEBUT_YEARS = "debutYears";
    public static final String FACET_LAST_ACTIVE_YEARS = "lastActiveYears";
    public static final String FACET_SPEX_COUNTS = "spexCounts";

    private static final String FIELD_HAS_IMAGE = "hasImage";
    private static final String FIELD_DEBUT_YEAR = "debutYear";
    private static final String FIELD_LAST_ACTIVE_YEAR = "lastActiveYear";
    private static final String FIELD_SPEX_COUNT = "spexCount";

    private static final Pattern YEAR_RANGE_PATTERN = Pattern.compile("^(\\d{4})?\\.\\.(\\d{4})?$");
    private static final List<String> YEAR_RANGE_FACETS = List.of(FACET_SPEX_YEARS, FACET_DEBUT_YEARS, FACET_LAST_ACTIVE_YEARS);

    private static final String AGGREGATION_DECEASED = Spexare_.DECEASED;
    private static final String AGGREGATION_PUBLISHED = Spexare_.PUBLISHED;
    private static final String AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_YEAR = String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.SPEX_ACTIVITY, SpexActivity_.SPEX, Spex_.YEAR) + AGGREGATION_COMPOSITE_DELIMITER + FACET_SPEX_YEARS;
    private static final String AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_TITLE = String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.SPEX_ACTIVITY, SpexActivity_.SPEX, Spex_.DETAILS, AGGREGATION_HIERARCHICAL_MARKER) + AGGREGATION_COMPOSITE_DELIMITER + FACET_SPEX_TITLES;
    private static final String AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_CATEGORY_NAME = String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.SPEX_ACTIVITY, SpexActivity_.SPEX, Spex_.DETAILS, SpexDetails_.CATEGORY, AGGREGATION_HIERARCHICAL_MARKER) + AGGREGATION_COMPOSITE_DELIMITER + FACET_SPEX_CATEGORY_NAMES;
    private static final String AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_NAME = String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.TASK_ACTIVITIES, TaskActivity_.TASK, AGGREGATION_HIERARCHICAL_MARKER) + AGGREGATION_COMPOSITE_DELIMITER + FACET_TASK_NAMES;
    private static final String AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_CATEGORY_NAME = String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.TASK_ACTIVITIES, TaskActivity_.TASK, Task_.CATEGORY, AGGREGATION_HIERARCHICAL_MARKER) + AGGREGATION_COMPOSITE_DELIMITER + FACET_TASK_CATEGORY_NAMES;
    private static final String AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_ACTORS_VOCAL = String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.TASK_ACTIVITIES, TaskActivity_.ACTORS, AGGREGATION_HIERARCHICAL_MARKER) + AGGREGATION_COMPOSITE_DELIMITER + FACET_ACTOR_VOCALS;
    private static final String AGGREGATION_TAGS_NAME = String.join(ATTRIBUTE_DELIMITER, Spexare_.TAGS, AGGREGATION_HIERARCHICAL_MARKER) + AGGREGATION_COMPOSITE_DELIMITER + FACET_TAGS;
    private static final String AGGREGATION_MEMBERSHIPS = String.join(ATTRIBUTE_DELIMITER, Spexare_.MEMBERSHIPS, AGGREGATION_HIERARCHICAL_MARKER) + AGGREGATION_COMPOSITE_DELIMITER + FACET_MEMBERSHIPS;
    private static final String AGGREGATION_CONSENTS = String.join(ATTRIBUTE_DELIMITER, Spexare_.CONSENTS, AGGREGATION_HIERARCHICAL_MARKER) + AGGREGATION_COMPOSITE_DELIMITER + FACET_CONSENTS;
    private static final String AGGREGATION_TOGGLES = String.join(ATTRIBUTE_DELIMITER, Spexare_.TOGGLES, AGGREGATION_HIERARCHICAL_MARKER) + AGGREGATION_COMPOSITE_DELIMITER + FACET_TOGGLES;
    private static final String AGGREGATION_ADDRESSES_TYPE = String.join(ATTRIBUTE_DELIMITER, Spexare_.ADDRESSES, AGGREGATION_HIERARCHICAL_MARKER) + AGGREGATION_COMPOSITE_DELIMITER + FACET_ADDRESS_TYPES;
    private static final String AGGREGATION_ADDRESSES_COUNTRY = String.join(ATTRIBUTE_DELIMITER, Spexare_.ADDRESSES, Address_.COUNTRY) + AGGREGATION_COMPOSITE_DELIMITER + FACET_COUNTRIES;
    private static final String AGGREGATION_DEBUT_YEAR = FIELD_DEBUT_YEAR + AGGREGATION_COMPOSITE_DELIMITER + FACET_DEBUT_YEARS;
    private static final String AGGREGATION_LAST_ACTIVE_YEAR = FIELD_LAST_ACTIVE_YEAR + AGGREGATION_COMPOSITE_DELIMITER + FACET_LAST_ACTIVE_YEARS;
    private static final String AGGREGATION_SPEX_COUNT = FIELD_SPEX_COUNT + AGGREGATION_COMPOSITE_DELIMITER + FACET_SPEX_COUNTS;
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
            AGGREGATION_TOGGLES,
            AGGREGATION_ADDRESSES_TYPE,
            AGGREGATION_ADDRESSES_COUNTRY,
            AGGREGATION_DEBUT_YEAR,
            AGGREGATION_LAST_ACTIVE_YEAR,
            AGGREGATION_SPEX_COUNT
    );
    private static final List<String> STRING_AGGREGATIONS = List.of(
            AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_YEAR,
            AGGREGATION_ADDRESSES_COUNTRY,
            AGGREGATION_DEBUT_YEAR,
            AGGREGATION_LAST_ACTIVE_YEAR
    );
    private static final List<String> HIERARCHICAL_AGGREGATIONS = List.of(
            AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_TITLE,
            AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_CATEGORY_NAME,
            AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_NAME,
            AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_CATEGORY_NAME,
            AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_ACTORS_VOCAL,
            AGGREGATION_TAGS_NAME,
            AGGREGATION_MEMBERSHIPS,
            AGGREGATION_CONSENTS,
            AGGREGATION_TOGGLES,
            AGGREGATION_ADDRESSES_TYPE
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
    public SearchResult<Spexare> search(final SearchSession searchSession, final SearchQuery query, final int offset, final int limit, final Sort sort) {
        final boolean isAdmin = isAdministrator();
        final boolean sensitiveReadable = SpexareSensitiveData.isReadableForAll();
        var search = searchSession
                .search(Spexare.class)
                .where(f -> f.bool().with(b -> {
                            if (hasText(query.freeTextQuery())) {
                                b.must(f.bool().with(sb -> {
                                    sb.should(f.match().fields(PRIMARY_FUZZY_FIELDS).matching(query.freeTextQuery()).fuzzy().boost(10.0f));
                                    sb.should(f.match().fields(SECONDARY_FUZZY_FIELDS).matching(query.freeTextQuery()).fuzzy().boost(5.0f));
                                    sb.should(f.match().fields(EXACT_FIELDS).matching(query.freeTextQuery()).boost(5.0f));
                                    sb.should(f.match().fields(FUZZY_FIELDS).matching(query.freeTextQuery()).fuzzy().boost(1.0f));
                                    Arrays.stream(query.freeTextQuery().split("\\s+")).forEach(term -> {
                                        if (SocialSecurityNumberHasher.isBirthDate(term)) {
                                            sb.should(f.match().field(Spexare.INDEX_BIRTH_DATE).matching(term).boost(5.0f));
                                        } else if (sensitiveReadable && SocialSecurityNumberHasher.isNumber(term)) {
                                            sb.should(f.match().field(Spexare.INDEX_SOCIAL_SECURITY_NUMBER_HASH).matching(term).boost(10.0f));
                                        }
                                    });
                                }));
                            } else {
                                b.must(f.matchAll());
                            }
                            if (!isAdmin) {
                                b.must(f.match().field(Spexare_.PUBLISHED).matching(true));
                            }
                            if (query.aggregationFilters() != null && !query.aggregationFilters().isEmpty()) {
                                b.must(f.bool().with(fb -> {
                                    query.aggregationFilters().forEach(a -> {
                                        if (AGGREGATION_DATA_QUALITY.equals(a.name())) {
                                            DataQualityIssue.fromKey(a.value())
                                                    .ifPresent(issue -> applyDataQualityIssue(f, fb, issue));
                                            return;
                                        }
                                        final String fullKey = AGGREGATIONS.stream()
                                                .filter(key -> key.endsWith(AGGREGATION_COMPOSITE_DELIMITER + a.name()) || key.equals(a.name()))
                                                .findFirst()
                                                .orElseThrow(() -> new BadRequestException("Unknown facet %s".formatted(a.name())));

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
                                        } else if (FACET_SPEX_COUNTS.equals(a.name())) {
                                            fb.must(f.match().field(fieldPath).matching(Integer.valueOf(a.value())));
                                        } else {
                                            final Matcher range = YEAR_RANGE_PATTERN.matcher(a.value());

                                            if (range.matches() && YEAR_RANGE_FACETS.contains(a.name())) {
                                                fb.must(f.range().field(fieldPath).between(range.group(1), range.group(2)));
                                            } else {
                                                fb.must(f.match().field(fieldPath).matching(a.value()));
                                            }
                                        }
                                    });
                                }));
                            }
                        })
                )
                .aggregation(AggregationKey.of(AGGREGATION_DECEASED), f -> f.terms().field(AGGREGATION_DECEASED, Boolean.class))
                .aggregation(AggregationKey.of(AGGREGATION_PUBLISHED), f -> isAdmin ? f.terms().field(AGGREGATION_PUBLISHED, Boolean.class) : f.terms().field(AGGREGATION_DECEASED, Boolean.class))
                .aggregation(AggregationKey.of(AGGREGATION_SPEX_COUNT), f -> f.terms().field(FIELD_SPEX_COUNT, Integer.class));

        for (final String key : STRING_AGGREGATIONS) {
            final String fieldName = key.split(Pattern.quote(AGGREGATION_COMPOSITE_DELIMITER))[0];
            search = search.aggregation(AggregationKey.of(key), f -> f.terms().field(fieldName, String.class));
        }

        for (final String key : HIERARCHICAL_AGGREGATIONS) {
            final String fieldPrefix = key.split(Pattern.quote(AGGREGATION_COMPOSITE_DELIMITER))[0];
            search = search.aggregation(AggregationKey.of(key), f -> f.terms().field(fieldPrefix + LocaleContextHolder.getLocale(), String.class));
        }

        return search.sort(f -> determineSort(Spexare.class, f, sort))
                .fetch(offset, limit);
    }

    @Override
    public SearchResult<Spexare> search(final SearchSession searchSession, final SearchQuery query, final Pageable pageable) {
        return search(searchSession, query, (int) pageable.getOffset(), pageable.getPageSize(), pageable.getSort());
    }

    public long countByDataQualityIssue(final DataQualityIssue issue) {
        final boolean isAdmin = isAdministrator();

        return searchSession()
                .search(Spexare.class)
                .where(f -> f.bool().with(b -> {
                    b.must(f.matchAll());
                    if (!isAdmin) {
                        b.must(f.match().field(Spexare_.PUBLISHED).matching(true));
                    }
                    applyDataQualityIssue(f, b, issue);
                }))
                .fetchTotalHitCount();
    }

    private static void applyDataQualityIssue(final SearchPredicateFactory f, final BooleanPredicateOptionsCollector<?, ?> collector, final DataQualityIssue issue) {
        switch (issue) {
            case NO_ADDRESS -> collector.mustNot(f.exists().field(hierarchicalField(Spexare_.ADDRESSES)));
            case NO_EMAIL_ADDRESS ->
                    collector.mustNot(f.exists().field(String.join(ATTRIBUTE_DELIMITER, Spexare_.ADDRESSES, Address_.EMAIL_ADDRESS)));
            case NO_PHONE -> {
                collector.mustNot(f.exists().field(String.join(ATTRIBUTE_DELIMITER, Spexare_.ADDRESSES, Address_.PHONE)));
                collector.mustNot(f.exists().field(String.join(ATTRIBUTE_DELIMITER, Spexare_.ADDRESSES, Address_.PHONE_MOBILE)));
            }
            case NO_IMAGE -> collector.must(f.match().field(FIELD_HAS_IMAGE).matching(false));
            case NO_MEMBERSHIP -> collector.mustNot(f.exists().field(hierarchicalField(Spexare_.MEMBERSHIPS)));
            case NO_CONSENT -> collector.mustNot(f.exists().field(hierarchicalField(Spexare_.CONSENTS)));
            case NO_ACTIVITY ->
                    collector.mustNot(f.exists().field(String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.SPEX_ACTIVITY, SpexActivity_.SPEX, Spex_.YEAR)));
            case NO_SOCIAL_SECURITY_NUMBER -> collector.mustNot(f.exists().field(Spexare.INDEX_SOCIAL_SECURITY_NUMBER_HASH));
            case NO_TAG -> collector.mustNot(f.exists().field(hierarchicalField(Spexare_.TAGS)));
        }
    }

    private static String hierarchicalField(final String collection) {
        return String.join(ATTRIBUTE_DELIMITER, collection, AGGREGATION_HIERARCHICAL_MARKER + LocaleContextHolder.getLocale().getLanguage());
    }

}
