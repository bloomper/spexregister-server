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
import org.springaicommunity.mcp.security.server.config.McpServerOAuth2Configurer;
import org.keycloak.admin.client.JacksonProvider;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.ClientRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.DelegatingJwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

import java.util.List;

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
                          @Value("${spring.graphql.http.path}") final String graphqlBaseUrl) {
        this.keycloakUrl = keycloakUrl;
        this.keycloakRealm = keycloakRealm;
        this.keycloakAdminClientId = keycloakAdminClientId;
        this.keycloakAdminClientSecret = keycloakAdminClientSecret;
        this.keycloakClientClientId = keycloakClientClientId;
        this.graphqlBaseUrl = graphqlBaseUrl;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain mcpSecurityFilterChain(final HttpSecurity http,
                                                      @Value("${spring.ai.mcp.server.streamable-http.mcp-endpoint}") final String mcpEndpoint,
                                                      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") final String issuerUri,
                                                      @Value("${spexregister.mcp.authorized-parties}") final List<String> authorizedParties) {
        final JwtDecoder jwtDecoder = new SupplierJwtDecoder(() -> {
            final NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                    JwtValidators.createDefaultWithIssuer(issuerUri),
                    new JwtClaimValidator<String>("azp", authorizedParties::contains),
                    new JwtClaimValidator<String>(JwtClaimNames.SUB, StringUtils::hasText),
                    new JwtClaimValidator<String>("email", StringUtils::hasText)
            ));
            return decoder;
        });

        http
                .securityMatcher(mcpEndpoint, "/.well-known/oauth-protected-resource/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers(HttpMethod.GET, "/.well-known/oauth-protected-resource/**").permitAll()
                                .anyRequest().authenticated()
                )
                .securityContext(context -> context.requireExplicitSave(false))
                .with(McpServerOAuth2Configurer.mcpServerOAuth2(), mcp -> mcp
                        .authorizationServer(issuerUri)
                        .resourcePath(mcpEndpoint)
                        .resourceName("Spexregister")
                        .validateAudienceClaim(false)
                        .jwtDecoder(jwtDecoder));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers(HttpMethod.GET, "/favicon.ico").permitAll()
                                .requestMatchers(HttpMethod.GET, "/docs/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "%s".formatted(graphqlBaseUrl)).permitAll()
                                .requestMatchers(HttpMethod.POST, "%s/**".formatted(graphqlBaseUrl)).permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/settings/**").permitAll()
                                .anyRequest().authenticated()
                )
                .securityContext(context -> context.requireExplicitSave(false))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));
        return http.build();
    }

    @Bean
    public OAuth2TokenValidator<Jwt> authorizedPartyValidator(@Value("${spexregister.security.authorized-parties:${spexregister.keycloak.client.client-id}}") final List<String> authorizedParties) {
        return new JwtClaimValidator<String>("azp", authorizedParties::contains);
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
    public Keycloak keycloakAdminClient(final Environment env) {
        final KeycloakBuilder builder = KeycloakBuilder.builder()
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .serverUrl(keycloakUrl)
                .realm(keycloakRealm)
                .clientId(keycloakAdminClientId)
                .clientSecret(keycloakAdminClientSecret);

        if (env.acceptsProfiles(Profiles.of("local"))) {
            builder.resteasyClient(
                    new org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl()
                            .disableTrustManager()
                            .register(JacksonProvider.class)
                            .build()
            );
        }

        return builder.build();
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
