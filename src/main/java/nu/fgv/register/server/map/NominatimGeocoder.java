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


package nu.fgv.register.server.map;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Geocodes with <a href="https://nominatim.org">Nominatim</a>, by default the public OpenStreetMap instance. Its usage
 * policy asks for an identifying User-Agent, a contact email and at most one request a second; the caller keeps to the
 * rate.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Component
public class NominatimGeocoder implements Geocoder {

    private final RestClient client;
    private final @Nullable String email;
    private final @Nullable String defaultCountry;

    @Autowired
    public NominatimGeocoder(@Value("${spexregister.geocoding.url}") final String url,
                             @Value("${spexregister.geocoding.user-agent}") final String userAgent,
                             @Value("${spexregister.geocoding.email:}") final @Nullable String email,
                             @Value("${spexregister.geocoding.default-country:}") final @Nullable String defaultCountry) {
        this(RestClient.builder().baseUrl(url).defaultHeader(HttpHeaders.USER_AGENT, userAgent).build(), email, defaultCountry);
    }

    NominatimGeocoder(final RestClient client, final @Nullable String email, final @Nullable String defaultCountry) {
        this.client = client;
        this.email = StringUtils.hasText(email) ? email : null;
        this.defaultCountry = StringUtils.hasText(defaultCountry) ? defaultCountry : null;
    }

    @Override
    public Optional<Coordinates> locate(final AddressQuery address) {
        try {
            final List<Place> places = client.get()
                    .uri(builder -> uri(builder, address))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Place>>() {
                    });

            return Optional.ofNullable(places)
                    .flatMap(list -> list.stream().findFirst())
                    .flatMap(Place::coordinates);
        } catch (final RestClientException e) {
            throw new GeocodingException("Nominatim lookup failed", e);
        }
    }

    private URI uri(final UriBuilder builder, final AddressQuery address) {
        builder.path("/search")
                .queryParam("format", "jsonv2")
                .queryParam("limit", 1);
        optional(builder, "street", address.streetAddress());
        optional(builder, "postalcode", address.postalCode());
        optional(builder, "city", address.city());
        optional(builder, "countrycodes", Optional.ofNullable(StringUtils.hasText(address.country()) ? address.country() : defaultCountry)
                .map(country -> country.toLowerCase(Locale.ROOT))
                .orElse(null));
        optional(builder, "email", email);

        return builder.build();
    }

    private static void optional(final UriBuilder builder, final String name, final @Nullable String value) {
        if (StringUtils.hasText(value)) {
            builder.queryParam(name, value.trim());
        }
    }

    record Place(@Nullable String lat, @Nullable String lon) {

        Optional<Coordinates> coordinates() {
            try {
                return lat == null || lon == null
                        ? Optional.empty()
                        : Optional.of(new Coordinates(Double.parseDouble(lat), Double.parseDouble(lon)));
            } catch (final NumberFormatException e) {
                log.warn("Nominatim returned unreadable coordinates: {}, {}", lat, lon);
                return Optional.empty();
            }
        }
    }
}
