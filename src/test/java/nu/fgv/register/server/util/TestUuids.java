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

import java.util.UUID;

/**
 * Fixed external ids for tests.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
public final class TestUuids {

    private TestUuids() {
    }

    /**
     * @param n a number to tell the ids apart
     * @return the same id for the same number
     */
    public static UUID uuid(final long n) {
        return new UUID(0x0000000000007000L, 0x8000000000000000L | n);
    }
}
