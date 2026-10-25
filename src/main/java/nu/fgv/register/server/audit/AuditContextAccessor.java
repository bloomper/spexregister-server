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

import io.micrometer.context.ThreadLocalAccessor;
import org.jspecify.annotations.Nullable;

/**
 * Carries the {@link AuditContext} into the threads that run GraphQL data fetchers and subscriptions, through
 * Micrometer's context propagation.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
public class AuditContextAccessor implements ThreadLocalAccessor<AuditContext.Origin> {

    static final String KEY = AuditContextAccessor.class.getName();

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public AuditContext.@Nullable Origin getValue() {
        return AuditContext.current();
    }

    @Override
    public void setValue(final AuditContext.Origin origin) {
        AuditContext.set(origin);
    }

    @Override
    public void setValue() {
        AuditContext.clear();
    }

}
