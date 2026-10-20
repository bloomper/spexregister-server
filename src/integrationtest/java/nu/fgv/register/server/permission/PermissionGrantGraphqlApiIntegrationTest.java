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

package nu.fgv.register.server.permission;

import nu.fgv.register.server.security.policy.Permission;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.SpexDetails;
import nu.fgv.register.server.spex.SpexDetailsRepository;
import nu.fgv.register.server.spex.SpexRepository;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.activity.Activity;
import nu.fgv.register.server.spexare.activity.ActivityRepository;
import nu.fgv.register.server.spexare.activity.spex.SpexActivity;
import nu.fgv.register.server.spexare.activity.spex.SpexActivityRepository;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.user.UserRepository;
import nu.fgv.register.server.user.state.StateRepository;
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Admins grant users rights on one spexare, everyone in a spex, or one spex; the grantee then has those rights and no
 * more.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
class PermissionGrantGraphqlApiIntegrationTest extends AbstractGraphqlIntegrationTest {

    private static final String CREATE = """
            mutation create($input: PermissionGrantCreate!) {
                permissionGrantCreate(input: $input) { id permission scope expired user { id } spexare { id } spex { id } }
            }""";
    private static final String UPDATE_SPEXARE = """
            mutation update($id: ID!, $firstName: String!) {
                spexareUpdate(input: { id: $id, firstName: $firstName, lastName: "Behörig", deceased: false, published: false }) { id firstName }
            }""";

