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
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;

import java.util.List;
import java.util.regex.Pattern;

import static nu.fgv.register.server.util.Constants.FACET_COMPOSITE_DELIMITER;
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
    private static final String AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_YEAR = String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.SPEX_ACTIVITY, SpexActivity_.SPEX, Spex_.YEAR) + FACET_COMPOSITE_DELIMITER + "spexYears";
    private static final String AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_TITLE = String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.SPEX_ACTIVITY, SpexActivity_.SPEX, Spex_.DETAILS, "hierarchical_") + FACET_COMPOSITE_DELIMITER + "spexTitles";
    private static final String AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_CATEGORY_NAME = String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.SPEX_ACTIVITY, SpexActivity_.SPEX, Spex_.DETAILS, SpexDetails_.CATEGORY, "hierarchical_") + FACET_COMPOSITE_DELIMITER + "spexCategoryNames";
    private static final String AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_NAME = String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.TASK_ACTIVITIES, TaskActivity_.TASK, "hierarchical_") + FACET_COMPOSITE_DELIMITER + "taskNames";
    private static final String AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_CATEGORY_NAME = String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.TASK_ACTIVITIES, TaskActivity_.TASK, Task_.CATEGORY, "hierarchical_") + FACET_COMPOSITE_DELIMITER + "taskCategoryNames";
    private static final String AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_ACTORS_VOCAL = String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.TASK_ACTIVITIES, TaskActivity_.ACTORS, "hierarchical_") + FACET_COMPOSITE_DELIMITER + "actorVocals";
    private static final String AGGREGATION_TAGS_NAME = String.join(ATTRIBUTE_DELIMITER, Spexare_.TAGS, "hierarchical_") + FACET_COMPOSITE_DELIMITER + "tags";
    private static final String AGGREGATION_MEMBERSHIPS = String.join(ATTRIBUTE_DELIMITER, Spexare_.MEMBERSHIPS, "hierarchical_") + FACET_COMPOSITE_DELIMITER + "memberships";
    private static final String AGGREGATION_CONSENTS = String.join(ATTRIBUTE_DELIMITER, Spexare_.CONSENTS, "hierarchical_") + FACET_COMPOSITE_DELIMITER + "consents";
    private static final String AGGREGATION_TOGGLES = String.join(ATTRIBUTE_DELIMITER, Spexare_.TOGGLES, "hierarchical_") + FACET_COMPOSITE_DELIMITER + "toggles";

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

    public SpexareSearchEnabledJpaRepository(final Class<Spexare> domainClass, final EntityManager entityManager) {
        super(domainClass, entityManager);
    }

    public SpexareSearchEnabledJpaRepository(final JpaEntityInformation<Spexare, Long> entityInformation, final EntityManager entityManager) {
        super(entityInformation, entityManager);
    }

    @Override
    public SearchResult<Spexare> search(final SearchSession searchSession, final SearchQuery query, final int offset, final int limit, final Sort sort) {
        final boolean isAdmin = isAdministrator();
        return searchSession
                .search(Spexare.class)
                .where(f -> f.bool().with(b -> {
                            if (hasText(query.freeTextQuery())) {
                                b.must(f.bool()
                                        .should(f.match().fields(PRIMARY_FUZZY_FIELDS).matching(query.freeTextQuery()).fuzzy().boost(10.0f))
                                        .should(f.match().fields(SECONDARY_FUZZY_FIELDS).matching(query.freeTextQuery()).fuzzy().boost(5.0f))
                                        .should(f.match().fields(EXACT_FIELDS).matching(query.freeTextQuery()).boost(5.0f))
                                        .should(f.match().fields(FUZZY_FIELDS).matching(query.freeTextQuery()).fuzzy().boost(1.0f))
                                );
                            } else {
                                b.must(f.matchAll());
                            }
                            if (!isAdmin) {
                                b.must(f.match().field(Spexare_.PUBLISHED).matching(true));
                            }
                            query.aggregations().forEach(a -> {
                                String fieldName = a.name();
                                if (fieldName.endsWith("_")) {
                                    fieldName += LocaleContextHolder.getLocale();
                                    b.must(f.wildcard().field(fieldName).matching(a.value() + FACET_COMPOSITE_DELIMITER + "*"));
                                } else {
                                    b.must(f.match().field(fieldName).matching(a.value()));
                                }
                            });
                        })
                )
                .aggregation(AggregationKey.of(AGGREGATION_DECEASED), f -> f.terms().field(AGGREGATION_DECEASED, Boolean.class))
                .aggregation(AggregationKey.of(AGGREGATION_PUBLISHED), f -> isAdmin ? f.terms().field(AGGREGATION_PUBLISHED, Boolean.class) : f.terms().field(AGGREGATION_DECEASED, Boolean.class)) // Defines a dummy if not admin to keep the chain valid
                .aggregation(AggregationKey.of(AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_YEAR), f -> {
                    final String fieldName = AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_YEAR.split(Pattern.quote(FACET_COMPOSITE_DELIMITER))[0];
                    return f.terms().field(fieldName, String.class);
                })
                .aggregation(AggregationKey.of(AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_TITLE), f -> {
                    final String fieldName = AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_TITLE.split(Pattern.quote(FACET_COMPOSITE_DELIMITER))[0];
                    return f.terms().field(fieldName + LocaleContextHolder.getLocale(), String.class);
                })
                .aggregation(AggregationKey.of(AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_CATEGORY_NAME), f -> {
                    final String fieldName = AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_CATEGORY_NAME.split(Pattern.quote(FACET_COMPOSITE_DELIMITER))[0];
                    return f.terms().field(fieldName + LocaleContextHolder.getLocale(), String.class);
                })
                .aggregation(AggregationKey.of(AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_NAME), f -> {
                    final String fieldName = AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_NAME.split(Pattern.quote(FACET_COMPOSITE_DELIMITER))[0];
                    return f.terms().field(fieldName + LocaleContextHolder.getLocale(), String.class);
                })
                .aggregation(AggregationKey.of(AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_CATEGORY_NAME), f -> {
                    final String fieldName = AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_CATEGORY_NAME.split(Pattern.quote(FACET_COMPOSITE_DELIMITER))[0];
                    return f.terms().field(fieldName + LocaleContextHolder.getLocale(), String.class);
                })
                .aggregation(AggregationKey.of(AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_ACTORS_VOCAL), f -> {
                    final String fieldName = AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_ACTORS_VOCAL.split(Pattern.quote(FACET_COMPOSITE_DELIMITER))[0];
                    return f.terms().field(fieldName + LocaleContextHolder.getLocale(), String.class);
                })
                .aggregation(AggregationKey.of(AGGREGATION_TAGS_NAME), f -> {
                    final String fieldName = AGGREGATION_TAGS_NAME.split(Pattern.quote(FACET_COMPOSITE_DELIMITER))[0];
                    return f.terms().field(fieldName + LocaleContextHolder.getLocale(), String.class);
                })
                .aggregation(AggregationKey.of(AGGREGATION_MEMBERSHIPS), f -> {
                    final String fieldName = AGGREGATION_MEMBERSHIPS.split(Pattern.quote(FACET_COMPOSITE_DELIMITER))[0];
                    return f.terms().field(fieldName + LocaleContextHolder.getLocale(), String.class);
                })
                .aggregation(AggregationKey.of(AGGREGATION_CONSENTS), f -> {
                    final String fieldName = AGGREGATION_CONSENTS.split(Pattern.quote(FACET_COMPOSITE_DELIMITER))[0];
                    return f.terms().field(fieldName + LocaleContextHolder.getLocale(), String.class);
                })
                .aggregation(AggregationKey.of(AGGREGATION_TOGGLES), f -> {
                    final String fieldName = AGGREGATION_TOGGLES.split(Pattern.quote(FACET_COMPOSITE_DELIMITER))[0];
                    return f.terms().field(fieldName + LocaleContextHolder.getLocale(), String.class);
                })
                .sort(f -> determineSort(Spexare.class, f, sort))
                .fetch(offset, limit);
    }

    @Override
    public SearchResult<Spexare> search(final SearchSession searchSession, final SearchQuery query, final Pageable pageable) {
        return search(searchSession, query, (int) pageable.getOffset(), pageable.getPageSize(), pageable.getSort());
    }

}
