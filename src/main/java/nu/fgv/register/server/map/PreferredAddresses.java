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

package nu.fgv.register.server.map;

import nu.fgv.register.server.map.Geocoder.AddressQuery;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Chooses the one address each spexare is placed at on the map: the home address when there is one, then another, then
 * the work address. Addresses without a postal code or city are never chosen.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
public final class PreferredAddresses {

    private static final List<String> TYPE_PREFERENCE = List.of("HOME", "OTHER", "WORK");

    private PreferredAddresses() {
    }

    /**
     * @param candidates the addresses of the spexare to place
     * @return the chosen address per spexare id, leaving out spexare with no usable address
     */
    public static Map<Long, AddressQuery> choose(final Collection<Candidate> candidates) {
        final Map<Long, Candidate> chosen = new HashMap<>();

        for (final Candidate candidate : candidates) {
            if (!StringUtils.hasText(candidate.address().postalCode()) && !StringUtils.hasText(candidate.address().city())) {
                continue;
            }
            chosen.merge(candidate.spexareId(), candidate, (current, next) ->
                    rank(next.type()) < rank(current.type()) ? next : current);
        }

        return chosen.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().address()));
    }

    private static int rank(final @Nullable String type) {
        final int index = type == null ? -1 : TYPE_PREFERENCE.indexOf(type);

        return index < 0 ? TYPE_PREFERENCE.size() : index;
    }

    /**
     * One address of a spexare.
     *
     * @param spexareId the spexare id
     * @param type      the address type, such as HOME, OTHER or WORK
     * @param address   the address
     */
    public record Candidate(long spexareId, @Nullable String type, AddressQuery address) {
    }
}
