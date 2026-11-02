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
import nu.fgv.register.server.map.Geocoder.AddressQuery;
import nu.fgv.register.server.map.Geocoder.Coordinates;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import nu.fgv.register.server.util.job.MaintenanceJob;
import nu.fgv.register.server.util.job.MaintenanceJobTracker;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class GeocodingServiceIntegrationTest extends AbstractIntegrationTest {

    private static final Coordinates GOTHENBURG = new Coordinates(57.69, 11.97);

    private final SpexareLocationRepository repository;
    private final EntityManager entityManager;
    private final PlatformTransactionManager transactionManager;
    private final MaintenanceJobTracker tracker = new MaintenanceJobTracker();
    private final List<AddressQuery> lookups = new ArrayList<>();

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public GeocodingServiceIntegrationTest(final JdbcClient jdbcClient,
                                           final Keycloak keycloakAdminClient,
                                           final String keycloakClientId,
                                           final ObjectMapper objectMapper,
                                           final SpexareLocationRepository repository,
                                           final EntityManager entityManager,
                                           final PlatformTransactionManager transactionManager) {
        super(jdbcClient, keycloakAdminClient, keycloakClientId, objectMapper);
        this.repository = repository;
        this.entityManager = entityManager;
        this.transactionManager = transactionManager;
    }

    @BeforeEach
    void setUp() {
        JdbcTestUtils.deleteFromTables(jdbcClient, "spexare_location", "address", "spexare");
    }

    private GeocodingService service(final boolean enabled, final int batchSize, final Function<AddressQuery, Optional<Coordinates>> answer) {
        final Geocoder geocoder = address -> {
            lookups.add(address);
            return answer.apply(address);
        };

        return new GeocodingService(repository, entityManager, geocoder, tracker, transactionManager, enabled, batchSize, Duration.ZERO);
    }

    private long spexare(final String firstName, final boolean deceased) {
        final String uuid = UUID.randomUUID().toString();

        jdbcClient.sql("""
                        INSERT INTO spexare (uuid, first_name, last_name, deceased, created_by, created_at)
                        VALUES (?, ?, 'Spexare', ?, 'test', NOW())
                        """)
                .params(uuid, firstName, deceased)
                .update();

        return jdbcClient.sql("SELECT id FROM spexare WHERE uuid = ?").param(uuid).query(Long.class).single();
    }

    private void address(final long spexareId, final String type, final @Nullable String street, final @Nullable String city) {
        jdbcClient.sql("""
                        INSERT INTO address (uuid, street_address, postal_code, city, type_id, spexare_id, created_by, created_at)
                        VALUES (?, ?, NULL, ?, ?, ?, 'test', NOW())
                        """)
                .params(UUID.randomUUID().toString(), street, city, type, spexareId)
                .update();
    }

    @Test
    void should_do_nothing_when_disabled() {
        address(spexare("Ada", false), "HOME", "Kemigården 1", "Göteborg");

        assertThat(service(false, 10, _ -> Optional.of(GOTHENBURG)).geocode()).isZero();
        assertThat(lookups).isEmpty();
        assertThat(repository.count()).isZero();
    }

    @Test
    void should_locate_the_preferred_address_of_each_living_spexare() {
        final long ada = spexare("Ada", false);
        final long bea = spexare("Bea", false);
        final long cid = spexare("Cid", true);
        final long dag = spexare("Dag", false);

        address(ada, "WORK", "Kontoret 1", "Stockholm");
        address(ada, "HOME", "Kemigården 1", "Göteborg");
        address(bea, "WORK", "Kontoret 2", "Malmö");
        address(cid, "HOME", "Gravgatan 1", "Lund");
        address(dag, "HOME", "Gatan 1", null);

        assertThat(service(true, 10, _ -> Optional.of(GOTHENBURG)).geocode()).isEqualTo(2);

        assertThat(lookups).extracting(AddressQuery::city).containsExactlyInAnyOrder("Göteborg", "Malmö");
        assertThat(repository.findById(ada)).hasValueSatisfying(location -> {
            assertThat(location.getLatitude()).isEqualTo(GOTHENBURG.latitude());
            assertThat(location.getLongitude()).isEqualTo(GOTHENBURG.longitude());
            assertThat(location.getQuery()).isEqualTo(new AddressQuery("Kemigården 1", null, "Göteborg", null).key());
        });
        assertThat(repository.findById(cid)).isEmpty();
        assertThat(repository.findById(dag)).isEmpty();
    }

    @Test
    void should_look_up_only_changed_addresses() {
        final long ada = spexare("Ada", false);

        address(ada, "HOME", "Kemigården 1", "Göteborg");
        service(true, 10, _ -> Optional.of(GOTHENBURG)).geocode();
        lookups.clear();

        assertThat(service(true, 10, _ -> Optional.of(GOTHENBURG)).geocode()).isZero();

        jdbcClient.sql("UPDATE address SET street_address = 'Kemigården 2' WHERE spexare_id = ?").param(ada).update();

        assertThat(service(true, 10, _ -> Optional.of(GOTHENBURG)).geocode()).isEqualTo(1);
        assertThat(lookups).extracting(AddressQuery::streetAddress).containsExactly("Kemigården 2");
    }

    @Test
    void should_fall_back_to_the_city_and_remember_what_was_not_found() {
        final long ada = spexare("Ada", false);
        final long bea = spexare("Bea", false);

        address(ada, "HOME", "Okänd väg 1", "Göteborg");
        address(bea, "HOME", "Okänd väg 2", "Ingenstans");

        service(true, 10, address -> address.streetAddress() == null && "Göteborg".equals(address.city())
                ? Optional.of(GOTHENBURG)
                : Optional.empty()).geocode();

        assertThat(lookups).hasSize(4);
        assertThat(repository.findById(ada)).hasValueSatisfying(location -> assertThat(location.getLatitude()).isEqualTo(GOTHENBURG.latitude()));
        assertThat(repository.findById(bea)).hasValueSatisfying(location -> assertThat(location.getLatitude()).isNull());

        lookups.clear();
        service(true, 10, _ -> Optional.of(GOTHENBURG)).geocode();

        assertThat(lookups).isEmpty();
    }

    @Test
    void should_stop_at_the_batch_size_and_when_the_service_fails() {
        for (final String name : List.of("Ada", "Bea", "Cid")) {
            address(spexare(name, false), "HOME", null, "Göteborg");
        }

        assertThat(service(true, 2, _ -> Optional.of(GOTHENBURG)).geocode()).isEqualTo(2);
        assertThat(service(true, 2, _ -> {
            throw new Geocoder.GeocodingException("down", new RuntimeException());
        }).geocode()).isZero();
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void should_remove_the_location_of_a_spexare_without_an_address() {
        final long ada = spexare("Ada", false);

        address(ada, "HOME", null, "Göteborg");
        service(true, 10, _ -> Optional.of(GOTHENBURG)).geocode();

        jdbcClient.sql("DELETE FROM address WHERE spexare_id = ?").param(ada).update();
        service(true, 10, _ -> Optional.of(GOTHENBURG)).geocode();

        assertThat(repository.findById(ada)).isEmpty();
    }

    @Test
    void should_track_a_scheduled_run() {
        service(false, 10, _ -> Optional.empty()).scheduledRun();

        assertThat(tracker.lastRun(MaintenanceJob.ADDRESS_GEOCODING)).hasValueSatisfying(run -> assertThat(run.succeeded()).isTrue());
    }
}
