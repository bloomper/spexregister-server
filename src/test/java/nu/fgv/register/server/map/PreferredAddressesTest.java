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
import nu.fgv.register.server.map.PreferredAddresses.Candidate;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.is;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class PreferredAddressesTest {

    private static final AddressQuery HOME = new AddressQuery("Hemvägen 1", "41296", "Göteborg", "SE");
    private static final AddressQuery OTHER = new AddressQuery("Sommarstigen 2", "45196", "Uddevalla", "SE");
    private static final AddressQuery WORK = new AddressQuery("Kontorsgatan 3", "11122", "Stockholm", "SE");

    @Test
    void should_prefer_home_then_other_then_work() {
        final Map<Long, AddressQuery> chosen = PreferredAddresses.choose(List.of(
                new Candidate(1L, "WORK", WORK),
                new Candidate(1L, "HOME", HOME),
                new Candidate(1L, "OTHER", OTHER),
                new Candidate(2L, "WORK", WORK),
                new Candidate(2L, "OTHER", OTHER),
                new Candidate(3L, "WORK", WORK)
        ));

        assertThat(chosen, is(Map.of(1L, HOME, 2L, OTHER, 3L, WORK)));
    }

    @Test
    void should_skip_addresses_without_postal_code_or_city() {
        final Map<Long, AddressQuery> chosen = PreferredAddresses.choose(List.of(
                new Candidate(1L, "HOME", new AddressQuery("Hemvägen 1", " ", null, "SE"))
        ));

        assertThat(chosen, is(anEmptyMap()));
    }

    @Test
    void should_fall_back_on_an_unknown_type() {
        final Map<Long, AddressQuery> chosen = PreferredAddresses.choose(List.of(
                new Candidate(1L, null, OTHER),
                new Candidate(2L, "UNKNOWN", WORK),
                new Candidate(2L, "WORK", HOME)
        ));

        assertThat(chosen, is(Map.of(1L, OTHER, 2L, HOME)));
    }
}
