package nu.fgv.register.server.spexare;

import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.SpexCategory;
import nu.fgv.register.server.spex.SpexDetails;
import nu.fgv.register.server.spexare.consent.Consent;
import nu.fgv.register.server.spexare.membership.Membership;
import nu.fgv.register.server.spexare.toggle.Toggle;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.task.Task;
import nu.fgv.register.server.task.TaskCategory;
import nu.fgv.register.server.util.search.SearchEnabledJpaRepository;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpexareRepository extends SearchEnabledJpaRepository<Spexare, Long>, QuerydslPredicateExecutor<Spexare> {

    String[] FIELDS = new String[]{
            "firstName", "lastName", "nickName", "socialSecurityNumber", "graduation", "comment",
            "activities.taskActivities.actors.role",
            "addresses.streetAddress", "addresses.postalCode", "addresses.city", "addresses.country", "addresses.phone", "addresses.phoneMobile", "addresses.emailAddress"
    };

    String AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_YEAR = "activities.spexActivity.spex.year";
    String AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_TITLE = "activities.spexActivity.spex.details.title";
    String AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_CATEGORY_NAME = "activities.spexActivity.spex.details.category.name";
    String AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_NAME = "activities.taskActivities.task.name";
    String AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_CATEGORY_NAME = "activities.taskActivities.task.category.name";
    String AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_ACTORS_VOCAL_ID = "activities.taskActivities.actors.vocal.id";
    String AGGREGATION_TAGS_NAME = "tags.name";
    String AGGREGATION_MEMBERSHIPS_YEAR = "memberships.year";
    String AGGREGATION_MEMBERSHIPS_TYPE_ID = "memberships.type.id";
    String AGGREGATION_CONSENTS_VALUE = "consents.value";
    String AGGREGATION_CONSENTS_TYPE_ID = "consents.type.id";
    String AGGREGATION_TOGGLES_VALUE = "toggles.value";
    String AGGREGATION_TOGGLES_TYPE_ID = "toggles.type.id";

    List<String> AGGREGATIONS = List.of(
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

    @Query("""
              SELECT s FROM Spexare s
              WHERE s.id IN :ids
            """)
    List<Spexare> findByIds(@Param("ids") List<Long> ids, Sort sort);

    default SearchResult<Spexare> getSearchResult(final SearchSession searchSession, final SearchQuery query, final Pageable pageable) {
        return searchSession
                .search(Spexare.class)
                .where(f -> f.bool(b -> {
                            if (query.freeTextQuery() != null) {
                                b.must(f.match().fields(FIELDS).matching(query.freeTextQuery()));
                            }
                            query.aggregations().forEach(a -> b.must(f.match().field(a.name()).matching(a.value())));
                        })
                )
                .aggregation(AggregationKey.of(AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_YEAR), f -> f.terms().field(AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_YEAR, Spex.class))
                .aggregation(AggregationKey.of(AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_TITLE), f -> f.terms().field(AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_TITLE, SpexDetails.class))
                .aggregation(AggregationKey.of(AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_CATEGORY_NAME), f -> f.terms().field(AGGREGATION_ACTIVITIES_SPEX_ACTIVITY_SPEX_DETAILS_CATEGORY_NAME, SpexCategory.class))
                .aggregation(AggregationKey.of(AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_NAME), f -> f.terms().field(AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_NAME, Task.class))
                .aggregation(AggregationKey.of(AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_CATEGORY_NAME), f -> f.terms().field(AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_TASK_CATEGORY_NAME, TaskCategory.class))
                .aggregation(AggregationKey.of(AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_ACTORS_VOCAL_ID), f -> f.terms().field(AGGREGATION_ACTIVITIES_TASK_ACTIVITIES_ACTORS_VOCAL_ID, Type.class))
                .aggregation(AggregationKey.of(AGGREGATION_TAGS_NAME), f -> f.terms().field(AGGREGATION_TAGS_NAME, Tag.class))
                .aggregation(AggregationKey.of(AGGREGATION_MEMBERSHIPS_YEAR), f -> f.terms().field(AGGREGATION_MEMBERSHIPS_YEAR, Membership.class))
                .aggregation(AggregationKey.of(AGGREGATION_MEMBERSHIPS_TYPE_ID), f -> f.terms().field(AGGREGATION_MEMBERSHIPS_TYPE_ID, Type.class))
                .aggregation(AggregationKey.of(AGGREGATION_CONSENTS_VALUE), f -> f.terms().field(AGGREGATION_CONSENTS_VALUE, Consent.class))
                .aggregation(AggregationKey.of(AGGREGATION_CONSENTS_TYPE_ID), f -> f.terms().field(AGGREGATION_CONSENTS_TYPE_ID, Type.class))
                .aggregation(AggregationKey.of(AGGREGATION_TOGGLES_VALUE), f -> f.terms().field(AGGREGATION_TOGGLES_VALUE, Toggle.class))
                .aggregation(AggregationKey.of(AGGREGATION_TOGGLES_TYPE_ID), f -> f.terms().field(AGGREGATION_TOGGLES_TYPE_ID, Type.class))
                .sort(f -> determineSort(Spexare.class, f, pageable.getSort()))
                .fetch(Long.valueOf(pageable.getOffset()).intValue(), pageable.getPageSize());
    }

}
