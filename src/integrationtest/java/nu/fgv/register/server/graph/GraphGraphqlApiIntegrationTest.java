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

package nu.fgv.register.server.graph;

import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.SpexDetails;
import nu.fgv.register.server.spex.SpexDetailsRepository;
import nu.fgv.register.server.spex.SpexRepository;
import nu.fgv.register.server.spex.category.SpexCategory;
import nu.fgv.register.server.spex.category.SpexCategoryRepository;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.activity.Activity;
import nu.fgv.register.server.spexare.activity.ActivityRepository;
import nu.fgv.register.server.spexare.activity.spex.SpexActivity;
import nu.fgv.register.server.spexare.activity.spex.SpexActivityRepository;
import nu.fgv.register.server.task.Task;
import nu.fgv.register.server.task.TaskRepository;
import nu.fgv.register.server.task.category.TaskCategory;
import nu.fgv.register.server.task.category.TaskCategoryRepository;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.user.UserRepository;
import nu.fgv.register.server.user.state.State;
import nu.fgv.register.server.user.state.StateRepository;
import nu.fgv.register.server.util.AbstractAuditable;
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import nu.fgv.register.server.util.randomizer.SocialSecurityNumberRandomizer;
import nu.fgv.register.server.util.randomizer.YearRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.inClass;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class GraphGraphqlApiIntegrationTest extends AbstractGraphqlIntegrationTest {

    private final EasyRandom random;
    private final SpexareRepository spexareRepository;
    private final ActivityRepository activityRepository;
    private final SpexActivityRepository spexActivityRepository;
    private final SpexRepository spexRepository;
    private final SpexDetailsRepository spexDetailsRepository;
    private final SpexCategoryRepository spexCategoryRepository;
    private final TaskRepository taskRepository;
    private final TaskCategoryRepository taskCategoryRepository;
    private final UserRepository userRepository;
    private final StateRepository stateRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public GraphGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
                                          final AclCache aclCache,
                                          final Keycloak keycloakAdminClient,
                                          final String keycloakClientId,
                                          final PermissionService permissionService,
                                          final SpexareRepository spexareRepository,
                                          final ActivityRepository activityRepository,
                                          final SpexActivityRepository spexActivityRepository,
                                          final SpexRepository spexRepository,
                                          final SpexDetailsRepository spexDetailsRepository,
                                          final SpexCategoryRepository spexCategoryRepository,
                                          final TaskRepository taskRepository,
                                          final TaskCategoryRepository taskCategoryRepository,
                                          final UserRepository userRepository,
                                          final StateRepository stateRepository,
                                          final ObjectMapper objectMapper) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.spexareRepository = spexareRepository;
        this.activityRepository = activityRepository;
        this.spexActivityRepository = spexActivityRepository;
        this.spexRepository = spexRepository;
        this.spexDetailsRepository = spexDetailsRepository;
        this.spexCategoryRepository = spexCategoryRepository;
        this.taskRepository = taskRepository;
        this.taskCategoryRepository = taskCategoryRepository;
        this.userRepository = userRepository;
        this.stateRepository = stateRepository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

        parameters
                .randomize(named("year"), new YearRandomizer())
                .randomize(named("firstYear"), new YearRandomizer())
                .randomize(named("socialSecurityNumber"), new SocialSecurityNumberRandomizer())
                .excludeField(named("partner").and(ofType(Spexare.class)).and(inClass(Spexare.class)))
                .excludeField(named("user").and(ofType(User.class)).and(inClass(Spexare.class)))
                .excludeField(named("tags").and(ofType(Set.class)).and(inClass(Spexare.class)))
                .excludeField(named("activities").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("memberships").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("consents").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("toggles").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("spexActivity").and(ofType(SpexActivity.class)).and(inClass(Activity.class)))
                .excludeField(named("taskActivities").and(ofType(Set.class)).and(inClass(Activity.class)))
                .excludeField(named("version").and(ofType(Long.class)).and(inClass(AbstractAuditable.class)))
                .randomizationDepth(1);
        random = new EasyRandom(parameters);
    }

    @BeforeEach
    void setUp() {
        httpGraphQlTester = HttpGraphQlTester.create(
                WebTestClient.bindToServer()
                        .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath))
                        .build()
        );

        jdbcClient.sql("UPDATE spexare SET partner_id = NULL").update();

        JdbcTestUtils.deleteFromTables(jdbcClient, "user_audit", "user", "spex_activity", "activity", "spexare", "spex", "spex_details", "spex_category",
                "task", "task_category", "task_audit", "task_category_audit",
                "spex_activity_audit", "activity_audit", "spexare_audit", "spex_audit", "spex_details_audit", "spex_category_audit");
    }

    private Spexare persistReadableSpexare(final String firstName, final String lastName, final boolean published) {
        final var spexare = random.nextObject(Spexare.class);

        spexare.setId(null);
        spexare.setFirstName(firstName);
        spexare.setLastName(lastName);
        spexare.setPublished(published);

        final var saved = spexareRepository.save(spexare);
        final var oid = toObjectIdentity(Spexare.class, saved.getId());

        // Mirrors SpexareService.create: the user role only gets READ on published people.
        grantReadPermissionToRoleAdmin(oid);
        grantReadPermissionToRoleEditor(oid);

        if (published) {
            grantReadPermissionToRoleUser(oid);
        }

        return saved;
    }

    /**
     * Points the register's own user row at the Keycloak account the test authenticates as.
     */
    private void linkToTestUser(final Spexare spexare) {
        final var externalId = keycloakAdminClient.realm(keycloakRealm).users().search(TEST_USER).getFirst().getId();
        final var user = new User();

        user.setExternalId(externalId);
        user.setState(stateRepository.save(random.nextObject(State.class)));
        user.setSpexare(spexare);

        userRepository.save(user);
    }

    private Task persistReadableTask(final String name, final String categoryName) {
        final var category = random.nextObject(TaskCategory.class);

        category.setId(null);
        category.setName(categoryName);

        final var savedCategory = taskCategoryRepository.save(category);

        grantReadPermissionToRoleUser(toObjectIdentity(TaskCategory.class, savedCategory.getId()));

        final var task = random.nextObject(Task.class);

        task.setId(null);
        task.setName(name);
        task.setCategory(savedCategory);

        final var saved = taskRepository.save(task);

        grantReadPermissionToRoleUser(toObjectIdentity(Task.class, saved.getId()));

        return saved;
    }

    private Spex persistReadableSpex() {
        final var category = random.nextObject(SpexCategory.class);

        category.setId(null);

        final var savedCategory = spexCategoryRepository.save(category);

        grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, savedCategory.getId()));
        grantReadPermissionToRoleEditor(toObjectIdentity(SpexCategory.class, savedCategory.getId()));
        grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, savedCategory.getId()));

        final var details = random.nextObject(SpexDetails.class);

        details.setId(null);
        details.setCategory(savedCategory);

        final var spex = random.nextObject(Spex.class);

        spex.setId(null);
        spex.setParent(null);
        spex.setDetails(spexDetailsRepository.save(details));

        final var saved = spexRepository.save(spex);
        final var oid = toObjectIdentity(Spex.class, saved.getId());

        grantReadPermissionToRoleAdmin(oid);
        grantReadPermissionToRoleEditor(oid);
        grantReadPermissionToRoleUser(oid);

        return saved;
    }

    private void persistParticipation(final Spexare spexare, final Spex spex) {
        final var activity = random.nextObject(Activity.class);

        activity.setId(null);
        activity.setSpexare(spexare);
        activity.setSpexActivity(null);

        final var savedActivity = activityRepository.save(activity);
        final var spexActivity = random.nextObject(SpexActivity.class);

        spexActivity.setId(null);
        spexActivity.setActivity(savedActivity);
        spexActivity.setSpex(spex);

        spexActivityRepository.save(spexActivity);
    }

    @Nested
    @DisplayName("Search")
    class SearchTests {

        @Test
        void should_return_matching_spexare() {
            persistReadableSpexare("Ada", "Lovelace", true);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("graph/graphSearch")
                    .variable("q", "lovel")
                    .execute()
                    .errors()
                    .verify()
                    .path("graphSearch")
                    .entityList(GraphNodeDto.class)
                    .hasSize(1);
        }

        @Test
        void should_return_zero_when_no_match() {
            persistReadableSpexare("Ada", "Lovelace", true);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("graph/graphSearch")
                    .variable("q", "zzzz")
                    .execute()
                    .errors()
                    .verify()
                    .path("graphSearch")
                    .entityList(GraphNodeDto.class)
                    .hasSize(0);
        }

        @Test
        void should_not_return_unpublished_spexare_to_user() {
            persistReadableSpexare("Grace", "Hopper", false);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("graph/graphSearch")
                    .variable("q", "hopper")
                    .execute()
                    .errors()
                    .verify()
                    .path("graphSearch")
                    .entityList(GraphNodeDto.class)
                    .hasSize(0);
        }

        @Test
        void should_seed_a_random_person_when_the_term_is_blank() {
            persistReadableSpexare("Ada", "Lovelace", true);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("graph/graphSearch")
                    .variable("q", "  ")
                    .execute()
                    .errors()
                    .verify()
                    .path("graphSearch")
                    .entityList(GraphNodeDto.class)
                    .hasSize(1);
        }

        /**
         * A blank term is "take me somewhere": your own entry if the account is linked to one, a
         * random person otherwise.
         */
        @Test
        void should_seed_the_current_users_own_spexare_when_linked() {
            // A crowd to pick from, so a fallback to random would show up as a failure rather than
            // passing half the time, and the search is repeated to make that near-certain.
            for (int i = 0; i < 10; i++) {
                persistReadableSpexare("Someone", String.valueOf(i), true);
            }

            final var mine = persistReadableSpexare("Grace", "Hopper", true);

            linkToTestUser(mine);

            for (int attempt = 0; attempt < 5; attempt++) {
                httpGraphQlTester
                        .mutate()
                        .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                        .build()
                        .documentName("graph/graphSearch")
                        .variable("q", "")
                        .execute()
                        .errors()
                        .verify()
                        .path("graphSearch").entityList(GraphNodeDto.class).hasSize(1)
                        .path("graphSearch[0].label").entity(String.class).isEqualTo("Grace Hopper");
            }
        }

        /**
         * Being linked to an entry you are not allowed to read must not break the way in; it falls
         * back to a random person like any unlinked account.
         */
        @Test
        void should_fall_back_to_random_when_the_own_spexare_is_not_readable() {
            final var readable = persistReadableSpexare("Ada", "Lovelace", true);
            final var mine = persistReadableSpexare("Grace", "Hopper", false);

            linkToTestUser(mine);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("graph/graphSearch")
                    .variable("q", "")
                    .execute()
                    .errors()
                    .verify()
                    .path("graphSearch").entityList(GraphNodeDto.class).hasSize(1)
                    .path("graphSearch[0].entityId").entity(String.class).isEqualTo(String.valueOf(readable.getId()));
        }

        @Test
        void should_not_seed_an_unpublished_person_at_random() {
            persistReadableSpexare("Grace", "Hopper", false);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("graph/graphSearch")
                    .variable("q", "")
                    .execute()
                    .errors()
                    .verify()
                    .path("graphSearch")
                    .entityList(GraphNodeDto.class)
                    .hasSize(0);
        }

        @Test
        void should_return_FORBIDDEN_when_not_authenticated() {
            httpGraphQlTester
                    .documentName("graph/graphSearch")
                    .variable("q", "ada")
                    .execute()
                    .errors()
                    .satisfy(errors -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.UNAUTHORIZED.toString())
                                    || error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    );
        }
    }

    @Nested
    @DisplayName("Neighbourhood")
    class NeighbourhoodTests {

        @Test
        void should_return_participation_for_spexare() {
            final var spexare = persistReadableSpexare("Ada", "Lovelace", true);
            final var spex = persistReadableSpex();
            persistParticipation(spexare, spex);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("graph/graphNeighbourhood")
                    .variable("type", GraphNodeType.SPEXARE)
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("graphNeighbourhood.origin.label").entity(String.class).isEqualTo("Ada Lovelace")
                    .path("graphNeighbourhood.groups[?(@.type == 'PARTICIPATION')].totalCount").entityList(Integer.class).containsExactly(1);
        }

        /**
         * The partner column lives on one side of the pair only, so the side without it would
         * otherwise show no partner at all.
         */
        @Test
        void should_return_the_partner_from_either_side() {
            final var ada = persistReadableSpexare("Ada", "Lovelace", true);
            final var grace = persistReadableSpexare("Grace", "Hopper", true);

            grace.setPartner(ada);
            spexareRepository.save(grace);

            for (final var spexare : List.of(ada, grace)) {
                httpGraphQlTester
                        .mutate()
                        .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                        .build()
                        .documentName("graph/graphNeighbourhood")
                        .variable("type", GraphNodeType.SPEXARE)
                        .variable("id", spexare.getId())
                        .execute()
                        .errors()
                        .verify()
                        .path("graphNeighbourhood.groups[?(@.type == 'PARTNER')].totalCount")
                        .entityList(Integer.class).containsExactly(1);
            }
        }

        @Test
        void should_omit_an_unreadable_partner_rather_than_deny() {
            final var ada = persistReadableSpexare("Ada", "Lovelace", true);
            final var grace = persistReadableSpexare("Grace", "Hopper", false);

            ada.setPartner(grace);
            grace.setPartner(ada);
            spexareRepository.save(ada);
            spexareRepository.save(grace);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("graph/graphNeighbourhood")
                    .variable("type", GraphNodeType.SPEXARE)
                    .variable("id", ada.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("graphNeighbourhood.groups[?(@.type == 'PARTNER')].totalCount")
                    .entityList(Integer.class).containsExactly(0);
        }

        @Test
        void should_return_null_when_not_found() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("graph/graphNeighbourhood")
                    .variable("type", GraphNodeType.SPEXARE)
                    .variable("id", 4711L)
                    .execute()
                    .errors()
                    .verify()
                    .path("graphNeighbourhood").valueIsNull();
        }

        /**
         * The regression that matters: traversing into a spex must not become a way to enumerate
         * people a plain user is not allowed to see.
         */
        @Test
        void should_not_leak_unpublished_spexare_when_expanding_a_spex() {
            final var published = persistReadableSpexare("Ada", "Lovelace", true);
            final var unpublished = persistReadableSpexare("Grace", "Hopper", false);
            final var spex = persistReadableSpex();
            persistParticipation(published, spex);
            persistParticipation(unpublished, spex);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("graph/graphNeighbourhood")
                    .variable("type", GraphNodeType.SPEX)
                    .variable("id", spex.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("graphNeighbourhood.groups[?(@.type == 'PARTICIPATION')].totalCount").entityList(Integer.class).containsExactly(1);
        }

        @Test
        void should_show_unpublished_spexare_to_editor_when_expanding_a_spex() {
            final var published = persistReadableSpexare("Ada", "Lovelace", true);
            final var unpublished = persistReadableSpexare("Grace", "Hopper", false);
            final var spex = persistReadableSpex();
            persistParticipation(published, spex);
            persistParticipation(unpublished, spex);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainEditorAccessToken()))
                    .build()
                    .documentName("graph/graphNeighbourhood")
                    .variable("type", GraphNodeType.SPEX)
                    .variable("id", spex.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("graphNeighbourhood.groups[?(@.type == 'PARTICIPATION')].totalCount").entityList(Integer.class).containsExactly(2);
        }

        @Test
        void should_cap_returned_nodes_but_report_the_true_total() {
            final var spex = persistReadableSpex();

            for (int i = 0; i < 5; i++) {
                persistParticipation(persistReadableSpexare("Person", String.valueOf(i), true), spex);
            }

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("graph/graphNeighbourhood")
                    .variable("type", GraphNodeType.SPEX)
                    .variable("id", spex.getId())
                    .variable("first", 2)
                    .execute()
                    .errors()
                    .verify()
                    .path("graphNeighbourhood.groups[?(@.type == 'PARTICIPATION')].totalCount").entityList(Integer.class).containsExactly(5)
                    .path("graphNeighbourhood.groups[?(@.type == 'PARTICIPATION')].nodes[*].id").entityList(String.class).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Task")
    class TaskTests {

        @Test
        void should_return_the_category_of_a_function() {
            final var task = persistReadableTask("Skadespelare", "Skadespeleri");

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("graph/graphNeighbourhood")
                    .variable("type", GraphNodeType.TASK)
                    .variable("id", task.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("graphNeighbourhood.groups[?(@.type == 'CATEGORY')].totalCount")
                    .entityList(Integer.class).containsExactly(1)
                    .path("graphNeighbourhood.groups[?(@.type == 'CATEGORY')].nodes[0].label")
                    .entityList(String.class).containsExactly("Skadespeleri");
        }
    }

    @Nested
    @DisplayName("Neighbours paged")
    class NeighboursPagedTests {

        @Test
        void should_walk_the_remainder_of_a_capped_group() {
            final var spex = persistReadableSpex();

            for (int i = 0; i < 5; i++) {
                persistParticipation(persistReadableSpexare("Person", String.valueOf(i), true), spex);
            }

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("graph/graphNeighboursPaged")
                    .variable("type", GraphNodeType.SPEX)
                    .variable("id", spex.getId())
                    .variable("edge", GraphEdgeType.PARTICIPATION)
                    .variable("first", 2)
                    .execute()
                    .errors()
                    .verify()
                    .path("graphNeighboursPaged.totalCount").entity(Integer.class).isEqualTo(5)
                    .path("graphNeighboursPaged.edges[*].node.id").entityList(String.class).hasSize(2)
                    .path("graphNeighboursPaged.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(true);
        }

        @Test
        void should_return_zero_for_an_unrelated_edge() {
            final var spex = persistReadableSpex();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                    .build()
                    .documentName("graph/graphNeighboursPaged")
                    .variable("type", GraphNodeType.SPEX)
                    .variable("id", spex.getId())
                    .variable("edge", GraphEdgeType.TAG)
                    .variable("first", 10)
                    .execute()
                    .errors()
                    .verify()
                    .path("graphNeighboursPaged.totalCount").entity(Integer.class).isEqualTo(0);
        }
    }

}
