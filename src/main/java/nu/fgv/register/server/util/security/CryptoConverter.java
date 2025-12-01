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

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.error.InternalErrorException;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;

import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Converter
public class CryptoConverter implements AttributeConverter<String, String> {

    private final byte[] secretKey;
    private final IvParameterSpec iv;
    private final Cipher cipher;

    public CryptoConverter(
            @Value("${spexregister.crypto.algorithm}") final String algorithm,
            @Value("${spexregister.crypto.secret-key}") final String secretKey,
            @Value("${spexregister.crypto.initialization-vector}") final String iv) {
        this.secretKey = secretKey.getBytes(StandardCharsets.UTF_8);
        this.iv = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
        try {
            cipher = Cipher.getInstance(algorithm);
        } catch (final Exception e) {
            log.error("Error during initialization", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    @Nullable
    public synchronized String convertToDatabaseColumn(final String plainValue) {
        if (hasText(plainValue)) {
            final Key key = new SecretKeySpec(secretKey, "AES");

            try {
                cipher.init(Cipher.ENCRYPT_MODE, key, iv); // NOSONAR
                return Base64.getEncoder().encodeToString(cipher.doFinal(plainValue.getBytes()));
            } catch (final Exception e) {
                log.error("Unexpected error during encryption", e);
                throw new InternalErrorException(e.getMessage());
            }
        } else {
            return null;
        }
    }

    @Override
    @Nullable
    public synchronized String convertToEntityAttribute(final String encryptedValue) {
        if (hasText(encryptedValue)) {
            final Key key = new SecretKeySpec(secretKey, "AES");

            try {
                cipher.init(Cipher.DECRYPT_MODE, key, iv);
                return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedValue)));
            } catch (final Exception e) {
                log.error("Unexpected error during decryption", e);
                throw new InternalErrorException(e.getMessage());
            }
        } else {
            return null;
        }
    }
}
