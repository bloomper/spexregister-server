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

package nu.fgv.register.server.impex.exporting;

import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
public abstract class AbstractExportService implements ExportService {

    protected final List<ExportEngine> engines;

    protected AbstractExportService(final List<ExportEngine> engines) {
        this.engines = engines;
    }

    @Override
    public List<ExportHolder<?>> doExport(final List<Long> ids, final String filter) {
        return getReports(ids, filter);
    }

    protected abstract List<ExportHolder<?>> getReports(final List<Long> ids, final String filter);

    protected <T, R> Iterable<R> toImpexDto(final Iterable<T> entities, final Function<T, R> mapper) {
        return () -> {
            final var iterator = entities.iterator();

            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public R next() {
                    return mapper.apply(iterator.next());
                }
            };
        };
    }
}