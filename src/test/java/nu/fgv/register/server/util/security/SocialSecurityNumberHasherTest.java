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

package nu.fgv.register.server.util.security;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class SocialSecurityNumberHasherTest {

    private final SocialSecurityNumberHasher hasher = new SocialSecurityNumberHasher("Zr4t7w!z%C*F-JaNdRgUkXp2s5v8x/A?");

    @Test
    void should_hash_a_number_the_same_with_or_without_dash() {
        assertThat(hasher.hashNumber("19850101-1234"), is(equalTo(hasher.hashNumber("198501011234"))));
    }

    @Test
    void should_not_expose_the_plain_value() {
        assertThat(hasher.hashNumber("19850101-1234"), is(not(equalTo("198501011234"))));
    }

    @Test
    void should_tell_a_full_number_from_its_birth_date() {
        assertThat(hasher.hashNumber("19850101"), is(not(equalTo(hasher.hashNumber("19850101-1234")))));
    }

    @Test
    void should_depend_on_the_key() {
        final SocialSecurityNumberHasher other = new SocialSecurityNumberHasher("another-key-of-thirty-two-bytes!");

        assertThat(other.hashNumber("19850101-1234"), is(not(equalTo(hasher.hashNumber("19850101-1234")))));
    }

    @Test
    void should_not_hash_what_is_not_a_social_security_number() {
        assertThat(hasher.hashNumber(null), is(nullValue()));
        assertThat(hasher.hashNumber("1985"), is(nullValue()));
        assertThat(hasher.hashNumber("Anna"), is(nullValue()));
    }

    @Test
    void should_recognise_search_terms() {
        assertThat(SocialSecurityNumberHasher.isNumber("19850101-1234"), is(true));
        assertThat(SocialSecurityNumberHasher.isBirthDate("19850101"), is(true));
        assertThat(SocialSecurityNumberHasher.isNumber("Anna"), is(false));
    }
}
