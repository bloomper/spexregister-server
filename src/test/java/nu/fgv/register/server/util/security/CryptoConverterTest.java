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

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class CryptoConverterTest {

    @Test
    void should_encrypt_and_decrypt() {
        final CryptoConverter converter = new CryptoConverter("AES/CFB/PKCS5Padding", "Zr4t7w!z%C*F-JaNdRgUkXp2s5v8x/A?", "2546540121759905");

        final String plainValue = "whatever";
        final String encryptedValue = converter.convertToDatabaseColumn(plainValue);
        final String decryptedValue = converter.convertToEntityAttribute(encryptedValue);

        assertThat(decryptedValue, is(equalTo(plainValue)));
    }

}