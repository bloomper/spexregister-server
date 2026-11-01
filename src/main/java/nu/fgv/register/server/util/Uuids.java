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

package nu.fgv.register.server.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Creates the external ids that entities are known by outside the system.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
public final class Uuids {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Uuids() {
    }

    /**
     * @return a new time-ordered UUID (version 7, RFC 9562)
     */
    public static UUID v7() {
        final long millis = System.currentTimeMillis();
        final long mostSignificantBits = (millis << 16) | 0x7000L | (RANDOM.nextLong() & 0x0FFFL);
        final long leastSignificantBits = (RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;

        return new UUID(mostSignificantBits, leastSignificantBits);
    }
}
