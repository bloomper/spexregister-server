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

package nu.fgv.register.server.audit;

import org.jspecify.annotations.Nullable;

import java.util.regex.Pattern;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Where the changes of the current thread come from, recorded on each Envers revision. Set per HTTP request by
 * {@link AuditContextFilter}, named after the GraphQL operation by {@link AuditOperationInstrumentation}, and carried
 * across threads by {@link AuditContextAccessor}.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
public final class AuditContext {

    static final int MAX_OPERATION_LENGTH = 255;
    static final int MAX_COMMENT_LENGTH = 512;
    static final int MAX_ACTING_PERSON_LENGTH = 255;

    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("[\\p{Cc}\\p{Cf}\\p{Zl}\\p{Zp}]");

    private static final ThreadLocal<@Nullable Origin> CURRENT = new ThreadLocal<>();

    private AuditContext() {
    }

    /**
     * @return the origin of the current thread, or null when none is set
     */
    public static @Nullable Origin current() {
        return CURRENT.get();
    }

    /**
     * @return the origin of the current thread, or {@link AuditSource#SYSTEM} without operation and comment
     */
    public static Origin currentOrSystem() {
        final Origin current = CURRENT.get();

        return current == null ? new Origin(AuditSource.SYSTEM, null, null) : current;
    }

    static void set(final Origin origin) {
        CURRENT.set(origin);
    }

    static void clear() {
        CURRENT.remove();
    }

    /**
     * Sets the origin until the current transaction completes, then restores the previous one. Outside a transaction it
     * stays set.
     *
     * @param origin the origin of the changes to come
     */
    public static void stamp(final Origin origin) {
        final Origin previous = CURRENT.get();

        CURRENT.set(origin);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(final int status) {
                    if (previous == null) {
                        CURRENT.remove();
                    } else {
                        CURRENT.set(previous);
                    }
                }
            });
        }
    }

    private static @Nullable String truncate(final @Nullable String value, final int max) {
        if (value == null) {
            return null;
        }

        final String trimmed = value.trim();

        if (trimmed.isEmpty()) {
            return null;
        }

        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private static @Nullable String withoutControlCharacters(final @Nullable String value) {
        return value == null ? null : CONTROL_CHARACTERS.matcher(value).replaceAll(" ");
    }

    /**
     * Where a change comes from.
     *
     * @param source        what kind of client made it
     * @param operation     the GraphQL operation or HTTP request
     * @param comment       why, as the client gave it
     * @param actingPerson  who a service account acted for, as the client gave it; only {@link Auditor} may decide
     *                      whether to believe it. Control characters become spaces. It is personal data: never log it.
     */
    public record Origin(AuditSource source, @Nullable String operation, @Nullable String comment, @Nullable String actingPerson) {

        public Origin {
            operation = truncate(operation, MAX_OPERATION_LENGTH);
            comment = truncate(comment, MAX_COMMENT_LENGTH);
            actingPerson = truncate(withoutControlCharacters(actingPerson), MAX_ACTING_PERSON_LENGTH);
        }

        public Origin(final AuditSource source, final @Nullable String operation, final @Nullable String comment) {
            this(source, operation, comment, null);
        }

        public Origin withSource(final AuditSource source) {
            return new Origin(source, operation, comment, actingPerson);
        }

        public Origin withOperation(final @Nullable String operation) {
            return new Origin(source, operation, comment, actingPerson);
        }

        public Origin withCommentIfAbsent(final @Nullable String fallback) {
            return comment == null ? new Origin(source, operation, fallback, actingPerson) : this;
        }

        @Override
        public String toString() {
            return "Origin[source=%s, operation=%s, comment=%s, actingPerson=%s]".formatted(source, operation, comment, actingPerson == null ? null : "<redacted>");
        }
    }
}
