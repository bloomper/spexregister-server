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

import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * A non-fatal condition encountered while restoring.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Builder
public record RestoreWarningDto(
        String code,
        String message,
        @Nullable Long previousId,
        @Nullable Long newId
) {
    public static final String ID_REASSIGNED = "ID_REASSIGNED";

    public static RestoreWarningDto idReassigned(final AuditedType type, final Long previousId, final Long newId) {
        return RestoreWarningDto.builder()
                .code(ID_REASSIGNED)
                .message("%s %d was re-created as %d".formatted(type, previousId, newId))
                .previousId(previousId)
                .newId(newId)
                .build();
    }
}
