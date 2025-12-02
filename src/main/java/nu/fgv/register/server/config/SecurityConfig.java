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

package nu.fgv.register.server.config;

import nu.fgv.register.server.util.security.KeycloakJwtRolesConverter;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.ClientRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.DelegatingJwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final String keycloakUrl;
    private final String keycloakRealm;
    private final String keycloakAdminClientId;
    private final String keycloakAdminClientSecret;
    private final String keycloakClientClientId;
    private final String graphqlBaseUrl;

    public SecurityConfig(@Value("${spexregister.keycloak.url}") final String keycloakUrl,
                          @Value("${spexregister.keycloak.realm}") final String keycloakRealm,
                          @Value("${spexregister.keycloak.admin.client-id}") final String keycloakAdminClientId,
                          @Value("${spexregister.keycloak.admin.client-secret}") final String keycloakAdminClientSecret,
                          @Value("${spexregister.keycloak.client.client-id}") final String keycloakClientClientId,
                          @Value("${spring.graphql.path}") final String graphqlBaseUrl) {
        this.keycloakUrl = keycloakUrl;
        this.keycloakRealm = keycloakRealm;
        this.keycloakAdminClientId = keycloakAdminClientId;
        this.keycloakAdminClientSecret = keycloakAdminClientSecret;
        this.keycloakClientClientId = keycloakClientClientId;
        this.graphqlBaseUrl = graphqlBaseUrl;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers(HttpMethod.GET, "/favicon.ico").permitAll()
                                .requestMatchers(HttpMethod.GET, "/docs/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/graphiql/**").permitAll()
                                .requestMatchers(HttpMethod.POST, "%s/**".formatted(graphqlBaseUrl)).permitAll()
                                .requestMatchers(HttpMethod.GET, "%s/schema".formatted(graphqlBaseUrl)).permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/settings/**").permitAll()
                                .anyRequest().authenticated()
                )
                .securityContext(context -> context.requireExplicitSave(false))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));
        return http.build();
    }

    @Bean
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new NullAuthenticatedSessionStrategy();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        final DelegatingJwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new DelegatingJwtGrantedAuthoritiesConverter(
                new JwtGrantedAuthoritiesConverter(),
                new KeycloakJwtRolesConverter()
        );

        final JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }

    @Bean
    public Keycloak keycloakAdminClient() {
        return KeycloakBuilder.builder()
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .serverUrl(keycloakUrl)
                .realm(keycloakRealm)
                .clientId(keycloakAdminClientId)
                .clientSecret(keycloakAdminClientSecret)
                .build();
    }

    @Bean
    public String keycloakClientId(final Keycloak keycloakAdminClient) {
        return keycloakAdminClient
                .realm(keycloakRealm)
                .clients()
                .findByClientId(keycloakClientClientId)
                .stream()
                .filter(c -> c.getClientId().equals(keycloakClientClientId))
                .findFirst()
                .map(ClientRepresentation::getId)
                .orElseThrow(() -> new RuntimeException("Could not retrieve id of client in Keycloak"));
    }
}
