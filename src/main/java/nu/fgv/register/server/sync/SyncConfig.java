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

package nu.fgv.register.server.sync;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId;

/**
 * The HTTP client for Spexipedia, with the client credentials of {@code spexregister-service}.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Configuration
@EnableConfigurationProperties(SyncProperties.class)
public class SyncConfig {

    public static final String REGISTRATION_ID = "spexregister-service";
    public static final String REST_CLIENT = "spexipediaRestClient";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Bean
    public OAuth2AuthorizedClientManager serviceAuthorizedClientManager(final ClientRegistrationRepository clientRegistrationRepository,
                                                                        final OAuth2AuthorizedClientService authorizedClientService) {
        final AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);

        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
        return manager;
    }

    @Bean(REST_CLIENT)
    public RestClient spexipediaRestClient(final OAuth2AuthorizedClientManager serviceAuthorizedClientManager) {
        return RestClient.builder()
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(HttpClientSettings.defaults()
                        .withConnectTimeout(Duration.ofSeconds(5))
                        .withReadTimeout(TIMEOUT)))
                .requestInterceptor(new OAuth2ClientHttpRequestInterceptor(serviceAuthorizedClientManager))
                .defaultRequest(request -> request.attributes(clientRegistrationId(REGISTRATION_ID)))
                .build();
    }
}
