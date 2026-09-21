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

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Getter
public enum DataQualityIssue {

    NO_ADDRESS("noAddress"),
    NO_EMAIL_ADDRESS("noEmailAddress"),
    NO_PHONE("noPhone"),
    NO_IMAGE("noImage"),
    NO_MEMBERSHIP("noMembership"),
    NO_CONSENT("noConsent"),
    NO_ACTIVITY("noActivity"),
    NO_SOCIAL_SECURITY_NUMBER("noSocialSecurityNumber"),
    NO_TAG("noTag");

    private final String key;

    DataQualityIssue(final String key) {
        this.key = key;
    }

    public static Optional<DataQualityIssue> fromKey(final String key) {
        return Arrays.stream(values())
                .filter(issue -> issue.key.equalsIgnoreCase(key))
                .findFirst();
    }
}
