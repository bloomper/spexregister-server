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

package nu.fgv.register.server.spexare.toggle;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.settings.TypeRepository;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import nu.fgv.register.server.util.graphql.CustomErrorType;
import nu.fgv.register.server.util.randomizer.LabelsRandomizer;
import nu.fgv.register.server.util.randomizer.SocialSecurityNumberRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.AfterEach;
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
class ToggleGraphqlApiIntegrationTest extends AbstractGraphqlIntegrationTest {

    private final EasyRandom random;
    private final ToggleRepository repository;
    private final TypeRepository typeRepository;
    private final SpexareRepository spexareRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public ToggleGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
                                           final AclCache aclCache,
                                           final Keycloak keycloakAdminClient,
                                           final String keycloakClientId,
                                           final PermissionService permissionService,
                                           final ObjectMapper objectMapper,
                                           final ToggleRepository repository,
                                           final TypeRepository typeRepository,
                                           final SpexareRepository spexareRepository) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.repository = repository;
        this.typeRepository = typeRepository;
        this.spexareRepository = spexareRepository;

        final EasyRandomParameters parameters = new EasyRandomParameters();

        parameters
                .randomize(
                        named("labels"), new LabelsRandomizer()
                )
                .randomize(
                        named("socialSecurityNumber"), new SocialSecurityNumberRandomizer()
                )
                .excludeField(named("partner").and(ofType(Spexare.class)).and(inClass(Spexare.class)))
                .excludeField(named("user").and(ofType(User.class)).and(inClass(Spexare.class)))
                .excludeField(named("activities").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("tags").and(ofType(Set.class)).and(inClass(Spexare.class)))
                .excludeField(named("addresses").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("memberships").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("toggles").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("toggles").and(ofType(List.class)).and(inClass(Spexare.class)))
                .excludeField(named("tags").and(ofType(List.class)).and(inClass(Spexare.class)))
                .randomizationDepth(1);
        random = new EasyRandom(parameters);
    }

    @BeforeEach
    void setUp() {
        httpGraphQlTester = HttpGraphQlTester.builder(
                        WebTestClient.bindToServer()
                                .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath)))
                .build();

        JdbcTestUtils.deleteFromTables(jdbcClient, "toggle", "type", "spexare", "event");
    }

    @AfterEach
    void tearDown() {
    }

    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        void should_create() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var type = persistType(randomizeType());
            final var dto = random.nextObject(ToggleCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/toggle/toggleCreate")
                    .variable("spexareId", spexare.getId())
                    .variable("typeId", type.getId())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .verify()
                    .path("toggleCreate", result -> result
                            .path("value").entity(Boolean.class).isEqualTo(dto.getValue())
                    );

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare.toggles")
                    .entityList(ToggleDto.class)
                    .hasSize(1);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare.togglesPaged.edges")
                    .entityList(ToggleDto.class)
                    .hasSize(1);

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_CONFLICT_when_creating_already_existing_value() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var type = persistType(randomizeType());
            final var dto = random.nextObject(ToggleCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/toggle/toggleCreate")
                    .variable("spexareId", spexare.getId())
                    .variable("typeId", type.getId())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .verify()
                    .path("toggleCreate", result -> result
                            .path("value").entity(Boolean.class).isEqualTo(dto.getValue())
                    );

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/toggle/toggleCreate")
                    .variable("spexareId", spexare.getId())
                    .variable("typeId", type.getId())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(CustomErrorType.CONFLICT.toString()))
                    )
                    .path("toggleCreate")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_NOT_FOUND_when_creating_and_spexare_not_found() {
            final var type = persistType(randomizeType());
            final var dto = random.nextObject(ToggleCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/toggle/toggleCreate")
                    .variable("spexareId", 1L)
                    .variable("typeId", type.getId())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("toggleCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_creating_and_type_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var dto = random.nextObject(ToggleCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/toggle/toggleCreate")
                    .variable("spexareId", spexare.getId())
                    .variable("typeId", "dummy")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("toggleCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_permission() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var type = persistType(randomizeType());
            final var dto = random.nextObject(ToggleCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/toggle/toggleCreate")
                    .variable("spexareId", spexare.getId())
                    .variable("typeId", type.getId())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("toggleCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_role() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var type = persistType(randomizeType());
            final var dto = random.nextObject(ToggleCreateDto.class);

            httpGraphQlTester
                    .mutate()
                    .build()
                    .documentName("spexare/toggle/toggleCreate")
                    .variable("spexareId", spexare.getId())
                    .variable("typeId", type.getId())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("toggleCreate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Update")
    class UpdateTests {

        @Test
        void should_update() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var type = persistType(randomizeType());
            final var toggle = persistToggle(randomizeToggle(type, spexare));
            toggle.setValue(Boolean.TRUE);
            repository.save(toggle);
            final var dto = random.nextObject(ToggleUpdateDto.class);
            dto.setId(toggle.getId());
            dto.setValue(Boolean.FALSE);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/toggle/toggleUpdate")
                    .variable("spexareId", spexare.getId())
                    .variable("typeId", type.getId())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .verify()
                    .path("toggleUpdate", result -> result
                            .path("value").entity(Boolean.class).isEqualTo(Boolean.FALSE)
                    );

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare.toggles")
                    .entityList(ToggleDto.class)
                    .hasSize(1);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare.togglesPaged.edges")
                    .entityList(ToggleDto.class)
                    .hasSize(1);

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_NOT_FOUND_when_updating_non_existing_value() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var type = persistType(randomizeType());
            final var dto = random.nextObject(ToggleUpdateDto.class);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/toggle/toggleUpdate")
                    .variable("spexareId", spexare.getId())
                    .variable("typeId", type.getId())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("toggleUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_updating_and_spexare_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var type = persistType(randomizeType());
            final var toggle = persistToggle(randomizeToggle(type, spexare));
            final var dto = random.nextObject(ToggleUpdateDto.class);
            dto.setId(toggle.getId());

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/toggle/toggleUpdate")
                    .variable("spexareId", 1L)
                    .variable("typeId", type.getId())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("toggleUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_NOT_FOUND_when_updating_and_type_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var type = persistType(randomizeType());
            final var toggle = persistToggle(randomizeToggle(type, spexare));
            final var dto = random.nextObject(ToggleUpdateDto.class);
            dto.setId(toggle.getId());

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/toggle/toggleUpdate")
                    .variable("spexareId", spexare.getId())
                    .variable("typeId", "dummy")
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("toggleUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_NOT_FOUND_when_updating_and_incorrect_spexare() {
            final var spexare1 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare1.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare1.getId()));
            final var spexare2 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare2.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare2.getId()));
            final var type = persistType(randomizeType());
            final var toggle = persistToggle(randomizeToggle(type, spexare2));
            final var dto = random.nextObject(ToggleUpdateDto.class);
            dto.setId(toggle.getId());

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/toggle/toggleUpdate")
                    .variable("spexareId", spexare1.getId())
                    .variable("typeId", type.getId())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("toggleUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_permission() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var type = persistType(randomizeType());
            final var toggle = persistToggle(randomizeToggle(type, spexare));
            final var dto = random.nextObject(ToggleUpdateDto.class);
            dto.setId(toggle.getId());

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/toggle/toggleUpdate")
                    .variable("spexareId", spexare.getId())
                    .variable("typeId", type.getId())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("toggleUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_role() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var type = persistType(randomizeType());
            final var toggle = randomizeToggle(type, spexare);
            final var dto = random.nextObject(ToggleUpdateDto.class);
            dto.setId(toggle.getId());

            httpGraphQlTester
                    .mutate()
                    .build()
                    .documentName("spexare/toggle/toggleUpdate")
                    .variable("spexareId", spexare.getId())
                    .variable("typeId", type.getId())
                    .variables(objectMapper.convertValue(dto, new TypeReference<>() {
                    }))
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("toggleUpdate")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        void should_delete() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var type = persistType(randomizeType());
            final var toggle = persistToggle(randomizeToggle(type, spexare));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/toggle/toggleDelete")
                    .variable("spexareId", spexare.getId())
                    .variable("typeId", type.getId())
                    .variable("id", toggle.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("toggleDelete")
                    .valueIsNull();

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare.toggles")
                    .entityList(ToggleDto.class)
                    .hasSize(0);

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/spexare")
                    .variable("id", spexare.getId())
                    .execute()
                    .errors()
                    .verify()
                    .path("spexare.togglesPaged.edges")
                    .entityList(ToggleDto.class)
                    .hasSize(0);

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_deleting_non_existing_value() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var type = persistType(randomizeType());

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/toggle/toggleDelete")
                    .variable("spexareId", spexare.getId())
                    .variable("typeId", type.getId())
                    .variable("id", 1L)
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("toggleDelete")
                    .valueIsNull();

            assertThat(repository.count()).isZero();
        }

        @Test
        void should_return_NOT_FOUND_when_deleting_and_spexare_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var type = persistType(randomizeType());
            final var toggle = persistToggle(randomizeToggle(type, spexare));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/toggle/toggleDelete")
                    .variable("spexareId", -1L)
                    .variable("typeId", type.getId())
                    .variable("id", toggle.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("toggleDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_NOT_FOUND_when_deleting_and_type_not_found() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var type = persistType(randomizeType());
            final var toggle = persistToggle(randomizeToggle(type, spexare));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/toggle/toggleDelete")
                    .variable("spexareId", spexare.getId())
                    .variable("typeId", "dummy")
                    .variable("id", toggle.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("toggleDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_NOT_FOUND_when_deleting_and_incorrect_spexare() {
            final var spexare1 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare1.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare1.getId()));
            final var spexare2 = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare2.getId()));
            grantWritePermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare2.getId()));
            final var type = persistType(randomizeType());
            final var toggle = persistToggle(randomizeToggle(type, spexare2));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/toggle/toggleDelete")
                    .variable("spexareId", spexare1.getId())
                    .variable("typeId", type.getId())
                    .variable("id", toggle.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("toggleDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_permission() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var type = persistType(randomizeType());
            final var toggle = persistToggle(randomizeToggle(type, spexare));

            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken()))
                    .build()
                    .documentName("spexare/toggle/toggleDelete")
                    .variable("spexareId", spexare.getId())
                    .variable("typeId", type.getId())
                    .variable("id", toggle.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("toggleDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        void should_return_FORBIDDEN_when_not_permitted_due_to_insufficient_role() {
            final var spexare = persistSpexare(randomizeSpexare());
            grantReadPermissionToRoleAdmin(toObjectIdentity(Spexare.class, spexare.getId()));
            final var type = persistType(randomizeType());
            final var toggle = persistToggle(randomizeToggle(type, spexare));

            httpGraphQlTester
                    .mutate()
                    .build()
                    .documentName("spexare/toggle/toggleDelete")
                    .variable("spexareId", spexare.getId())
                    .variable("typeId", type.getId())
                    .variable("id", toggle.getId())
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.FORBIDDEN.toString()))
                    )
                    .path("toggleDelete")
                    .valueIsNull();

            assertThat(repository.count()).isEqualTo(1);
        }
    }

    private Toggle randomizeToggle(final Type type, final Spexare spexare) {
        final var toggle = random.nextObject(Toggle.class);
        toggle.setSpexare(spexare);
        toggle.setType(type);
        return toggle;
    }

    private Toggle persistToggle(final Toggle toggle) {
        toggle.setId(null);

        return repository.save(toggle);
    }

    private Type randomizeType() {
        final var type = random.nextObject(Type.class);
        type.setType(TypeType.TOGGLE);
        return type;
    }

    private Type persistType(final Type type) {
        return typeRepository.save(type);
    }

    private Spexare randomizeSpexare() {
        return random.nextObject(Spexare.class);
    }

    private Spexare persistSpexare(final Spexare spexare) {
        spexare.setId(null);

        return spexareRepository.save(spexare);
    }

}
