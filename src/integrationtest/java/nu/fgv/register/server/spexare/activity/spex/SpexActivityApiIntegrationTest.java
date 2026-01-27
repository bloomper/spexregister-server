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

package nu.fgv.register.server.spexare.activity.spex;

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
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.util.AbstractAuditable;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.randomizer.SocialSecurityNumberRandomizer;
import nu.fgv.register.server.util.randomizer.YearRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.ApiVersionInserter;
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
class SpexActivityApiIntegrationTest extends AbstractIntegrationTest {

    private final EasyRandom random;
    private final SpexActivityRepository repository;
    private final ActivityRepository activityRepository;
    private final SpexareRepository spexareRepository;
    private final SpexRepository spexRepository;
    private final SpexDetailsRepository spexDetailsRepository;
    private final SpexCategoryRepository spexCategoryRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public SpexActivityApiIntegrationTest(final JdbcClient jdbcClient,
                                          final AclCache aclCache,
                                          final Keycloak keycloakAdminClient,
                                          final String keycloakClientId,
                                          final PermissionService permissionService,
                                          final SpexActivityRepository repository,
                                          final ActivityRepository activityRepository,
                                          final SpexareRepository spexareRepository,
                                          final SpexRepository spexRepository,
                                          final SpexDetailsRepository spexDetailsRepository,
                                          final SpexCategoryRepository spexCategoryRepository,
                                          final ObjectMapper objectMapper) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.repository = repository;
        this.activityRepository = activityRepository;
        this.spexareRepository = spexareRepository;
        this.spexRepository = spexRepository;
        this.spexDetailsRepository = spexDetailsRepository;
        this.spexCategoryRepository = spexCategoryRepository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

        parameters
                .randomize(
                        named("year"), new YearRandomizer()
                )
                .randomize(
                        named("firstYear"), new YearRandomizer()
                )
                .randomize(
                        named("socialSecurityNumber"), new SocialSecurityNumberRandomizer()
                )
                .excludeField(named("partner").and(ofType(Spexare.class)).and(inClass(Spexare.class)))
                .excludeField(named("user").and(ofType(User.class)).and(inClass(Spexare.class)))
                .excludeField(named("tags").and(ofType(Set.class)).and(inClass(Spexare.class)))
                .excludeField(named("activities").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("memberships").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("consents").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("toggles").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("tags").and(ofType(List.class)).and(inClass(Spexare.class)))
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
                .baseUrl("http://localhost:%s/api/spexare/{spexareId}/activities/{activityId}/spex-activity".formatted(localPort))
                .apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
                .build();

