package nu.fgv.register.server.config;

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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final String keycloakUrl;
    private final String keycloakRealm;
    private final String keycloakAdminClientId;
    private final String keycloakAdminClientSecret;
    private final String keycloakClientClientId;

    public SecurityConfig(@Value("${spexregister.keycloak.url}") final String keycloakUrl,
                          @Value("${spexregister.keycloak.realm}") final String keycloakRealm,
                          @Value("${spexregister.keycloak.admin.client-id}") final String keycloakAdminClientId,
                          @Value("${spexregister.keycloak.admin.client-secret}") final String keycloakAdminClientSecret,
                          @Value("${spexregister.keycloak.client.client-id}") final String keycloakClientClientId) {
        this.keycloakUrl = keycloakUrl;
        this.keycloakRealm = keycloakRealm;
        this.keycloakAdminClientId = keycloakAdminClientId;
        this.keycloakAdminClientSecret = keycloakAdminClientSecret;
        this.keycloakClientClientId = keycloakClientClientId;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers(HttpMethod.GET, "/api/v1/settings/**").permitAll()
                                .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));
        return http.build();
    }

    @Bean
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new NullAuthenticatedSessionStrategy();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        final JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities");
        grantedAuthoritiesConverter.setAuthorityPrefix("");

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
