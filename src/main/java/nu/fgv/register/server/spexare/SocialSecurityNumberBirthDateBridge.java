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

package nu.fgv.register.server.spexare;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.jspecify.annotations.Nullable;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class SocialSecurityNumberBirthDateBridge implements ValueBridge<String, String> {

    private static final int BIRTH_DATE_LENGTH = 8;

    @Override
    public @Nullable String toIndexedValue(@Nullable final String value, final ValueBridgeToIndexedValueContext context) {
        final String digits = value == null ? "" : value.replaceAll("\\D", "");

        return digits.length() >= BIRTH_DATE_LENGTH ? digits.substring(0, BIRTH_DATE_LENGTH) : null;
    }
}
