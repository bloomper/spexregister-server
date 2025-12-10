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

package nu.fgv.register.server.acl;

import jakarta.persistence.Query;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.projection.ProjectionFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
abstract class FluentQuerySupport<S, R> {
    protected final Class<R> resultType;
    protected final Sort sort;
    protected final int limit;
    protected final Set<String> properties;
    protected final Class<S> entityType;
    protected final ProjectionFactory projectionFactory;

    FluentQuerySupport(final Class<R> resultType,
                       final Sort sort,
                       final int limit,
                       @Nullable final Collection<String> properties,
                       final Class<S> entityType,
                       final ProjectionFactory projectionFactory) {
        this.resultType = resultType;
        this.sort = sort;
        this.limit = limit;
        if (properties != null) {
            this.properties = new HashSet<>(properties);
        } else {
            this.properties = Collections.emptySet();
        }
        this.entityType = entityType;
        this.projectionFactory = projectionFactory;
    }

    final Collection<String> mergeProperties(final Collection<String> additionalProperties) {
        final Set<String> newProperties = new HashSet<>();

        newProperties.addAll(properties);
        newProperties.addAll(additionalProperties);

        return Collections.unmodifiableCollection(newProperties);
    }

    final Function<Object, R> getConversionFunction(final Class<S> inputType, final Class<R> targetType) {
        if (targetType.isAssignableFrom(inputType)) {
            return (Function<Object, R>) Function.identity();
        } else {
            return targetType.isInterface() ? o -> projectionFactory.createProjection(targetType, o) : o -> DefaultConversionService.getSharedInstance().convert(o, targetType);
        }
    }

    interface ScrollQueryFactory {
        Query createQuery(Sort sort, ScrollPosition scrollPosition);
    }
}
