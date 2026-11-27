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

import org.junit.jupiter.api.Test;

import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;

class MembershipYearBinderTest {

    private static Membership membership(final String year, final boolean lifetime) {
        final Membership membership = new Membership();

        membership.setYear(year);
        membership.setLifetime(lifetime);
        return membership;
    }

    @Test
    void aYearlyMembershipCountsInItsYear() {
        assertThat(MembershipYearBinder.years(membership("2020", false))).containsExactly("2020");
    }

    @Test
    void aLifeMembershipCountsInEveryYearFromItsFirst() {
        final int now = Year.now().getValue();

        assertThat(MembershipYearBinder.years(membership(Integer.toString(now - 2), true)))
                .containsExactly(Integer.toString(now - 2), Integer.toString(now - 1), Integer.toString(now));
    }

    @Test
    void aLifeMembershipFromThisYearOrLaterCountsOnceInThatYear() {
        final String later = Integer.toString(Year.now().getValue() + 1);

        assertThat(MembershipYearBinder.years(membership(later, true))).containsExactly(later);
    }
}
