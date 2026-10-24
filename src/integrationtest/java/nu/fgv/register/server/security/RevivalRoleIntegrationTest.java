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

package nu.fgv.register.server.security;

import nu.fgv.register.server.spex.SpexCreateDto;
import nu.fgv.register.server.spex.SpexDto;
import nu.fgv.register.server.spex.SpexService;
import nu.fgv.register.server.spexare.SpexareCreateDto;
import nu.fgv.register.server.spexare.SpexareDto;
import nu.fgv.register.server.spexare.SpexareService;
import nu.fgv.register.server.spexare.activity.ActivityDto;
import nu.fgv.register.server.spexare.activity.ActivityService;
import nu.fgv.register.server.spexare.activity.spex.SpexActivityService;
import nu.fgv.register.server.spexare.membership.MembershipCreateDto;
import nu.fgv.register.server.spexare.membership.MembershipService;
import nu.fgv.register.server.tag.TagCreateDto;
import nu.fgv.register.server.tag.TagDto;
import nu.fgv.register.server.tag.TagService;
import nu.fgv.register.server.task.TaskCreateDto;
import nu.fgv.register.server.task.TaskDto;
import nu.fgv.register.server.task.TaskService;
import nu.fgv.register.server.task.category.TaskCategoryCreateDto;
import nu.fgv.register.server.task.category.TaskCategoryDto;
import nu.fgv.register.server.task.category.TaskCategoryService;
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import nu.fgv.register.server.util.randomizer.SocialSecurityNumberRandomizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@code REVIVAL} role, held only by Nyuppsättning's service account, reads spexes, tasks, task categories, tags
 * and spexare, creates what Nyuppsättning needs, and nothing else.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
class RevivalRoleIntegrationTest extends AbstractGraphqlIntegrationTest {
    private static final String REVIVAL_CLIENT_ID = "fgv-revival-service";
    private static final String ACTING_PERSON = "Anna Andersson (via Nyuppsättning)";
    private static final String ACTING_PERSON_HEADER = "Anna%20Andersson%20(via%20Nyupps%C3%A4ttning)";

    private final SpexService spexService;
    private final TaskService taskService;
    private final TaskCategoryService taskCategoryService;
    private final SpexareService spexareService;
    private final ActivityService activityService;
    private final SpexActivityService spexActivityService;
    private final MembershipService membershipService;
    private final TagService tagService;
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public RevivalRoleIntegrationTest(final JdbcClient jdbcClient,
                                      final Keycloak keycloakAdminClient,
                                      final String keycloakClientId,
                                      final ObjectMapper objectMapper,
                                      final SpexService spexService,
                                      final TaskService taskService,
                                      final TaskCategoryService taskCategoryService,
                                      final SpexareService spexareService,
                                      final ActivityService activityService,
                                      final SpexActivityService spexActivityService,
                                      final MembershipService membershipService,
                                      final TagService tagService) {
        super(jdbcClient, keycloakAdminClient, keycloakClientId, objectMapper);
        this.spexService = spexService;
        this.taskService = taskService;
        this.taskCategoryService = taskCategoryService;
        this.spexareService = spexareService;
        this.activityService = activityService;
        this.spexActivityService = spexActivityService;
        this.membershipService = membershipService;
        this.tagService = tagService;
    }

