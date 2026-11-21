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

import nu.fgv.register.server.util.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The API is stateless: the access token is the only credential, and a request with one leaves no session behind
 * that would still be logged in after the token has expired.
 */
class StatelessApiIntegrationTest extends AbstractIntegrationTest {

    private final HttpClient client = HttpClient.newHttpClient();
    @Value("${spring.graphql.http.path}")
    private String graphqlPath;

    @Autowired
    StatelessApiIntegrationTest(final JdbcClient jdbcClient, final Keycloak keycloakAdminClient, final String keycloakClientId,
                                final ObjectMapper objectMapper) {
        super(jdbcClient, keycloakAdminClient, keycloakClientId, objectMapper);
    }

    @Test
    void graphQlWithATokenSetsNoSessionCookie() throws IOException, InterruptedException {
        final HttpResponse<String> response = client.send(HttpRequest.newBuilder(uri(graphqlPath))
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"{ __typename }\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().allValues(HttpHeaders.SET_COOKIE)).isEmpty();
    }

    @Test
    void restWithATokenSetsNoSessionCookie() throws IOException, InterruptedException {
        final HttpResponse<String> response = client.send(HttpRequest.newBuilder(uri("/api/admin/jobs"))
                .header(HttpHeaders.AUTHORIZATION, obtainAdminAccessToken())
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().allValues(HttpHeaders.SET_COOKIE)).isEmpty();
    }

    private URI uri(final String path) {
        return URI.create("http://localhost:%s%s".formatted(localPort, path));
    }
}