    private final PermissionGrantRepository grantRepository;
    private final UserRepository userRepository;
    private final StateRepository stateRepository;
    private final SpexareRepository spexareRepository;
    private final SpexRepository spexRepository;
    private final SpexDetailsRepository spexDetailsRepository;
    private final ActivityRepository activityRepository;
    private final SpexActivityRepository spexActivityRepository;
    private User grantee;
    private Spex spex;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public PermissionGrantGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
                                                    final Keycloak keycloakAdminClient,
                                                    final String keycloakClientId,
                                                    final ObjectMapper objectMapper,
                                                    final PermissionGrantRepository grantRepository,
                                                    final UserRepository userRepository,
                                                    final StateRepository stateRepository,
                                                    final SpexareRepository spexareRepository,
                                                    final SpexRepository spexRepository,
                                                    final SpexDetailsRepository spexDetailsRepository,
                                                    final ActivityRepository activityRepository,
                                                    final SpexActivityRepository spexActivityRepository) {
        super(jdbcClient, keycloakAdminClient, keycloakClientId, objectMapper);
        this.grantRepository = grantRepository;
        this.userRepository = userRepository;
        this.stateRepository = stateRepository;
        this.spexareRepository = spexareRepository;
        this.spexRepository = spexRepository;
        this.spexDetailsRepository = spexDetailsRepository;
        this.activityRepository = activityRepository;
        this.spexActivityRepository = spexActivityRepository;
    }

    @BeforeEach
    void setUp() {
        httpGraphQlTester = HttpGraphQlTester.create(
                WebTestClient.bindToServer()
                        .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath))
                        .build()
        );

        jdbcClient.sql("UPDATE spex SET parent_id = NULL").update();
        JdbcTestUtils.deleteFromTables(jdbcClient, "permission_grant", "permission_grant_audit", "user_audit", "user",
                "spex_activity", "activity", "spexare", "spex", "spex_details",
                "spex_activity_audit", "activity_audit", "spexare_audit", "spex_audit", "spex_details_audit");

        final User user = new User();
        user.setExternalId(keycloakAdminClient.realm(keycloakRealm).users().search(TEST_USER).getFirst().getId());
        user.setState(stateRepository.findAll().getFirst());
        grantee = userRepository.save(user);

        final SpexDetails details = new SpexDetails();
        details.setTitle("Ali Baba");
        final Spex aliBaba = new Spex();
        aliBaba.setYear("1956");
        aliBaba.setDetails(spexDetailsRepository.save(details));
        spex = spexRepository.save(aliBaba);
    }

    @Test
    void should_let_only_admins_manage_grants() {
        final Spexare spexare = spexare("Petra", false);
        final Map<String, Object> input = Map.of("userId", grantee.getId(), "permission", "READ", "scope", "SPEXARE", "spexareId", spexare.getId());

        for (final GraphQlTester tester : new GraphQlTester[]{as(obtainEditorAccessToken()), as(obtainUserAccessToken())}) {
            tester.document(CREATE).variable("input", input).execute().errors()
                    .satisfy(errors -> assertThat(errors).anyMatch(this::isForbidden));
            tester.document("query { permissionGrantPaged(first: 10) { totalCount } }").execute().errors()
                    .satisfy(errors -> assertThat(errors).anyMatch(this::isForbidden));
        }

        final String id = as(obtainAdminAccessToken()).document(CREATE).variable("input", input).execute().errors().verify()
                .path("permissionGrantCreate.user.id").entity(Long.class).isEqualTo(grantee.getId())
                .path("permissionGrantCreate.expired").entity(Boolean.class).isEqualTo(false)
                .path("permissionGrantCreate.id").entity(String.class).get();

        as(obtainAdminAccessToken())
                .document("mutation u($id: ID!) { permissionGrantUpdate(input: { id: $id, permission: WRITE }) { permission } }")
                .variable("id", id).execute().errors().verify()
                .path("permissionGrantUpdate.permission").entity(String.class).isEqualTo("WRITE");

        as(obtainAdminAccessToken())
                .document("query { permissionGrantPaged(first: 10, filter: \"scope:SPEXARE\") { totalCount } }")
                .execute().errors().verify()
                .path("permissionGrantPaged.totalCount").entity(Integer.class).isEqualTo(1);

        as(obtainAdminAccessToken())
                .document("mutation d($id: ID!) { permissionGrantDelete(id: $id) }")
                .variable("id", id).execute().errors().verify();

        assertThat(grantRepository.count()).isZero();
        as(obtainAdminAccessToken())
                .document("query r($id: ID!) { revisions(type: PERMISSION_GRANT, id: $id) { revisionType } }")
                .variable("id", id).execute().errors().verify()
                .path("revisions[*].revisionType").entityList(String.class).hasSize(3);
    }

    @Test
    void should_refuse_invalid_grants() {
        final Spexare spexare = spexare("Petra", false);

        for (final Map<String, Object> input : new Map[]{
                Map.of("userId", grantee.getId(), "role", "USER", "permission", "READ", "scope", "SPEXARE", "spexareId", spexare.getId()),
                Map.of("permission", "READ", "scope", "SPEXARE", "spexareId", spexare.getId()),
                Map.of("role", "REVIVAL", "permission", "READ", "scope", "SPEX", "spexId", spex.getId()),
                Map.of("userId", grantee.getId(), "permission", "READ", "scope", "SPEX", "spexareId", spexare.getId()),
                Map.of("userId", grantee.getId(), "permission", "READ", "scope", "SPEXARE", "spexareId", spexare.getId(),
                        "validUntil", LocalDate.now().minusDays(1).toString())
        }) {
            as(obtainAdminAccessToken()).document(CREATE).variable("input", input).execute().errors()
                    .satisfy(errors -> assertThat(errors).anyMatch(error -> ErrorType.BAD_REQUEST.toString().equals(classification(error))));
        }
        assertThat(grantRepository.count()).isZero();
    }

    @Test
    void should_let_a_user_edit_everyone_in_a_spex_they_were_granted() {
        final Spexare participant = participant("Petra");
        final Spexare outsider = spexare("Olle", false);
        grant(Permission.WRITE, PermissionGrantScope.SPEX_PARTICIPANTS, null, spex, null);

        updateSpexare(participant, true);
        updateSpexare(outsider, false);

        final Spexare newcomer = participant("Nina");
        updateSpexare(newcomer, true);

        as(obtainUserAccessToken())
                .document("query s($id: ID!) { spexare(id: $id) { permissions { read write delete administer } } }")
                .variable("id", participant.getId()).execute().errors().verify()
                .path("spexare.permissions.write").entity(Boolean.class).isEqualTo(true)
                .path("spexare.permissions.delete").entity(Boolean.class).isEqualTo(false);
    }

    @Test
    void should_let_a_user_find_one_unpublished_spexare_they_may_read() {
        final Spexare readable = spexare("Petra", false);
        spexare("Olle", false);
        spexare("Pål", true);
        grant(Permission.READ, PermissionGrantScope.SPEXARE, readable, null, null);

        as(obtainUserAccessToken())
                .document("query { spexarePaged(first: 10, filter: \"\") { totalCount } }")
                .execute().errors().verify()
                .path("spexarePaged.totalCount").entity(Integer.class).isEqualTo(2);

        as(obtainUserAccessToken())
                .document("query { spexareSearchPaged(first: 10, q: \"\") { totalCount } }")
                .execute().errors().verify()
                .path("spexareSearchPaged.totalCount").entity(Integer.class).isEqualTo(2);

        updateSpexare(readable, false);
    }

    @Test
    void should_ignore_an_expired_grant() {
        final Spexare spexare = spexare("Petra", false);
        grant(Permission.WRITE, PermissionGrantScope.SPEXARE, spexare, null, LocalDate.now().minusDays(1));

        updateSpexare(spexare, false);
    }

    @Test
    void should_let_a_user_edit_a_spex_but_not_delete_it() {
        grant(Permission.WRITE, PermissionGrantScope.SPEX, null, spex, null);

        as(obtainUserAccessToken())
                .document("mutation u($id: ID!) { spexUpdate(input: { id: $id, year: \"1956\", title: \"Ali Baba och de fyrtio rövarna\" }) { title } }")
                .variable("id", spex.getId()).execute().errors().verify()
                .path("spexUpdate.title").entity(String.class).isEqualTo("Ali Baba och de fyrtio rövarna");

        as(obtainUserAccessToken())
                .document("mutation d($id: ID!) { spexDelete(id: $id) }")
                .variable("id", spex.getId()).execute().errors()
                .satisfy(errors -> assertThat(errors).anyMatch(this::isForbidden));
    }

    @Test
    void should_keep_the_personal_identity_number_hidden_from_a_grantee() {
        final Spexare spexare = spexare("Petra", false);
        spexare.setSocialSecurityNumber("19121212-1212");
        spexareRepository.save(spexare);
        grant(Permission.ADMINISTRATION, PermissionGrantScope.SPEXARE, spexare, null, null);

        as(obtainUserAccessToken())
                .document("query s($id: ID!) { spexare(id: $id) { socialSecurityNumber } }")
                .variable("id", spexare.getId()).execute().errors().verify()
                .path("spexare.socialSecurityNumber").entity(String.class).isEqualTo("19121212");
    }

    @Test
    void should_remove_the_grants_on_a_deleted_spexare() {
        final Spexare spexare = spexare("Petra", false);
        grant(Permission.READ, PermissionGrantScope.SPEXARE, spexare, null, null);

        as(obtainAdminAccessToken())
                .document("mutation d($id: ID!) { spexareDelete(id: $id) }")
                .variable("id", spexare.getId()).execute().errors().verify();

        assertThat(grantRepository.count()).isZero();
    }

    private void updateSpexare(final Spexare spexare, final boolean permitted) {
        final var response = as(obtainUserAccessToken())
                .document(UPDATE_SPEXARE)
                .variable("id", spexare.getId())
                .variable("firstName", spexare.getFirstName() + "-ändrad")
                .execute();

        if (permitted) {
            response.errors().verify().path("spexareUpdate.firstName").entity(String.class).isEqualTo(spexare.getFirstName() + "-ändrad");
        } else {
            response.errors().satisfy(errors -> assertThat(errors).isNotEmpty()).path("spexareUpdate").valueIsNull();
        }
    }

    private void grant(final Permission permission, final PermissionGrantScope scope, final Spexare spexare, final Spex onSpex, final LocalDate validUntil) {
        final PermissionGrant grant = new PermissionGrant();

        grant.setUser(grantee);
        grant.setPermission(permission);
        grant.setScope(scope);
        grant.setSpexare(spexare);
        grant.setSpex(onSpex);
        grant.setValidUntil(validUntil);
        grantRepository.save(grant);
    }

    private Spexare spexare(final String firstName, final boolean published) {
        final Spexare spexare = new Spexare();

        spexare.setFirstName(firstName);
        spexare.setLastName("Behörig");
        spexare.setDeceased(false);
        spexare.setPublished(published);

        return spexareRepository.save(spexare);
    }

    private Spexare participant(final String firstName) {
        final Spexare spexare = spexare(firstName, false);
        final Activity activity = new Activity();
        activity.setSpexare(spexare);
        final SpexActivity spexActivity = new SpexActivity();
        spexActivity.setActivity(activityRepository.save(activity));
        spexActivity.setSpex(spex);
        spexActivityRepository.save(spexActivity);

        return spexare;
    }

    private GraphQlTester as(final String token) {
        return httpGraphQlTester.mutate().headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, token)).build();
    }

    private boolean isForbidden(final org.springframework.graphql.ResponseError error) {
        return ErrorType.FORBIDDEN.toString().equals(classification(error));
    }

    private static String classification(final org.springframework.graphql.ResponseError error) {
        return String.valueOf(error.getExtensions().get("classification"));
    }
}
