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

package nu.fgv.register.server.util.search;

import org.springframework.data.domain.ScrollPosition;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
public class WindowWithFacetsImpl<T> implements WindowWithFacets<T> {
    private final List<T> items;
    private final IntFunction<? extends ScrollPosition> positionFunction;
    private final boolean hasNext;
    private final List<Facet> facets;

    public WindowWithFacetsImpl(final List<T> items, final IntFunction<? extends ScrollPosition> positionFunction, final boolean hasNext, final List<Facet> facets) {
        this.items = items;
        this.positionFunction = positionFunction;
        this.hasNext = hasNext;
        this.facets = facets;
    }

    @Override
    public List<Facet> getFacets() {
        return facets;
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public List<T> getContent() {
        return items;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public ScrollPosition positionAt(final int index) {
        if (index >= 0 && index < size()) {
            return positionFunction.apply(index);
        } else {
            throw new IndexOutOfBoundsException(index);
        }
    }

    @Override
    public <U> WindowWithFacets<U> map(final Function<? super T, ? extends U> converter) {
        Assert.notNull(converter, "Function must not be null");

        return new WindowWithFacetsImpl<>(
                stream()
                        .map(converter)
                        .collect(Collectors.toList()),
                positionFunction,
                hasNext,
                facets);
    }

    @Override
    @NonNull
    public Iterator<T> iterator() {
        return items.iterator();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (o != null && getClass() == o.getClass()) {
            final WindowWithFacetsImpl<?> that = (WindowWithFacetsImpl<?>)o;

            return ObjectUtils.nullSafeEquals(items, that.items) &&
                    ObjectUtils.nullSafeEquals(positionFunction, that.positionFunction) &&
                    ObjectUtils.nullSafeEquals(hasNext, that.hasNext) &&
                    ObjectUtils.nullSafeEquals(facets, that.facets);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = ObjectUtils.nullSafeHashCode(items);
        result = 31 * result + ObjectUtils.nullSafeHashCode(positionFunction);
        result = 31 * result + ObjectUtils.nullSafeHashCode(hasNext);
        result = 31 * result + ObjectUtils.nullSafeHashCode(facets);

        return result;
    }

    public String toString() {
        return "WindowWithFacets " + items;
    }
}
