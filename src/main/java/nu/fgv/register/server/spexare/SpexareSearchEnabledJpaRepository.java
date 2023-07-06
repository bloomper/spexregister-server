package nu.fgv.register.server.spexare;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import nu.fgv.register.server.util.search.AbstractSearchEnabledJpaRepository;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;

import java.util.List;

@Transactional
public class SpexareSearchEnabledJpaRepository extends AbstractSearchEnabledJpaRepository<Spexare, Long> {

    private static final String[] FIELDS = new String[]{
            "firstName", "lastName", "nickName", "socialSecurityNumber", "graduation", "comment",
            "activities.taskActivities.actors.role",
            "addresses.streetAddress", "addresses.postalCode", "addresses.city", "addresses.country", "addresses.phone", "addresses.phoneMobile", "addresses.emailAddress"
    };

    private static final String AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_YEAR = "activities.spexActivity.spex.year";
    private static final String AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_TITLE = "activities.spexActivity.spex.details.title";
    private static final String AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_CATEGORY_NAME = "activities.spexActivity.spex.details.category.name";
    private static final String AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_NAME = "activities.taskActivities.task.name";
    private static final String AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_CATEGORY_NAME = "activities.taskActivities.task.category.name";
    private static final String AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_ACTORS_VOCAL_ID = "activities.taskActivities.actors.vocal.id";
    private static final String AGGREGATION_TAGS_NAME = "tags.name";
    private static final String AGGREGATION_MEMBERSHIPS_YEAR = "memberships.year";
    private static final String AGGREGATION_MEMBERSHIPS_TYPE_ID = "memberships.type.id";
    private static final String AGGREGATION_CONSENTS_VALUE = "consents.value";
    private static final String AGGREGATION_CONSENTS_TYPE_ID = "consents.type.id";
    private static final String AGGREGATION_TOGGLES_VALUE = "toggles.value";
    private static final String AGGREGATION_TOGGLES_TYPE_ID = "toggles.type.id";

    static final List<String> AGGREGATIONS = List.of(
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

    public SearchResult<Spexare> getSearchResult(final SearchSession searchSession, final SearchQuery query, final Pageable pageable) {
        return searchSession
                .search(Spexare.class)
                .where(f -> f.bool(b -> {
                            if (query.freeTextQuery() != null) {
                                b.must(f.match().fields(FIELDS).matching(query.freeTextQuery()));
                            }
                            query.aggregations().forEach(a -> b.must(f.match().field(a.name()).matching(a.value())));
                        })
                )
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
                .fetch(Long.valueOf(pageable.getOffset()).intValue(), pageable.getPageSize());
    }
}