    @BeforeEach
    void setUp() {
        httpGraphQlTester = HttpGraphQlTester.create(
                WebTestClient.bindToServer()
                        .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath))
                        .build()
        );

        jdbcClient.sql("UPDATE user SET spexare_id = NULL").update();
        jdbcClient.sql("UPDATE spexare SET partner_id = NULL").update();
        jdbcClient.sql("DELETE FROM permission_grant").update();
        for (final String table : List.of("tagging", "actor", "task_activity", "spex_activity", "activity", "address", "consent", "membership", "toggle", "spexare")) {
            jdbcClient.sql("DELETE FROM " + table).update();
        }
        jdbcClient.sql("UPDATE spex SET parent_id = NULL").update();
        jdbcClient.sql("UPDATE task SET category_id = NULL").update();
        jdbcClient.sql("DELETE FROM spex").update();
        jdbcClient.sql("DELETE FROM spex_details").update();
        jdbcClient.sql("DELETE FROM task").update();
        jdbcClient.sql("DELETE FROM task_category").update();
        jdbcClient.sql("DELETE FROM tag").update();
        // Other tests empty the type table, so make sure the types Nyuppsättning uses are there
        for (final String[] type : List.of(new String[]{"HOME", "ADDRESS"}, new String[]{"REGISTER", "CONSENT"}, new String[]{"FGV", "MEMBERSHIP"}, new String[]{"B1", "VOCAL"})) {
            jdbcClient.sql("INSERT IGNORE INTO type (id, type, labels, created_by, created_at, version) VALUES (:id, :type, '{ \"en\": \"x\", \"sv\": \"x\" }', 'system', CURRENT_DATE(), 0)")
                    .param("id", type[0])
                    .param("type", type[1])
                    .update();
        }
    }

    @Test
    void should_read_spexes_and_their_revivals() {
        final SpexDto original = asAdmin(() -> spexService.create(SpexCreateDto.builder().year("1956").title("Ali Baba").build()));
        final SpexDto revival = asAdmin(() -> spexService.addRevival(original.getId(), "2028"));

        revivalTester()
                .document("""
                        query { spexPaged(first: 500, filter: "parent:NULL", sort: ["year"], direction: ASC) {
                            edges { node { id title revivals { id year } } }
                            totalCount
                        } }""")
                .execute()
                .errors()
                .verify()
                .path("spexPaged.totalCount").entity(Integer.class).isEqualTo(1)
                .path("spexPaged.edges[0].node.title").entity(String.class).isEqualTo("Ali Baba")
                .path("spexPaged.edges[0].node.revivals[0].year").entity(String.class).isEqualTo("2028");

        revivalTester()
                .document("query spex($id: ID!) { spex(id: $id) { id title revival } }")
                .variable("id", original.getId())
                .execute()
                .errors()
                .verify()
                .path("spex.revival").entity(Boolean.class).isEqualTo(false);

        revivalTester()
                .document("query spexRevival($spexId: ID!, $id: ID!) { spexRevival(spexId: $spexId, id: $id) { id revival } }")
                .variable("spexId", original.getId())
                .variable("id", revival.getId())
                .execute()
                .errors()
                .verify()
                .path("spexRevival.revival").entity(Boolean.class).isEqualTo(true);
    }

    @Test
    void should_read_tasks_and_task_categories() {
        final TaskCategoryDto category = asAdmin(() -> taskCategoryService.create(TaskCategoryCreateDto.builder().name("Ensemble").actorPresent(true).build()));
        final TaskDto task = asAdmin(() -> taskService.create(TaskCreateDto.builder().name("Sångare").build()));
        asAdmin(() -> {
            taskService.addCategory(task.getId(), category.getId());
            return null;
        });

        revivalTester()
                .document("query { taskPaged(first: 100) { edges { node { id name category { id name } } } totalCount } }")
                .execute()
                .errors()
                .verify()
                .path("taskPaged.totalCount").entity(Integer.class).isEqualTo(1)
                .path("taskPaged.edges[0].node.category.name").entity(String.class).isEqualTo("Ensemble");

        revivalTester()
                .document("query task($id: ID!) { task(id: $id) { name } }")
                .variable("id", task.getId())
                .execute()
                .errors()
                .verify()
                .path("task.name").entity(String.class).isEqualTo("Sångare");

        revivalTester()
                .document("query { taskCategoryPaged(first: 100) { totalCount } }")
                .execute()
                .errors()
                .verify()
                .path("taskCategoryPaged.totalCount").entity(Integer.class).isEqualTo(1);

        revivalTester()
                .document("query taskCategory($id: ID!) { taskCategory(id: $id) { name } }")
                .variable("id", category.getId())
                .execute()
                .errors()
                .verify()
                .path("taskCategory.name").entity(String.class).isEqualTo("Ensemble");
    }

    @Test
    void should_have_the_register_consent_type() {
        assertThat(jdbcClient.sql("SELECT success FROM flyway_schema_history WHERE version = '1.33'").query(Boolean.class).single()).isTrue();
    }

    @Test
    void should_read_every_spexare_but_only_the_birth_date_of_the_personal_identity_number() {
        final String socialSecurityNumber = fullSocialSecurityNumber();
        final SpexareDto spexare = asAdmin(() -> spexareService.create(SpexareCreateDto.builder()
                .firstName("Ada").lastName("Lovelace").nickName("Ada").socialSecurityNumber(socialSecurityNumber)
                .deceased(true).published(false).graduation("K85").build()));

        revivalTester()
                .document("""
                        query spexare($id: ID!) { spexare(id: $id) {
                            id firstName lastName nickName graduation deceased published socialSecurityNumber
                            addresses { id } consents { id } activities { id spexActivity { spex { id } } taskActivities { task { id } actors { role vocal { id } } } }
                        } }""")
                .variable("id", spexare.getId())
                .execute()
                .errors()
                .verify()
                .path("spexare.firstName").entity(String.class).isEqualTo("Ada")
                .path("spexare.deceased").entity(Boolean.class).isEqualTo(true)
                .path("spexare.published").entity(Boolean.class).isEqualTo(false)
                .path("spexare.socialSecurityNumber").entity(String.class).isEqualTo(socialSecurityNumber.substring(0, 8));
    }

    @Test
    void should_fetch_spexare_by_ids() {
        final SpexareDto published = asAdmin(() -> createSpexare("Bertil", true));
        final SpexareDto unpublished = asAdmin(() -> createSpexare("Cecilia", false));

        revivalTester()
                .document("query spexareByIds($ids: [ID!]!) { spexareByIds(ids: $ids) { id firstName deceased } }")
                .variable("ids", List.of(published.getId(), unpublished.getId(), published.getId(), 999_999_999L))
                .execute()
                .errors()
                .verify()
                .path("spexareByIds[*].firstName").entityList(String.class).hasSize(2).contains("Bertil", "Cecilia");
    }

    @Test
    void should_refuse_more_than_500_ids() {
        revivalTester()
                .document("query spexareByIds($ids: [ID!]!) { spexareByIds(ids: $ids) { id } }")
                .variable("ids", LongStream.rangeClosed(1, 501).boxed().toList())
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors)
                        .anyMatch(error -> ErrorType.BAD_REQUEST.toString().equals(String.valueOf(error.getExtensions().get("classification")))));
    }

    @Test
    void should_list_the_participants_of_a_spex_and_its_revivals() {
        final SpexDto original = asAdmin(() -> spexService.create(SpexCreateDto.builder().year("1956").title("Ali Baba").build()));
        final SpexDto revival = asAdmin(() -> spexService.addRevival(original.getId(), "2028"));
        final SpexDto other = asAdmin(() -> spexService.create(SpexCreateDto.builder().year("1957").title("Bröderna Marx").build()));
        final SpexareDto inOriginal = asAdmin(() -> createSpexare("Doris", false));
        final SpexareDto inRevival = asAdmin(() -> createSpexare("Erik", true));
        final SpexareDto inOther = asAdmin(() -> createSpexare("Fredrika", true));
        asAdmin(() -> {
            participate(inOriginal, original);
            participate(inOriginal, revival);
            participate(inRevival, revival);
            participate(inOther, other);
            return null;
        });

        revivalTester()
                .document("query spexareBySpex($spexId: ID!) { spexareBySpex(spexId: $spexId) { id firstName deceased } }")
                .variable("spexId", original.getId())
                .execute()
                .errors()
                .verify()
                .path("spexareBySpex[*].firstName").entityList(String.class).containsExactly("Doris", "Erik");

        // The same policies apply to every role: a user sees only the published participant
        httpGraphQlTester
                .mutate()
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainUserAccessToken()))
                .build()
                .document("query spexareBySpex($spexId: ID!) { spexareBySpex(spexId: $spexId) { id firstName } }")
                .variable("spexId", original.getId())
                .execute()
                .errors()
                .verify()
                .path("spexareBySpex[*].firstName").entityList(String.class).containsExactly("Erik");

        revivalTester()
                .document("query spexareBySpex($spexId: ID!) { spexareBySpex(spexId: $spexId) { id } }")
                .variable("spexId", 999_999_999L)
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors)
                        .anyMatch(error -> ErrorType.NOT_FOUND.toString().equals(String.valueOf(error.getExtensions().get("classification")))));
    }

    @Test
    void should_search_spexare_by_name() {
        asAdmin(() -> spexareService.create(SpexareCreateDto.builder().firstName("Zebulon").lastName("Zakrisson")
                .deceased(true).published(false).build()));

        revivalTester()
                .document("""
                        query { spexareSearchPaged(first: 20, q: "Zakrisson") {
                            edges { node { id firstName lastName deceased } }
                            facets { id }
                        } }""")
                .execute()
                .errors()
                .verify()
                .path("spexareSearchPaged.edges[*].node.lastName").entityList(String.class).contains("Zakrisson")
                .path("spexareSearchPaged.edges[0].node.deceased").entity(Boolean.class).isEqualTo(true)
                .path("spexareSearchPaged.facets[*].id").entityList(String.class).doesNotContain("memberships");
    }

    @Test
    void should_create_what_nyuppsattning_needs_and_record_who_acted() {
        final SpexDto spex = asAdmin(() -> spexService.create(SpexCreateDto.builder().year("1956").title("Ali Baba").build()));
        final TaskDto task = asAdmin(() -> taskService.create(TaskCreateDto.builder().name("Sångare").build()));
        final TagDto tag = asAdmin(() -> tagService.create(new TagCreateDto("Nyuppsättning")));
        final String socialSecurityNumber = fullSocialSecurityNumber();

        final Long spexareId = Long.valueOf(revivalTester(ACTING_PERSON_HEADER)
                .document("""
                        mutation spexareCreate($ssn: SocialSecurityNumber) { spexareCreate(input: {
                            firstName: "Gustav", lastName: "Gran", nickName: "Gurra", socialSecurityNumber: $ssn,
                            deceased: false, published: false, graduation: "E95"
                        }) { id socialSecurityNumber createdBy } }""")
                .variable("ssn", socialSecurityNumber)
                .execute()
                .errors()
                .verify()
                .path("spexareCreate.socialSecurityNumber").valueIsNull()
                .path("spexareCreate.id").entity(String.class).get());

        final String addressId = revivalTester(ACTING_PERSON_HEADER)
                .document("""
                        mutation addressCreate($spexareId: ID!) { addressCreate(spexareId: $spexareId, typeId: "HOME", input: {
                            emailAddress: "gustav@example.com", phoneMobile: "0701234567"
                        }) { id } }""")
                .variable("spexareId", spexareId)
                .execute()
                .errors()
                .verify()
                .path("addressCreate.id").entity(String.class).get();

        revivalTester(ACTING_PERSON_HEADER)
                .document("""
                        mutation addressUpdate($spexareId: ID!, $id: ID!) { addressUpdate(spexareId: $spexareId, typeId: "HOME", input: {
                            id: $id, emailAddress: "gustav.gran@example.com", phoneMobile: "0701234567"
                        }) { emailAddress } }""")
                .variable("spexareId", spexareId)
                .variable("id", addressId)
                .execute()
                .errors()
                .verify()
                .path("addressUpdate.emailAddress").entity(String.class).isEqualTo("gustav.gran@example.com");

        final String consentId = revivalTester(ACTING_PERSON_HEADER)
                .document("mutation consentCreate($spexareId: ID!) { consentCreate(spexareId: $spexareId, typeId: \"REGISTER\", input: { value: true }) { id type { id } } }")
                .variable("spexareId", spexareId)
                .execute()
                .errors()
                .verify()
                .path("consentCreate.type.id").entity(String.class).isEqualTo("REGISTER")
                .path("consentCreate.id").entity(String.class).get();

        revivalTester(ACTING_PERSON_HEADER)
                .document("mutation consentUpdate($spexareId: ID!, $id: ID!) { consentUpdate(spexareId: $spexareId, typeId: \"REGISTER\", input: { id: $id, value: false }) { value } }")
                .variable("spexareId", spexareId)
                .variable("id", consentId)
                .execute()
                .errors()
                .verify()
                .path("consentUpdate.value").entity(Boolean.class).isEqualTo(false);

        final String revivalId = revivalTester(ACTING_PERSON_HEADER)
                .document("mutation spexRevivalCreate($spexId: ID!) { spexRevivalCreate(spexId: $spexId, year: \"2030\") { id } }")
                .variable("spexId", spex.getId())
                .execute()
                .errors()
                .verify()
                .path("spexRevivalCreate.id").entity(String.class).get();

        final String activityId = revivalTester(ACTING_PERSON_HEADER)
                .document("mutation activityCreate($spexareId: ID!) { activityCreate(spexareId: $spexareId) { id } }")
                .variable("spexareId", spexareId)
                .execute()
                .errors()
                .verify()
                .path("activityCreate.id").entity(String.class).get();

        revivalTester(ACTING_PERSON_HEADER)
                .document("mutation spexActivityCreate($spexareId: ID!, $activityId: ID!, $spexId: ID!) { spexActivityCreate(spexareId: $spexareId, activityId: $activityId, spexId: $spexId) { spex { year } } }")
                .variable("spexareId", spexareId)
                .variable("activityId", activityId)
                .variable("spexId", revivalId)
                .execute()
                .errors()
                .verify()
                .path("spexActivityCreate.spex.year").entity(String.class).isEqualTo("2030");

        final String taskActivityId = revivalTester(ACTING_PERSON_HEADER)
                .document("mutation taskActivityCreate($spexareId: ID!, $activityId: ID!, $taskId: ID!) { taskActivityCreate(spexareId: $spexareId, activityId: $activityId, taskId: $taskId) { id } }")
                .variable("spexareId", spexareId)
                .variable("activityId", activityId)
                .variable("taskId", task.getId())
                .execute()
                .errors()
                .verify()
                .path("taskActivityCreate.id").entity(String.class).get();

        revivalTester(ACTING_PERSON_HEADER)
                .document("""
                        mutation actorCreate($spexareId: ID!, $activityId: ID!, $taskActivityId: ID!) {
                            actorCreate(spexareId: $spexareId, activityId: $activityId, taskActivityId: $taskActivityId, vocalId: "B1", input: { role: "Ali Baba" }) { role vocal { id } }
                        }""")
                .variable("spexareId", spexareId)
                .variable("activityId", activityId)
                .variable("taskActivityId", taskActivityId)
                .execute()
                .errors()
                .verify()
                .path("actorCreate.role").entity(String.class).isEqualTo("Ali Baba");

        revivalTester(ACTING_PERSON_HEADER)
                .document("mutation taggingCreate($spexareId: ID!, $tagId: ID!) { taggingCreate(spexareId: $spexareId, tagId: $tagId) }")
                .variable("spexareId", spexareId)
                .variable("tagId", tag.getId())
                .execute()
                .errors()
                .verify();

        revivalTester(ACTING_PERSON_HEADER)
                .document("mutation membershipCreate($spexareId: ID!) { membershipCreate(spexareId: $spexareId, typeId: \"FGV\", input: { year: \"2030\" }) { id } }")
                .variable("spexareId", spexareId)
                .execute()
                .errors()
                .verify();

        // The integration test profile replaces the auditor of created_by and last_modified_by (see JpaConfig), so the
        // revision, which always uses Auditor, is what shows who acted here; AuditorTest covers created_by.
        final String modifiedBy = jdbcClient.sql("SELECT r.modified_by FROM revinfo r JOIN spexare_audit a ON a.rev = r.id WHERE a.id = :id ORDER BY r.id LIMIT 1")
                .param("id", spexareId).query(String.class).single();
        final String socialSecurityNumberStored = jdbcClient.sql("SELECT social_security_number FROM spexare WHERE id = :id").param("id", spexareId).query(String.class).optional().orElse(null);

        assertThat(modifiedBy).isEqualTo(REVIVAL_CLIENT_ID + " on behalf of " + ACTING_PERSON);
        assertThat(socialSecurityNumberStored).isNull();
        assertThat(jdbcClient.sql("SELECT r.modified_by FROM revinfo r JOIN membership_audit a ON a.rev = r.id WHERE a.spexare_id = :id")
                .param("id", spexareId).query(String.class).single())
                .isEqualTo(modifiedBy);
    }

    @Test
    void should_refuse_memberships_and_filtering_on_them() {
        final SpexareDto spexare = asAdmin(() -> createSpexare("Hilda", true));
        asAdmin(() -> membershipService.create(spexare.getId(), "FGV", MembershipCreateDto.builder().year("2020").build()));

        for (final String document : List.of(
                "query spexare($id: ID!) { spexare(id: $id) { id memberships { id } } }",
                "query spexare($id: ID!) { spexare(id: $id) { id membershipsPaged(first: 10) { totalCount } } }",
                "query spexare($id: ID!) { spexare(id: $id) { id partner { id } } }",
                "query spexare($id: ID!) { spexare(id: $id) { id taggings { id } } }")) {
            revivalTester()
                    .document(document)
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .satisfy(errors -> assertThat(errors)
                            .as(document)
                            .anyMatch(error -> ErrorType.FORBIDDEN.toString().equals(String.valueOf(error.getExtensions().get("classification")))));
        }

        for (final String document : List.of(
                "query { spexarePaged(first: 10, filter: \"memberships.type.id:FGV\") { totalCount } }",
                "query { spexareSearchPaged(first: 10, q: \"\", aggregationFilters: [{ name: \"memberships\", value: \"FGV\" }]) { totalCount } }",
                "query { spexareSearchPaged(first: 10, q: \"\", aggregationFilters: [{ name: \"quality\", value: \"noMembership\" }]) { totalCount } }")) {
            revivalTester()
                    .document(document)
                    .execute()
                    .errors()
                    .satisfy(errors -> assertThat(errors)
                            .as(document)
                            .anyMatch(error -> ErrorType.BAD_REQUEST.toString().equals(String.valueOf(error.getExtensions().get("classification")))));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "query { spexCategoryPaged(first: 10) { totalCount } }",
            "query { newsPaged(first: 10) { totalCount } }",
            "query { userPaged(first: 10) { totalCount } }",
            "query { spexareMe { id } }",
            "query { spexareExport(type: EXCEL) { id } }",
            "mutation { spexCreate(input: { year: \"1999\", title: \"Inte tillåten\" }) { id } }",
            "mutation { taskCreate(input: { name: \"Inte tillåten\" }) { id } }",
            "mutation { tagCreate(input: { name: \"Inte tillåten\" }) { id } }",
            "mutation { spexareUpdate(input: { id: 1, firstName: \"A\", lastName: \"B\", deceased: false, published: true }) { id } }",
            "mutation { spexareDelete(id: 1) }",
            "mutation { spexarePartnerAdd(spexareId: 1, id: 2) }",
            "mutation { addressDelete(spexareId: 1, typeId: \"HOME\", id: 1) }",
            "mutation { consentDelete(spexareId: 1, typeId: \"REGISTER\", id: 1) }",
            "mutation { membershipDelete(spexareId: 1, typeId: \"FGV\", id: 1) }",
            "mutation { activityDelete(spexareId: 1, id: 1) }",
            "mutation { spexActivityUpdate(spexareId: 1, activityId: 1, spexId: 1, id: 1) { id } }",
            "mutation { spexActivityDelete(spexareId: 1, activityId: 1, id: 1) }",
            "mutation { taskActivityDelete(spexareId: 1, activityId: 1, id: 1) }",
            "mutation { actorDelete(spexareId: 1, activityId: 1, taskActivityId: 1, vocalId: \"B1\", id: 1) }",
            "mutation { taggingDelete(spexareId: 1, tagId: 1) }",
            "mutation { spexRevivalDelete(spexId: 1, id: 1) }",
            "mutation { toggleCreate(spexareId: 1, typeId: \"X\", input: { value: true }) { id } }"
    })
    void should_refuse_everything_else(final String document) {
        revivalTester()
                .document(document)
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors)
                        .anyMatch(error -> ErrorType.FORBIDDEN.toString().equals(String.valueOf(error.getExtensions().get("classification")))));
    }

    @Test
    void should_refuse_the_category_of_a_spex() {
        asAdmin(() -> spexService.create(SpexCreateDto.builder().year("1956").title("Ali Baba").build()));

        revivalTester()
                .document("query { spexPaged(first: 10) { edges { node { id category { id } } } } }")
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors)
                        .anyMatch(error -> ErrorType.FORBIDDEN.toString().equals(String.valueOf(error.getExtensions().get("classification")))));
    }

    private HttpGraphQlTester revivalTester() {
        final String token = obtainServiceAccessToken();

        return httpGraphQlTester
                .mutate()
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, token))
                .build();
    }

    private HttpGraphQlTester revivalTester(final String actingPerson) {
        final String token = obtainServiceAccessToken();

        return httpGraphQlTester
                .mutate()
                .headers(headers -> {
                    headers.set(HttpHeaders.AUTHORIZATION, token);
                    headers.set("X-Acting-Person", actingPerson);
                })
                .build();
    }

    private SpexareDto createSpexare(final String firstName, final boolean published) {
        return spexareService.create(SpexareCreateDto.builder().firstName(firstName).lastName("Testsson").deceased(false).published(published).build());
    }

    private void participate(final SpexareDto spexare, final SpexDto spex) {
        final ActivityDto activity = activityService.create(spexare.getId());

        spexActivityService.create(spexare.getId(), activity.getId(), spex.getId());
    }

    private static String fullSocialSecurityNumber() {
        final SocialSecurityNumberRandomizer randomizer = new SocialSecurityNumberRandomizer();

        while (true) {
            final String value = randomizer.getRandomValue();

            if (value.length() > 8) {
                return value;
            }
        }
    }

    private String obtainServiceAccessToken() {
        final MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("grant_type", "client_credentials");
        formData.add("client_id", REVIVAL_CLIENT_ID);
        formData.add("client_secret", serviceClientSecret());

        @SuppressWarnings("unchecked") final Map<String, Object> response = WebClient.builder().build()
                .post()
                .uri(issuerUri + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return "Bearer " + response.get("access_token");
    }

    /**
     * The secret of {@code fgv-revival-service} in the imported test realm. The test's admin client may not read secrets.
     */
    private String serviceClientSecret() {
        try {
            final JsonNode realmJson = objectMapper.readTree(new ClassPathResource("keycloak/fgv.json").getContentAsString(StandardCharsets.UTF_8));

            for (final JsonNode client : realmJson.get("clients")) {
                if (REVIVAL_CLIENT_ID.equals(client.get("clientId").asString())) {
                    return client.get("secret").asString();
                }
            }
            throw new IllegalStateException("No client " + REVIVAL_CLIENT_ID + " in the test realm");
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private <T> T asAdmin(final Supplier<T> action) {
        final var previous = SecurityContextHolder.getContext().getAuthentication();

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin-creator", "ignored", "ROLE_spexregister_ADMIN"));
        try {
            return action.get();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }
}
