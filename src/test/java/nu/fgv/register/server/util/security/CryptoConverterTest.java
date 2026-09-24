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

package nu.fgv.register.server.util.security;

import nu.fgv.register.server.util.error.InternalErrorException;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class CryptoConverterTest {

    private final CryptoConverter converter = new CryptoConverter("Zr4t7w!z%C*F-JaNdRgUkXp2s5v8x/A?");

    @Test
    void should_encrypt_and_decrypt() {
        final String plainValue = "19850101-1234";
        final String encryptedValue = converter.convertToDatabaseColumn(plainValue);

        assertThat(encryptedValue, is(not(equalTo(plainValue))));
        assertThat(converter.convertToEntityAttribute(encryptedValue), is(equalTo(plainValue)));
    }

    @Test
    void should_use_a_fresh_iv_for_every_value() {
        assertThat(converter.convertToDatabaseColumn("19850101-1234"), is(not(equalTo(converter.convertToDatabaseColumn("19850101-1234")))));
    }

    @Test
    void should_reject_tampered_value() {
        final String encryptedValue = converter.convertToDatabaseColumn("19850101-1234");
        final byte[] data = Base64.getDecoder().decode(encryptedValue);
        data[data.length - 1] ^= 1;
        final String tampered = Base64.getEncoder().encodeToString(data);

        assertThrows(InternalErrorException.class, () -> converter.convertToEntityAttribute(tampered));
    }

    @Test
    void should_pass_empty_values_through_as_null() {
        assertThat(converter.convertToDatabaseColumn(""), is(nullValue()));
        assertThat(converter.convertToEntityAttribute(null), is(nullValue()));
    }

}
