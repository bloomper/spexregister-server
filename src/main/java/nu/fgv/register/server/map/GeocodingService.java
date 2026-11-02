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

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.map.Geocoder.AddressQuery;
import nu.fgv.register.server.map.Geocoder.Coordinates;
import nu.fgv.register.server.util.job.MaintenanceJob;
import nu.fgv.register.server.util.job.MaintenanceJobTracker;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static nu.fgv.register.server.util.security.SecurityUtil.runAsSystem;

/**
 * Keeps the locations of spexare in step with their addresses. Each spexare is placed at one address, the home address
 * when there is one, then another, then the work address; deceased spexare and addresses without a postal code or city
 * are left out. A run looks up only the spexare whose chosen address changed since it was last looked up, at most
 * {@code spexregister.geocoding.batch-size} of them, one lookup per {@code spexregister.geocoding.delay}. Runs on the
 * {@code spexregister.jobs.geocode-addresses.cron-expression} schedule when {@code spexregister.geocoding.enabled}.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Service
public class GeocodingService {

    private final SpexareLocationRepository repository;
    private final EntityManager entityManager;
    private final Geocoder geocoder;
    private final MaintenanceJobTracker tracker;
    private final TransactionTemplate transactions;
    private final boolean enabled;
    private final int batchSize;
    private final Duration delay;

    public GeocodingService(final SpexareLocationRepository repository,
                            final EntityManager entityManager,
                            final Geocoder geocoder,
                            final MaintenanceJobTracker tracker,
                            final PlatformTransactionManager transactionManager,
                            @Value("${spexregister.geocoding.enabled:false}") final boolean enabled,
                            @Value("${spexregister.geocoding.batch-size:300}") final int batchSize,
                            @Value("${spexregister.geocoding.delay:PT1.1S}") final Duration delay) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.geocoder = geocoder;
        this.tracker = tracker;
        this.transactions = new TransactionTemplate(transactionManager);
        this.enabled = enabled;
        this.batchSize = batchSize;
        this.delay = delay;
    }

    /**
     * Geocodes the changed addresses on the {@code spexregister.jobs.geocode-addresses.cron-expression} schedule.
     */
    @Scheduled(cron = "${spexregister.jobs.geocode-addresses.cron-expression}")
    public void scheduledRun() {
        runAsSystem(() -> tracker.run(MaintenanceJob.ADDRESS_GEOCODING, this::geocode));
    }

    /**
     * Removes the locations of spexare who no longer have an address to place them at, then looks up the spexare whose
     * address is new or changed.
     *
     * @return how many spexare were looked up
     */
    int geocode() {
        if (!enabled) {
            log.info("Geocoding is disabled, not looking up any addresses");
            return 0;
        }

        log.info("Starting address geocoding job");
        final Map<Long, AddressQuery> wanted = transactions.execute(_ -> chosenAddresses());
        final Map<Long, String> known = transactions.execute(_ -> repository.findAll().stream()
                .collect(Collectors.toMap(SpexareLocation::getSpexareId, SpexareLocation::getQuery)));
        final int removed = removeStale(wanted, known);
        final List<Map.Entry<Long, AddressQuery>> changed = wanted.entrySet().stream()
                .filter(entry -> !entry.getValue().key().equals(known.get(entry.getKey())))
                .toList();

        int located = 0;
        int lookedUp = 0;

        for (final Map.Entry<Long, AddressQuery> entry : changed.subList(0, Math.min(batchSize, changed.size()))) {
            if (lookedUp > 0 && !pause()) {
                break;
            }
            final Optional<Coordinates> coordinates;
            try {
                coordinates = lookUp(entry.getValue());
            } catch (final Geocoder.GeocodingException e) {
                log.warn("Stopping address geocoding job after {} lookups: {}", lookedUp, e.getMessage());
                break;
            }
            transactions.executeWithoutResult(_ -> save(entry.getKey(), entry.getValue(), coordinates.orElse(null)));
            lookedUp++;
            if (coordinates.isPresent()) {
                located++;
            }
        }

        log.info("Finished address geocoding job (looked up: {}, located: {}, removed: {}, remaining: {})",
                lookedUp, located, removed, changed.size() - lookedUp);
        return lookedUp;
    }

    private Optional<Coordinates> lookUp(final AddressQuery address) {
        final Optional<Coordinates> exact = geocoder.locate(address);

        if (exact.isPresent() || !StringUtils.hasText(address.streetAddress())) {
            return exact;
        }
        return pause() ? geocoder.locate(address.withoutStreet()) : Optional.empty();
    }

    private boolean pause() {
        if (delay.isZero() || delay.isNegative()) {
            return true;
        }
        try {
            Thread.sleep(delay);
            return true;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private int removeStale(final Map<Long, AddressQuery> wanted, final Map<Long, String> known) {
        final List<Long> stale = known.keySet().stream().filter(id -> !wanted.containsKey(id)).toList();

        if (!stale.isEmpty()) {
            transactions.executeWithoutResult(_ -> repository.deleteAllByIdInBatch(stale));
        }
        return stale.size();
    }

    private void save(final Long spexareId, final AddressQuery address, final @Nullable Coordinates coordinates) {
        final SpexareLocation location = repository.findById(spexareId).orElseGet(() -> new SpexareLocation(spexareId));

        location.setQuery(address.key());
        location.setLatitude(coordinates == null ? null : coordinates.latitude());
        location.setLongitude(coordinates == null ? null : coordinates.longitude());
        location.setGeocodedAt(Instant.now());
        repository.save(location);
    }

    private Map<Long, AddressQuery> chosenAddresses() {
        return PreferredAddresses.choose(entityManager.createQuery("""
                        select a.spexare.id as spexareId, a.type.id as type, a.streetAddress as street,
                               a.postalCode as postalCode, a.city as city, a.country as country
                        from Address a
                        where a.spexare is not null and (a.spexare.deceased is null or a.spexare.deceased = false)
                        """, Tuple.class)
                .getResultStream()
                .map(row -> new PreferredAddresses.Candidate(row.get("spexareId", Long.class), row.get("type", String.class),
                        new AddressQuery(row.get("street", String.class), row.get("postalCode", String.class),
                                row.get("city", String.class), row.get("country", String.class))))
                .toList());
    }
}
