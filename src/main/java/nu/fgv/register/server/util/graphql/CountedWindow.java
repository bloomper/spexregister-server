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

package nu.fgv.register.server.util.graphql;

import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.util.Assert;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * A {@link Window} that also knows the total number of elements matching the underlying query.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
public interface CountedWindow<T> extends Window<T>, TotalCountAware {

    static <T> CountedWindow<T> of(final Window<T> window, final long totalCount) {
        Assert.notNull(window, "Window must not be null");

        return window instanceof final CountedWindow<T> counted && counted.getTotalCount() == totalCount ?
                counted :
                new DelegatingCountedWindow<>(window, totalCount);
    }

    final class DelegatingCountedWindow<T> implements CountedWindow<T> {

        private final Window<T> delegate;
        private final long totalCount;

        private DelegatingCountedWindow(final Window<T> delegate, final long totalCount) {
            this.delegate = delegate;
            this.totalCount = totalCount;
        }

        @Override
        public long getTotalCount() {
            return totalCount;
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public List<T> getContent() {
            return delegate.getContent();
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public ScrollPosition positionAt(final int index) {
            return delegate.positionAt(index);
        }

        @Override
        public <U> CountedWindow<U> map(final Function<? super T, ? extends U> converter) {
            return new DelegatingCountedWindow<>(delegate.map(converter), totalCount);
        }

        @Override
        public Iterator<T> iterator() {
            return delegate.iterator();
        }
    }
}
