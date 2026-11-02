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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class AddressQueryTest {

    @Test
    void should_ignore_case_and_spacing_in_the_key() {
        assertThat(new AddressQuery(" Kemigården  1", "412 96", "GÖTEBORG", "se").key())
                .isEqualTo(new AddressQuery("kemigården 1", "412 96", "göteborg", "SE").key());
    }

    @Test
    void should_change_the_key_when_a_part_changes() {
        assertThat(new AddressQuery("Kemigården 1", "412 96", "Göteborg", null).key())
                .isNotEqualTo(new AddressQuery("Kemigården 2", "412 96", "Göteborg", null).key());
    }

    @Test
    void should_drop_the_street() {
        assertThat(new AddressQuery("Kemigården 1", "412 96", "Göteborg", "SE").withoutStreet())
                .isEqualTo(new AddressQuery(null, "412 96", "Göteborg", "SE"));
    }
}
