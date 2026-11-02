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

import nu.fgv.register.server.map.Geocoder.AddressQuery;
import nu.fgv.register.server.map.Geocoder.Coordinates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParamCount;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class NominatimGeocoderTest {

    private MockRestServiceServer server;
    private NominatimGeocoder geocoder;

    @BeforeEach
    void setUp() {
        final RestClient.Builder builder = RestClient.builder().baseUrl("https://nominatim.test");

        server = MockRestServiceServer.bindTo(builder).build();
        geocoder = new NominatimGeocoder(builder.build(), "admin@example.com", "SE");
    }

    @Test
    void should_locate_a_structured_address() {
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(startsWith("https://nominatim.test/search?")))
                .andExpect(queryParam("format", "jsonv2"))
                .andExpect(queryParam("limit", "1"))
                .andExpect(queryParam("street", "Kemig%C3%A5rden%201"))
                .andExpect(queryParam("postalcode", "412%2096"))
                .andExpect(queryParam("city", "G%C3%B6teborg"))
                .andExpect(queryParam("countrycodes", "se"))
                .andExpect(queryParam("email", "admin@example.com"))
                .andRespond(withSuccess("[{\"lat\":\"57.6893\",\"lon\":\"11.9776\"}]", MediaType.APPLICATION_JSON));

        assertThat(geocoder.locate(new AddressQuery(" Kemigården 1 ", "412 96", "Göteborg", null)))
                .contains(new Coordinates(57.6893, 11.9776));
        server.verify();
    }

    @Test
    void should_leave_out_missing_parts_and_use_the_address_country() {
        final NominatimGeocoder withoutEmail = new NominatimGeocoder(clientFor(), null, "SE");

        server.expect(queryParamCount(4))
                .andExpect(queryParam("city", "Oslo"))
                .andExpect(queryParam("countrycodes", "no"))
                .andRespond(withSuccess("[{\"lat\":\"59.91\",\"lon\":\"10.75\"}]", MediaType.APPLICATION_JSON));

        assertThat(withoutEmail.locate(new AddressQuery(null, " ", "Oslo", "NO"))).contains(new Coordinates(59.91, 10.75));
        server.verify();
    }

    @Test
    void should_be_empty_when_nothing_is_found() {
        server.expect(method(HttpMethod.GET)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(geocoder.locate(new AddressQuery("Nowhere 1", null, "Nowhere", null))).isEmpty();
    }

    @Test
    void should_be_empty_when_the_coordinates_are_unreadable() {
        server.expect(method(HttpMethod.GET)).andRespond(withSuccess("[{\"lat\":\"north\",\"lon\":\"11.9\"}]", MediaType.APPLICATION_JSON));

        assertThat(geocoder.locate(new AddressQuery(null, null, "Göteborg", null))).isEmpty();
    }

    @Test
    void should_throw_when_the_service_fails() {
        server.expect(method(HttpMethod.GET)).andRespond(withServerError());

        assertThatThrownBy(() -> geocoder.locate(new AddressQuery(null, null, "Göteborg", null)))
                .isInstanceOf(Geocoder.GeocodingException.class);
    }

    private RestClient clientFor() {
        final RestClient.Builder builder = RestClient.builder().baseUrl("https://nominatim.test");

        server = MockRestServiceServer.bindTo(builder).build();
        return builder.build();
    }
}
