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

import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.security.policy.Permission;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.Spexare_;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The spexare on the map.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class MapService {

    private final SpexareRepository spexareRepository;
    private final SpexareLocationRepository locationRepository;

    /**
     * @return every living spexare the caller may read who has been located
     */
    @RequiresAdminOrEditorOrUser
    public List<SpexareLocationDto> findLocated() {
        final List<Spexare> spexare = spexareRepository.findAll(located(), Permission.READ);

        if (spexare.isEmpty()) {
            return List.of();
        }

        final Map<Long, SpexareLocation> locations = locationRepository
                .findBySpexareIdInAndLatitudeIsNotNullAndLongitudeIsNotNull(spexare.stream().map(Spexare::getId).toList())
                .stream()
                .collect(Collectors.toMap(SpexareLocation::getSpexareId, Function.identity()));

        return spexare.stream()
                .filter(s -> locations.containsKey(s.getId()))
                .map(s -> toDto(s, locations.get(s.getId())))
                .toList();
    }

    private static SpexareLocationDto toDto(final Spexare spexare, final SpexareLocation location) {
        return new SpexareLocationDto(spexare.getUuid(), spexare.getFirstName(), spexare.getLastName(),
                spexare.getNickName(), Objects.requireNonNull(location.getLatitude()), Objects.requireNonNull(location.getLongitude()));
    }

    private static Specification<Spexare> located() {
        return (root, query, cb) -> {
            final Subquery<Long> locatedIds = Objects.requireNonNull(query).subquery(Long.class);
            final var location = locatedIds.from(SpexareLocation.class);

            locatedIds.select(location.get("spexareId"))
                    .where(cb.isNotNull(location.get("latitude")), cb.isNotNull(location.get("longitude")));

            return cb.and(
                    root.get(Spexare_.id).in(locatedIds),
                    cb.or(cb.isNull(root.get(Spexare_.deceased)), cb.isFalse(root.get(Spexare_.deceased)))
            );
        };
    }
}