        JdbcTestUtils.deleteFromTables(jdbcClient, "spex_activity", "activity", "spexare", "spex", "spex_details", "spex_category", "event", "spex_activity_audit", "activity_audit", "spexare_audit", "spex_audit", "spex_details_audit", "spex_category_audit");
    }

    @AfterEach
    void tearDown() {
    }

    @Nested
    @DisplayName("Retrieve")
    class RetrieveTests {

        @Test
        void should_return_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            final SpexActivityDto result = restTestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder.build(spexare.getId(), activity.getId()))
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(SpexActivityDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id")
                    .isEqualTo(spexActivity.getId());
        }

        @Test
        void should_return_404_when_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));

            final ProblemDetail result = restTestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder.build(spexare.getId(), activity.getId()))
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_spexare_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            final ProblemDetail result = restTestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder.build(1L, activity.getId()))
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_activity_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            final ProblemDetail result = restTestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder.build(spexare.getId(), 1L))
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_incorrect_spexare() {
            final var spexare1 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare1.getId()));
            final var spexare2 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare2.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare2));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            final ProblemDetail result = restTestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder.build(spexare1.getId(), activity.getId()))
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_incorrect_activity() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var activity1 = persistActivity(randomizeActivity(spexare));
            final var activity2 = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity2, spex));

            final ProblemDetail result = restTestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder.build(spexare.getId(), activity1.getId()))
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }

    @Nested
    @DisplayName("Retrieve by id")
    class RetrieveByIdTests {
        @Test
        void should_return_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            final SpexActivityDto result = restTestClient
                    .get()
                    .uri("/{id}", spexare.getId(), activity.getId(), spexActivity.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(SpexActivityDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id")
                    .isEqualTo(spexActivity.getId());
        }

        @Test
        void should_return_404_when_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));

            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/{id}", spexare.getId(), activity.getId(), 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_spexare_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/{id}", 1L, activity.getId(), spexActivity.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_activity_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/{id}", spexare.getId(), -1L, spexActivity.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_incorrect_spexare() {
            final var spexare1 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare1.getId()));
            final var spexare2 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare2.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare2));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/{id}", spexare1.getId(), activity.getId(), spexActivity.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_incorrect_activity() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleUser(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleUser(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleUser(toObjectIdentity(Spex.class, spex.getId()));
            final var activity1 = persistActivity(randomizeActivity(spexare));
            final var activity2 = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity2, spex));

            final ProblemDetail result = restTestClient
                    .get()
                    .uri("/{id}", spexare.getId(), activity1.getId(), spexActivity.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainUserAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create_and_return_201() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));

            restTestClient
                    .post()
                    .uri("/{spexId}", spexare.getId(), activity.getId(), spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isCreated();

            final SpexActivityDto result = restTestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder.build(spexare.getId(), activity.getId()))
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(SpexActivityDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
        }

        @Test
        void should_return_404_when_creating_and_spexare_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));

            final ProblemDetail result = restTestClient
                    .post()
                    .uri("/{spexId}", 1L, activity.getId(), spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_creating_and_activity_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            persistActivity(randomizeActivity(spexare));

            final ProblemDetail result = restTestClient
                    .post()
                    .uri("/{spexId}", spexare.getId(), 1L, spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_creating_and_spex_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));

            final ProblemDetail result = restTestClient
                    .post()
                    .uri("/{spexId}", spexare.getId(), activity.getId(), 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_409_when_creating_and_incorrect_spexare() {
            final var spexare1 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare1.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare1.getId()));
            final var spexare2 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare2.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare2.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare2));

            final ProblemDetail result = restTestClient
                    .post()
                    .uri("/{spexId}", spexare1.getId(), activity.getId(), spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_permission() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));

            final ProblemDetail result = restTestClient
                    .post()
                    .uri("/{spexId}", spexare.getId(), activity.getId(), spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_401_when_not_permitted_due_to_insufficient_role() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));

            restTestClient
                    .post()
                    .uri("/{spexId}", spexare.getId(), activity.getId(), spex.getId())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isUnauthorized();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Update")
    class UpdateTests {

        @Test
        void should_update_and_return_204() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex1 = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex1.getId()));
            final var spex2 = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex2.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex1));

            final SpexActivityDto result = restTestClient
                    .put()
                    .uri("/{id}/{spexId}", spexare.getId(), activity.getId(), spexActivity.getId(), spex2.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(SpexActivityDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result)
                    .extracting("id")
                    .isEqualTo(spexActivity.getId());
        }

        @Test
        void should_return_404_when_updating_and_spexare_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{id}/{spexId}", 1L, activity.getId(), spexActivity.getId(), spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_updating_and_activity_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{id}/{spexId}", spexare.getId(), 1L, spexActivity.getId(), spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_updating_and_spex_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{id}/{spexId}", spexare.getId(), activity.getId(), spexActivity.getId(), 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_updating_and_incorrect_spexare() {
            final var spexare1 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare1.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare1.getId()));
            final var spexare2 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare2.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare2.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare2));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{id}/{spexId}", spexare1.getId(), activity.getId(), spexActivity.getId(), spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_updating_and_incorrect_activity() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity1 = persistActivity(randomizeActivity(spexare));
            final var activity2 = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity2, spex));

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{id}/{spexId}", spexare.getId(), activity1.getId(), spexActivity.getId(), spex.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_permission() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex1 = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex1.getId()));
            final var spex2 = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex2.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex1));

            final ProblemDetail result = restTestClient
                    .put()
                    .uri("/{id}/{spexId}", spexare.getId(), activity.getId(), spexActivity.getId(), spex2.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_401_when_not_permitted_due_to_insufficient_role() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex1 = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex1.getId()));
            final var spex2 = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex2.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex1));

            restTestClient
                    .put()
                    .uri("/{spexId}", spexare.getId(), activity.getId(), spex2.getId())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isUnauthorized();

            assertThat(repository.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        void should_delete_and_return_204() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            restTestClient
                    .delete()
                    .uri("/{id}", spexare.getId(), activity.getId(), spexActivity.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNoContent();

            restTestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder.build(spexare.getId(), activity.getId()))
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_404_when_deleting_non_existing_value() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{id}", spexare.getId(), activity.getId(), 1L)
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_deleting_and_spexare_not_found() {
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(null));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{id}", 1L, activity.getId(), spexActivity.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_deleting_and_activity_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(null));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{id}", spexare.getId(), 1L, spexActivity.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_deleting_and_incorrect_spexare() {
            final var spexare1 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare1.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare1.getId()));
            final var spexare2 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare2.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare2.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare2));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{id}", spexare1.getId(), activity.getId(), spexActivity.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_404_when_deleting_and_incorrect_activity() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity1 = persistActivity(randomizeActivity(spexare));
            final var activity2 = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity2, spex));

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{id}", spexare.getId(), 1L, spexActivity.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void should_return_403_when_not_permitted_due_to_insufficient_permission() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            final ProblemDetail result = restTestClient
                    .delete()
                    .uri("/{id}", spexare.getId(), activity.getId(), spexActivity.getId())
                    .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(ProblemDetail.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void should_return_401_when_not_permitted_due_to_insufficient_role() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var category = persistSpexCategory(randomizeSpexCategory());
            grantReadPermissionToRoleAdmin(toObjectIdentity(SpexCategory.class, category.getId()));
            final var spex = persistSpex(randomizeSpex(category));
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spex.class, spex.getId()));
            final var activity = persistActivity(randomizeActivity(spexare));
            final var spexActivity = persistSpexActivity(randomizeSpexActivity(activity, spex));

            restTestClient
                    .delete()
                    .uri(uriBuilder -> uriBuilder.build(spexare.getId(), activity.getId()))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .apiVersion("1.0")
                    .exchange()
                    .expectStatus().isUnauthorized();

            assertThat(repository.count()).isEqualTo(1);
        }
    }

    private SpexActivity randomizeSpexActivity(final Activity activity, final Spex spex) {
        final var spexActivity = random.nextObject(SpexActivity.class);
        spexActivity.setActivity(activity);
        spexActivity.setSpex(spex);
        return spexActivity;
    }

    private SpexActivity persistSpexActivity(final SpexActivity spexActivity) {
        spexActivity.setId(null);

        return repository.save(spexActivity);
    }

    private Activity randomizeActivity(@Nullable final Spexare spexare) {
        final var activity = random.nextObject(Activity.class);

        activity.setSpexare(spexare);
        activity.setSpexActivity(null);

        return activity;
    }

    private Activity persistActivity(final Activity activity) {
        activity.setId(null);

        return activityRepository.save(activity);
    }

    private Spexare randomizeSpexare() {
        return random.nextObject(Spexare.class);
    }

    private Spexare persistSpexare(final Spexare spexare) {
        spexare.setId(null);

        return spexareRepository.save(spexare);
    }

    private Spex randomizeSpex(final SpexCategory category) {
        final var spex = random.nextObject(Spex.class);
        spex.setParent(null);
        final var details = random.nextObject(SpexDetails.class);
        details.setCategory(category);
        spex.setDetails(details);
        return spex;
    }

    private Spex persistSpex(final Spex spex) {
        spex.setId(null);
        spex.getDetails().setId(null);
        final var details = spexDetailsRepository.save(spex.getDetails());
        spex.setDetails(details);
        return spexRepository.save(spex);
    }

    private SpexCategory randomizeSpexCategory() {
        return random.nextObject(SpexCategory.class);
    }

    private SpexCategory persistSpexCategory(final SpexCategory category) {
        category.setId(null);

        return spexCategoryRepository.save(category);
    }

}