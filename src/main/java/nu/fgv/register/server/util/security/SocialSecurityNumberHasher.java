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

import org.jspecify.annotations.Nullable;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

/**
 * A keyed HMAC of the digits of a Swedish social security number (12 digits) or a birth date (8 digits), so the search
 * index can match a number without holding it in clear text. The key is derived from the encryption key.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
public final class SocialSecurityNumberHasher {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String KEY_LABEL = "spexregister:social-security-number-index";
    private static final int BIRTH_DATE_LENGTH = 8;
    private static final int NUMBER_LENGTH = 12;

    private final SecretKeySpec key;

    /**
     * @param secretKey the encryption key, from which the HMAC key is derived
     */
    public SocialSecurityNumberHasher(final String secretKey) {
        this.key = new SecretKeySpec(hmac(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM), KEY_LABEL), ALGORITHM);
    }

    /**
     * @param value a search term
     * @return whether it has the 12 digits of a full number
     */
    public static boolean isNumber(final String value) {
        return digitsOf(value).length() == NUMBER_LENGTH;
    }

    /**
     * @param value a search term
     * @return whether it has the 8 digits of a birth date
     */
    public static boolean isBirthDate(final String value) {
        return digitsOf(value).length() == BIRTH_DATE_LENGTH;
    }

    /**
     * @param value a number or birth date, with or without separators
     * @return the hash of its digits, or null when they are neither a full number nor a birth date
     */
    public @Nullable String hashNumber(@Nullable final String value) {
        final String digits = digitsOf(value);

        return digits.length() == NUMBER_LENGTH || digits.length() == BIRTH_DATE_LENGTH
                ? Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(key, digits))
                : null;
    }

    private static byte[] hmac(final SecretKeySpec key, final String value) {
        try {
            final Mac mac = Mac.getInstance(ALGORITHM);

            mac.init(key);
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException("Could not hash social security number", e);
        }
    }

    private static String digitsOf(@Nullable final String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }
}
