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
import nu.fgv.register.server.settings.Type_;
import nu.fgv.register.server.spex.SpexDetails_;
import nu.fgv.register.server.spex.Spex_;
import nu.fgv.register.server.spex.category.SpexCategory_;
import nu.fgv.register.server.spexare.activity.Activity_;
import nu.fgv.register.server.spexare.activity.spex.SpexActivity_;
import nu.fgv.register.server.spexare.activity.task.TaskActivity_;
import nu.fgv.register.server.spexare.activity.task.actor.Actor_;
import nu.fgv.register.server.spexare.address.Address_;
import nu.fgv.register.server.spexare.consent.Consent_;
import nu.fgv.register.server.spexare.membership.Membership_;
import nu.fgv.register.server.spexare.toggle.Toggle_;
import nu.fgv.register.server.tag.Tag_;
import nu.fgv.register.server.task.Task_;
import nu.fgv.register.server.task.category.TaskCategory_;
import nu.fgv.register.server.util.search.AbstractSearchEnabledJpaRepository;
import nu.fgv.register.server.util.security.SecurityUtil;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Transactional
public class SpexareSearchEnabledJpaRepository extends AbstractSearchEnabledJpaRepository<Spexare, Long> {

    private static final String ATTRIBUTE_DELIMITER = ".";
    private static final String[] FIELDS = new String[]{
            Spexare_.FIRST_NAME, Spexare_.LAST_NAME, Spexare_.NICK_NAME, Spexare_.SOCIAL_SECURITY_NUMBER, Spexare_.GRADUATION, Spexare_.COMMENT,
            String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.TASK_ACTIVITIES, TaskActivity_.ACTORS, Actor_.ROLE),
            String.join(ATTRIBUTE_DELIMITER, Spexare_.ADDRESSES, Address_.STREET_ADDRESS),
            String.join(ATTRIBUTE_DELIMITER, Spexare_.ADDRESSES, Address_.POSTAL_CODE),
            String.join(ATTRIBUTE_DELIMITER, Spexare_.ADDRESSES, Address_.CITY),
            String.join(ATTRIBUTE_DELIMITER, Spexare_.ADDRESSES, Address_.COUNTRY),
            String.join(ATTRIBUTE_DELIMITER, Spexare_.ADDRESSES, Address_.PHONE),
            String.join(ATTRIBUTE_DELIMITER, Spexare_.ADDRESSES, Address_.PHONE_MOBILE),
            String.join(ATTRIBUTE_DELIMITER, Spexare_.ADDRESSES, Address_.EMAIL_ADDRESS)
    };

    private static final String AGGREGATION_DECEASED = Spexare_.DECEASED;
    private static final String AGGREGATION_PUBLISHED = Spexare_.PUBLISHED;
    private static final String AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_YEAR = String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.SPEX_ACTIVITY, SpexActivity_.SPEX, Spex_.YEAR);
    private static final String AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_TITLE = String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.SPEX_ACTIVITY, SpexActivity_.SPEX, Spex_.DETAILS, SpexDetails_.TITLE);
    private static final String AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_CATEGORY_NAME = String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.SPEX_ACTIVITY, SpexActivity_.SPEX, Spex_.DETAILS, SpexDetails_.CATEGORY, SpexCategory_.NAME);
    private static final String AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_NAME = String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.TASK_ACTIVITIES, TaskActivity_.TASK, Task_.NAME);
    private static final String AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_CATEGORY_NAME = String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.TASK_ACTIVITIES, TaskActivity_.TASK, Task_.CATEGORY, TaskCategory_.NAME);
    private static final String AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_ACTORS_VOCAL_ID = String.join(ATTRIBUTE_DELIMITER, Spexare_.ACTIVITIES, Activity_.TASK_ACTIVITIES, TaskActivity_.ACTORS, Actor_.VOCAL, Type_.ID);
    private static final String AGGREGATION_TAGS_NAME = String.join(ATTRIBUTE_DELIMITER, Spexare_.TAGS, Tag_.NAME);
    private static final String AGGREGATION_MEMBERSHIPS_YEAR = String.join(ATTRIBUTE_DELIMITER, Spexare_.MEMBERSHIPS, Membership_.YEAR);
    private static final String AGGREGATION_MEMBERSHIPS_TYPE_ID = String.join(ATTRIBUTE_DELIMITER, Spexare_.MEMBERSHIPS, Membership_.TYPE, Type_.ID);
    private static final String AGGREGATION_CONSENTS_VALUE = String.join(ATTRIBUTE_DELIMITER, Spexare_.CONSENTS, Consent_.VALUE);
    private static final String AGGREGATION_CONSENTS_TYPE_ID = String.join(ATTRIBUTE_DELIMITER, Spexare_.CONSENTS, Consent_.TYPE, Type_.ID);
    private static final String AGGREGATION_TOGGLES_VALUE = String.join(ATTRIBUTE_DELIMITER, Spexare_.TOGGLES, Toggle_.VALUE);
    private static final String AGGREGATION_TOGGLES_TYPE_ID = String.join(ATTRIBUTE_DELIMITER, Spexare_.TOGGLES, Toggle_.TYPE, Type_.ID);

    static final List<String> AGGREGATIONS = List.of(
            AGGREGATION_DECEASED,
            AGGREGATION_PUBLISHED,
            AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_YEAR,
            AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_TITLE,
            AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_CATEGORY_NAME,
            AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_NAME,
            AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_CATEGORY_NAME,
            AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_ACTORS_VOCAL_ID,
            AGGREGATION_TAGS_NAME,
            AGGREGATION_MEMBERSHIPS_YEAR,
            AGGREGATION_MEMBERSHIPS_TYPE_ID,
            AGGREGATION_CONSENTS_VALUE,
            AGGREGATION_CONSENTS_TYPE_ID,
            AGGREGATION_TOGGLES_VALUE,
            AGGREGATION_TOGGLES_TYPE_ID
    );

    public SpexareSearchEnabledJpaRepository(final Class<Spexare> domainClass, final EntityManager entityManager) {
        super(domainClass, entityManager);
    }

    public SpexareSearchEnabledJpaRepository(final JpaEntityInformation<Spexare, Long> entityInformation, final EntityManager entityManager) {
        super(entityInformation, entityManager);
    }

    public SearchResult<Spexare> search(final SearchSession searchSession, final SearchQuery query, final Pageable pageable) {
        return searchSession
                .search(Spexare.class)
                .where(f -> f.bool().with(b -> {
                            if (hasText(query.freeTextQuery())) {
                                b.must(f.match().fields(FIELDS).matching(query.freeTextQuery()));
                            }
                            if (!isAdministrator()) {
                                b.must(f.match().field(Spexare_.PUBLISHED).matching(true));
                            }
                            query.aggregations().forEach(a -> b.must(f.match().field(a.name()).matching(a.value())));
                        })
                )
                .aggregation(AggregationKey.of(AGGREGATION_DECEASED), f -> f.terms().field(AGGREGATION_DECEASED, Boolean.class))
                .aggregation(AggregationKey.of(AGGREGATION_PUBLISHED), f -> f.terms().field(AGGREGATION_PUBLISHED, Boolean.class))
                .aggregation(AggregationKey.of(AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_YEAR), f -> f.terms().field(AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_YEAR, String.class))
                .aggregation(AggregationKey.of(AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_TITLE), f -> f.terms().field(AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_TITLE, String.class))
                .aggregation(AggregationKey.of(AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_CATEGORY_NAME), f -> f.terms().field(AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_CATEGORY_NAME, String.class))
                .aggregation(AggregationKey.of(AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_NAME), f -> f.terms().field(AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_NAME, String.class))
                .aggregation(AggregationKey.of(AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_CATEGORY_NAME), f -> f.terms().field(AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_CATEGORY_NAME, String.class))
                .aggregation(AggregationKey.of(AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_ACTORS_VOCAL_ID), f -> f.terms().field(AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_ACTORS_VOCAL_ID, String.class))
                .aggregation(AggregationKey.of(AGGREGATION_TAGS_NAME), f -> f.terms().field(AGGREGATION_TAGS_NAME, String.class))
                .aggregation(AggregationKey.of(AGGREGATION_MEMBERSHIPS_YEAR), f -> f.terms().field(AGGREGATION_MEMBERSHIPS_YEAR, String.class))
                .aggregation(AggregationKey.of(AGGREGATION_MEMBERSHIPS_TYPE_ID), f -> f.terms().field(AGGREGATION_MEMBERSHIPS_TYPE_ID, String.class))
                .aggregation(AggregationKey.of(AGGREGATION_CONSENTS_VALUE), f -> f.terms().field(AGGREGATION_CONSENTS_VALUE, Boolean.class))
                .aggregation(AggregationKey.of(AGGREGATION_CONSENTS_TYPE_ID), f -> f.terms().field(AGGREGATION_CONSENTS_TYPE_ID, String.class))
                .aggregation(AggregationKey.of(AGGREGATION_TOGGLES_VALUE), f -> f.terms().field(AGGREGATION_TOGGLES_VALUE, Boolean.class))
                .aggregation(AggregationKey.of(AGGREGATION_TOGGLES_TYPE_ID), f -> f.terms().field(AGGREGATION_TOGGLES_TYPE_ID, String.class))
                .sort(f -> determineSort(Spexare.class, f, pageable.getSort()))
                .fetch((int) pageable.getOffset(), pageable.getPageSize());
    }

    private static boolean isAdministrator() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(SecurityUtil.ROLE_ADMIN));
    }
}
