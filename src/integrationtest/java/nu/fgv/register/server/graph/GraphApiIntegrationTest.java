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
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.util.AbstractAuditable;
import nu.fgv.register.server.util.AbstractIntegrationTest;
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
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.ApiVersionInserter;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.jeasy.random.FieldPredicates.inClass;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class GraphApiIntegrationTest extends AbstractIntegrationTest {

    private final EasyRandom random;
    private final SpexareRepository spexareRepository;
    private final ActivityRepository activityRepository;
    private final SpexActivityRepository spexActivityRepository;
    private final SpexRepository spexRepository;
    private final SpexDetailsRepository spexDetailsRepository;
    private final SpexCategoryRepository spexCategoryRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public GraphApiIntegrationTest(final JdbcClient jdbcClient,
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
                                   final ObjectMapper objectMapper) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.spexareRepository = spexareRepository;
        this.activityRepository = activityRepository;
        this.spexActivityRepository = spexActivityRepository;
        this.spexRepository = spexRepository;
        this.spexDetailsRepository = spexDetailsRepository;
        this.spexCategoryRepository = spexCategoryRepository;

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
        restTestClient = RestTestClient
                .bindToServer()
                .baseUrl("http://localhost:%s/api/graph".formatted(localPort))
                .apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
                .build();

        // spexare.partner_id points back at spexare, so the rows cannot be deleted while any pair
        // is still linked.
        jdbcClient.sql("UPDATE spexare SET partner_id = NULL").update();

        JdbcTestUtils.deleteFromTables(jdbcClient, "spex_activity", "activity", "spexare", "spex", "spex_details", "spex_category",
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

            restTestClient
                    .get()
                    .uri("/search?q=lovel")
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("_embedded.graphNodes").isArray()
                    .jsonPath("_embedded.graphNodes.length()").isEqualTo(1)
                    .jsonPath("_embedded.graphNodes[0].label").isEqualTo("Ada Lovelace");
        }

        @Test
        void should_seed_a_random_person_when_the_term_is_blank() {
            persistReadableSpexare("Ada", "Lovelace", true);

            restTestClient
                    .get()
                    .uri("/search")
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("_embedded.graphNodes.length()").isEqualTo(1);
        }

        @Test
        void should_not_return_unpublished_spexare_to_user() {
            persistReadableSpexare("Grace", "Hopper", false);

            restTestClient
                    .get()
                    .uri("/search?q=hopper")
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("_embedded").doesNotExist();
        }

        @Test
        void should_return_401_when_not_authenticated() {
            restTestClient
                    .get()
                    .uri("/search?q=ada")
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
    }

    @Nested
    @DisplayName("Neighbourhood")
    class NeighbourhoodTests {

        @Test
        void should_return_the_spex_a_person_took_part_in() {
            final var spexare = persistReadableSpexare("Ada", "Lovelace", true);
            final var spex = persistReadableSpex();
            persistParticipation(spexare, spex);

            restTestClient
                    .get()
                    .uri("/SPEXARE/{id}", spexare.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("origin.label").isEqualTo("Ada Lovelace")
                    .jsonPath("groups[?(@.type == 'PARTICIPATION')].totalCount").isEqualTo(1)
                    .jsonPath("groups[?(@.type == 'PARTICIPATION')].nodes[0].type").isEqualTo("SPEX");
        }

        @Test
        void should_return_404_when_not_found() {
            restTestClient
                    .get()
                    .uri("/SPEXARE/{id}", 4711L)
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound();
        }

        /**
         * The same rule the GraphQL side is held to: traversing into a spex must not become a way to
         * enumerate people a plain user may not see.
         */
        @Test
        void should_not_leak_unpublished_spexare_when_expanding_a_spex() {
            final var published = persistReadableSpexare("Ada", "Lovelace", true);
            final var unpublished = persistReadableSpexare("Grace", "Hopper", false);
            final var spex = persistReadableSpex();
            persistParticipation(published, spex);
            persistParticipation(unpublished, spex);

            restTestClient
                    .get()
                    .uri("/SPEX/{id}", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("groups[?(@.type == 'PARTICIPATION')].totalCount").isEqualTo(1);
        }

        @Test
        void should_show_unpublished_spexare_to_editor_when_expanding_a_spex() {
            final var published = persistReadableSpexare("Ada", "Lovelace", true);
            final var unpublished = persistReadableSpexare("Grace", "Hopper", false);
            final var spex = persistReadableSpex();
            persistParticipation(published, spex);
            persistParticipation(unpublished, spex);

            restTestClient
                    .get()
                    .uri("/SPEX/{id}", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainEditorAccessToken())
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("groups[?(@.type == 'PARTICIPATION')].totalCount").isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Neighbours")
    class NeighboursTests {

        @Test
        void should_page_by_offset() {
            final var spex = persistReadableSpex();

            for (int i = 0; i < 5; i++) {
                persistParticipation(persistReadableSpexare("Person", String.valueOf(i), true), spex);
            }

            restTestClient
                    .get()
                    .uri("/SPEX/{id}/neighbours?edge=PARTICIPATION&offset=0&first=2", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("_embedded.graphNodes.length()").isEqualTo(2);

            restTestClient
                    .get()
                    .uri("/SPEX/{id}/neighbours?edge=PARTICIPATION&offset=4&first=2", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("_embedded.graphNodes.length()").isEqualTo(1);
        }

        @Test
        void should_return_zero_for_an_unrelated_edge() {
            final var spex = persistReadableSpex();

            restTestClient
                    .get()
                    .uri("/SPEX/{id}/neighbours?edge=TAG", spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("_embedded").doesNotExist();
        }
    }

}
