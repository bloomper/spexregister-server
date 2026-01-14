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

package nu.fgv.register.server.util.impex.exporting;

import org.jspecify.annotations.Nullable;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public record ReportModel<T>(
        @Nullable String name,
        Iterable<T> data,
        Class<T> clazz,
        boolean readOnly
) {
    public static <T> ReportModel<T> of(final Iterable<T> data, final Class<T> clazz) {
        return new ReportModel<>(null, data, clazz, false);
    }

    public static <T> ReportModel<T> of(final Iterable<T> data, final Class<T> clazz, final boolean readOnly) {
        return new ReportModel<>(null, data, clazz, readOnly);
    }

    public static <T> ReportModel<T> of(final String name, final Iterable<T> data, final Class<T> clazz) {
        return new ReportModel<>(name, data, clazz, false);
    }

    public static <T> ReportModel<T> of(@Nullable final String name, final Iterable<T> data, final Class<T> clazz, final boolean readOnly) {
        return new ReportModel<>(name, data, clazz, readOnly);
    }
}
