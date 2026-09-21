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

package nu.fgv.register.server.analytics;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Builder
public record BucketDto(
        String key,
        String label,
        Long count,
        @Nullable String facet
) {

    public static BucketDto of(final String key, final String label, final Long count, final @Nullable String facet) {
        return BucketDto.builder().key(key).label(label).count(count).facet(facet).build();
    }

    public static BucketDto of(final String key, final String label, final Long count) {
        return of(key, label, count, null);
    }
}
