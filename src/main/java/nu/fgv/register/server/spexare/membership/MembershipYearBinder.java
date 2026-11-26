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

package nu.fgv.register.server.spexare.membership;

import nu.fgv.register.server.util.search.StringTypeBinder;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;

import java.time.Year;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * Indexes a membership's association and year, for the {@code memberships} facet. A life membership counts in its first
 * year and every year after it, up to this year (the search index is rebuilt every few hours, so the new year follows).
 */
public class MembershipYearBinder extends StringTypeBinder<Membership> {
    public MembershipYearBinder() {
        super(Membership.class, Membership::getType, "year", true, MembershipYearBinder::years);
    }

    @Override
    protected void extraDependencies(final TypeBindingContext context) {
        context.dependencies().use("lifetime");
    }

    static Collection<String> years(final Membership membership) {
        final int first = Integer.parseInt(membership.getYear());

        if (!membership.isLifetime() || first >= Year.now().getValue()) {
            return java.util.List.of(membership.getYear());
        }
        return IntStream.rangeClosed(first, Year.now().getValue()).mapToObj(Integer::toString).toList();
    }
}
