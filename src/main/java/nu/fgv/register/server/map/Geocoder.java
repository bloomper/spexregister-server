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

import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Turns an address into coordinates.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
public interface Geocoder {

    /**
     * @param address the address
     * @return where the address is, or empty when it could not be found
     * @throws GeocodingException when the lookup itself failed, so it is worth trying again later
     */
    Optional<Coordinates> locate(AddressQuery address);

    /**
     * An address to look up. Any part may be missing.
     *
     * @param streetAddress the street and number
     * @param postalCode    the postal code
     * @param city          the city
     * @param country       the ISO 3166-1 alpha-2 country code
     */
    record AddressQuery(@Nullable String streetAddress, @Nullable String postalCode, @Nullable String city,
                        @Nullable String country) {

        /**
         * @return the same address without the street, to fall back on when the full address is not found
         */
        public AddressQuery withoutStreet() {
            return new AddressQuery(null, postalCode, city, country);
        }

        /**
         * @return the address as one string, which changes whenever any part of it does
         */
        public String key() {
            return String.join("|", part(streetAddress), part(postalCode), part(city), part(country));
        }

        private static String part(final @Nullable String value) {
            return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase();
        }
    }

    /**
     * A position in WGS 84.
     *
     * @param latitude  the latitude
     * @param longitude the longitude
     */
    record Coordinates(double latitude, double longitude) {
    }

    /**
     * The geocoding service could not be reached or answered with an error.
     */
    class GeocodingException extends RuntimeException {

        public GeocodingException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
