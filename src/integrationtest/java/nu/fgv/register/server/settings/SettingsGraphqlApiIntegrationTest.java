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

package nu.fgv.register.server.settings;

import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
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
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class SettingsGraphqlApiIntegrationTest extends AbstractGraphqlIntegrationTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public SettingsGraphqlApiIntegrationTest(final JdbcClient jdbcClient,
                                             final AclCache aclCache,
                                             final Keycloak keycloakAdminClient,
                                             final String keycloakClientId,
                                             final PermissionService permissionService) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService);
    }

    @BeforeEach
    void setUp() {
        httpGraphQlTester = HttpGraphQlTester.builder(
                        WebTestClient.bindToServer()
                                .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath)))
                .build();
    }

    @AfterEach
    void tearDown() {
    }

    @Nested
    @DisplayName("Retrieve language(s)")
    class RetrieveLanguageTests {

        @Test
        void should_return_many() {
            httpGraphQlTester
                    .documentName("settings/languages")
                    .execute()
                    .errors()
                    .verify()
                    .path("languages")
                    .entityList(LanguageDto.class)
                    .hasSize(2);
        }

        @Test
        void should_return_found() {
            httpGraphQlTester
                    .documentName("settings/language")
                    .variable("isoCode", "sv")
                    .execute()
                    .errors()
                    .verify()
                    .path("language", result -> result
                            .path("isoCode").entity(String.class).isEqualTo("sv")
                            .path("label").entity(String.class).isEqualTo("Svenska")
                    );
        }

        @Test
        void should_return_found_in_sv() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.ACCEPT_LANGUAGE, "sv"))
                    .build()
                    .documentName("settings/language")
                    .variable("isoCode", "sv")
                    .execute()
                    .errors()
                    .verify()
                    .path("language", result -> result
                            .path("isoCode").entity(String.class).isEqualTo("sv")
                            .path("label").entity(String.class).isEqualTo("Svenska")
                    );
        }

        @Test
        void should_return_found_in_en() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                    .build()
                    .documentName("settings/language")
                    .variable("isoCode", "sv")
                    .execute()
                    .errors()
                    .verify()
                    .path("language", result -> result
                            .path("isoCode").entity(String.class).isEqualTo("sv")
                            .path("label").entity(String.class).isEqualTo("Swedish")
                    );
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            httpGraphQlTester
                    .documentName("settings/language")
                    .variable("isoCode", "123")
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("language")
                    .valueIsNull();
        }
    }

    @Nested
    @DisplayName("Retrieve country(ies)")
    class RetrieveCountryTests {

        @Test
        void should_return_many() {
            httpGraphQlTester
                    .documentName("settings/countries")
                    .execute()
                    .errors()
                    .verify()
                    .path("countries")
                    .entityList(CountryDto.class)
                    .hasSize(249);
        }

        @Test
        void should_return_found() {
            httpGraphQlTester
                    .documentName("settings/country")
                    .variable("isoCode", "SE")
                    .execute()
                    .errors()
                    .verify()
                    .path("country", result -> result
                            .path("isoCode").entity(String.class).isEqualTo("SE")
                            .path("label").entity(String.class).isEqualTo("Sverige")
                    );
        }

        @Test
        void should_return_found_in_sv() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.ACCEPT_LANGUAGE, "sv"))
                    .build()
                    .documentName("settings/country")
                    .variable("isoCode", "SE")
                    .execute()
                    .errors()
                    .verify()
                    .path("country", result -> result
                            .path("isoCode").entity(String.class).isEqualTo("SE")
                            .path("label").entity(String.class).isEqualTo("Sverige")
                    );
        }

        @Test
        void should_return_found_in_en() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                    .build()
                    .documentName("settings/country")
                    .variable("isoCode", "SE")
                    .execute()
                    .errors()
                    .verify()
                    .path("country", result -> result
                            .path("isoCode").entity(String.class).isEqualTo("SE")
                            .path("label").entity(String.class).isEqualTo("Sweden")
                    );
        }

    }

    @Nested
    @DisplayName("Retrieve type(s)")
    class RetrieveTypeTests {

        @Test
        void should_return_many() {
            httpGraphQlTester
                    .documentName("settings/types")
                    .execute()
                    .errors()
                    .verify()
                    .path("types")
                    .entityList(TypeDto.class)
                    .hasSize(17);
        }

        @Test
        void should_return_many_of_type() {
            httpGraphQlTester
                    .documentName("settings/typesOfType")
                    .variable("type", TypeType.ADDRESS)
                    .execute()
                    .errors()
                    .verify()
                    .path("typesOfType")
                    .entityList(TypeDto.class)
                    .hasSize(3);
        }

        @Test
        void should_return_ValidationError_when_unknown_type() {
            httpGraphQlTester
                    .documentName("settings/typesOfType")
                    .variable("type", "whatever")
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals("ValidationError"))
                    );
        }

        @Test
        void should_return_found() {
            httpGraphQlTester
                    .documentName("settings/type")
                    .variable("type", TypeType.ADDRESS)
                    .variable("id", "HOME")
                    .execute()
                    .errors()
                    .verify()
                    .path("type", result -> result
                            .path("id").entity(String.class).isEqualTo("HOME")
                            .path("label").entity(String.class).isEqualTo("Hem")
                            .path("type").entity(TypeType.class).isEqualTo(TypeType.ADDRESS)
                    );
        }

        @Test
        void should_return_found_in_sv() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.ACCEPT_LANGUAGE, "sv"))
                    .build()
                    .documentName("settings/type")
                    .variable("type", TypeType.ADDRESS)
                    .variable("id", "HOME")
                    .execute()
                    .errors()
                    .verify()
                    .path("type", result -> result
                            .path("id").entity(String.class).isEqualTo("HOME")
                            .path("label").entity(String.class).isEqualTo("Hem")
                            .path("type").entity(TypeType.class).isEqualTo(TypeType.ADDRESS)
                    );
        }

        @Test
        void should_return_found_in_en() {
            httpGraphQlTester
                    .mutate()
                    .headers(headers -> headers.set(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                    .build()
                    .documentName("settings/type")
                    .variable("type", TypeType.ADDRESS)
                    .variable("id", "HOME")
                    .execute()
                    .errors()
                    .verify()
                    .path("type", result -> result
                            .path("id").entity(String.class).isEqualTo("HOME")
                            .path("label").entity(String.class).isEqualTo("Home")
                            .path("type").entity(TypeType.class).isEqualTo(TypeType.ADDRESS)
                    );
        }

        @Test
        void should_return_NOT_FOUND_when_not_found() {
            httpGraphQlTester
                    .documentName("settings/type")
                    .variable("type", TypeType.ADDRESS)
                    .variable("id", "whatever")
                    .execute()
                    .errors()
                    .satisfy((errors) -> assertThat(errors)
                            .anyMatch(error -> error.getExtensions().get("classification").toString().equals(ErrorType.NOT_FOUND.toString()))
                    )
                    .path("type")
                    .valueIsNull();
        }
    }
}
