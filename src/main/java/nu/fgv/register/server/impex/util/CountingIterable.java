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

package nu.fgv.register.server.impex.util;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.step.StepExecution;

import java.util.Iterator;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@RequiredArgsConstructor
public class CountingIterable<T> implements Iterable<T> {
    private final Iterable<T> delegate;
    private final StepExecution stepExecution;

    @Override
    public Iterator<T> iterator() {
        final Iterator<T> iterator = delegate.iterator();

        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                final T item = iterator.next();

                stepExecution.setReadCount(stepExecution.getReadCount() + 1);

                return item;
            }
        };
    }
}
