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

package nu.fgv.register.server.util.data;

import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import net.datafaker.providers.base.IdNumber;
import nu.fgv.register.server.map.Geocoder;
import nu.fgv.register.server.map.PreferredAddresses;
import nu.fgv.register.server.news.NewsMapper;
import nu.fgv.register.server.security.policy.PermissionService;
import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.spex.category.SpexCategory;
import nu.fgv.register.server.task.category.TaskCategory;
import nu.fgv.register.server.user.authority.AuthorityService;
import nu.fgv.register.server.util.security.CryptoConverter;
import org.apache.commons.lang3.tuple.Pair;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Generates sample news, tags, spexare with their records and partners, and users, also in Keycloak.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Component
public class SampleDataImporter {
    private static final int NUMBER_OF_SAMPLES_NEWS = 20;
    private static final int NUMBER_OF_SAMPLES_TAGS = 5;
    private static final int NUMBER_OF_SAMPLES_SPEXARE = 500;
    private static final int NUMBER_OF_SAMPLES_SPEXARE_WITH_PARTNER = NUMBER_OF_SAMPLES_SPEXARE / 10;
    private static final int NUMBER_OF_SAMPLES_SPEXARE_MAX_MEMBERSHIPS_OF_EACH_TYPE = 3;
    private static final int NUMBER_OF_SAMPLES_SPEXARE_MAX_ACTIVITIES = 5;
    private static final int NUMBER_OF_SAMPLES_SPEXARE_MAX_TASK_ACTIVITIES_PER_ACTIVITY = 3;
    private static final int NUMBER_OF_SAMPLES_USERS = 50;
    private static final int SPEXARE_MIN_AGE = 18;
    private static final int SPEXARE_MAX_AGE = 105;
    private static final String SYSTEM_USER = "system";
    private static final List<SamplePlace> SAMPLE_PLACES = List.of(
            new SamplePlace(57.7089, 11.9746, 0.06, 60),
            new SamplePlace(59.3293, 18.0686, 0.05, 12),
            new SamplePlace(55.6050, 13.0038, 0.03, 5),
            new SamplePlace(57.6348, 18.2948, 0.02, 1),
            new SamplePlace(56.6745, 12.8568, 0.02, 3),
            new SamplePlace(57.7210, 12.9401, 0.02, 3),
            new SamplePlace(58.2837, 12.2886, 0.02, 2),
            new SamplePlace(58.3912, 13.8451, 0.02, 2),
            new SamplePlace(59.8586, 17.6389, 0.02, 2),
            new SamplePlace(58.4108, 15.6214, 0.02, 2),
            new SamplePlace(57.7826, 14.1618, 0.02, 2),
            new SamplePlace(63.8258, 20.2630, 0.02, 1),
            new SamplePlace(65.5848, 22.1547, 0.02, 1),
            new SamplePlace(59.9139, 10.7522, 0.03, 2),
            new SamplePlace(55.6761, 12.5683, 0.03, 2)
    );
    protected static final Authentication AUTH = new TestingAuthenticationToken(SYSTEM_USER, "ignored", "ROLE_ADMIN");
    private static final String SAMPLE_USER_DOMAIN = "@fgv.nu";
    private final PermissionService permissionService;
    private final AuthorityService authorityService;
    private final AuditDataSeeder auditDataSeeder;
    private final String sampleUserPassword;
    private final Keycloak keycloakAdminClient;
    private final String keycloakClientId;
    private final Random rnd = new SecureRandom();
    private final AtomicInteger failedImageDownloads = new AtomicInteger();
    private final Faker faker = new Faker(Locale.of("sv", "SE"));
    private final CustomSwedenIdNumber customSwedenIdNumber = new CustomSwedenIdNumber();
    private final CryptoConverter cryptoConverter;
    @Value("${spexregister.keycloak.realm}")
    private String keycloakRealm;

    public SampleDataImporter(final PermissionService permissionService,
                              final AuthorityService authorityService,
                              final AuditDataSeeder auditDataSeeder,
                              final Keycloak keycloakAdminClient,
                              final String keycloakClientId,
                              @Value("${spexregister.data.sample-user-password}") final String sampleUserPassword,
                              @Value("${spexregister.crypto.secret-key}") final String secretKey) {
        this.permissionService = permissionService;
        this.authorityService = authorityService;
        this.auditDataSeeder = auditDataSeeder;
        this.keycloakAdminClient = keycloakAdminClient;
        this.keycloakClientId = keycloakClientId;
        this.sampleUserPassword = sampleUserPassword;
        cryptoConverter = new CryptoConverter(secretKey);
    }

    private static long insertImage(final JdbcClient jdbcClient, final byte[] data, final String contentType) {
        final KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient
                .sql("INSERT INTO image (data, content_type) VALUES (:data, :contentType)")
                .param("data", data)
                .param("contentType", contentType)
                .update(keyHolder);

        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    /**
     * @param connection the migrated database
     */
    public void importSampleData(final Connection connection) {
        final JdbcClient jdbcClient = JdbcClient.create(new SingleConnectionDataSource(connection, true));

        final long started = System.nanoTime();

        log.info("Importing sample data");
        timed("Emptied the tables", () -> purgeAllRelevantTables(jdbcClient));
        timed("Deleted the users in Keycloak", this::purgeKeycloak);
        SecurityContextHolder.getContext().setAuthentication(AUTH);

        timed("Created the task categories and tasks", () -> createSampleTaskCategoriesAndTasks(connection, jdbcClient));
        timed("Created the spex categories and spex", () -> createSampleSpexCategoriesAndSpex(connection, jdbcClient));
        timed("Created the news", () -> createSampleNews(jdbcClient));
        timed("Created the tags", () -> createSampleTags(jdbcClient));
        failedImageDownloads.set(0);
        timed("Created the spexare", () -> createSampleSpexare(jdbcClient));
        timed("Placed the spexare on the map", () -> createSampleLocations(jdbcClient));
        if (failedImageDownloads.get() > 0) {
            log.warn("{} sample spexare images could not be downloaded and were generated instead", failedImageDownloads.get());
        }
        final List<String> users = NotificationSuspender.whileSuspended(keycloakAdminClient.realm(keycloakRealm), () -> {
            timed("Created the fixed users in Keycloak", this::createFixedSampleUsers);
            return timedResult("Created the random users in Keycloak", () -> createSampleUsers(jdbcClient));
        });

        timed("Seeded the sample history", () -> auditDataSeeder.seedSampleHistory(jdbcClient, users));

        SecurityContextHolder.clearContext();
        log.info("Imported sample data in {} s", Duration.ofNanos(System.nanoTime() - started).toSeconds());
    }

    private void timed(final String step, final Runnable action) {
        timedResult(step, () -> {
            action.run();
            return null;
        });
    }

    private <T> T timedResult(final String step, final Supplier<T> action) {
        final long started = System.nanoTime();
        final T result = action.get();

        log.info("{} in {} ms", step, Duration.ofNanos(System.nanoTime() - started).toMillis());
        return result;
    }

    private void purgeAllRelevantTables(final JdbcClient jdbcClient) {
        jdbcClient.sql("SELECT id FROM spexare WHERE partner_id IS NOT NULL")
                .query()
                .listOfRows()
                .forEach(row ->
                        jdbcClient
                                .sql("UPDATE spexare SET partner_id = NULL WHERE id = :id")
                                .param("id", row.get("id"))
                                .update()
                );
        jdbcClient.sql("SELECT id FROM spex WHERE parent_id IS NOT NULL")
                .query()
                .listOfRows()
                .forEach(row ->
                        jdbcClient
                                .sql("UPDATE spex SET parent_id = NULL WHERE id = :id")
                                .param("id", row.get("id"))
                                .update()
                );
        jdbcClient.sql("SELECT id FROM user WHERE spexare_id IS NOT NULL")
                .query()
                .listOfRows()
                .forEach(row ->
                        jdbcClient
                                .sql("UPDATE user SET spexare_id = NULL WHERE id = :id")
                                .param("id", row.get("id"))
                                .update()
                );

        final List<String> tables = List.of(
                "permission_grant",
                "actor",
                "task_activity",
                "spex_activity",
                "activity",
                "address",
                "consent",
                "membership",
                "tagging",
                "toggle",
                "spexare",
                "task",
                "task_category",
                "spex",
                "spex_details",
                "spex_category",
                "image",
                "news",
                "tag",
                "user"
        );

        tables.forEach(table ->
                jdbcClient
                        .sql("DELETE FROM %s".formatted(table))
                        .update()
        );
    }

    private void purgeKeycloak() {
        final List<UserRepresentation> users = keycloakAdminClient
                .realm(keycloakRealm)
                .users()
                .list();

        log.info("Deleting {} users in Keycloak", users.size());
        users.forEach(representation -> {
            try (final Response response = keycloakAdminClient
                    .realm(keycloakRealm)
                    .users()
                    .delete(representation.getId())) {
                if (response.getStatus() != HttpStatus.NO_CONTENT.value()) {
                    throw new IllegalStateException("Unable to delete user " + representation.getEmail());
                }
            }
        });
    }

    private void createSampleTaskCategoriesAndTasks(final Connection connection, final JdbcClient jdbcClient) {
        ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/sampledata/tasks.sql"));

        final String taskSql = "UPDATE task SET created_at = :createdAt WHERE id = :id";

        jdbcClient.sql("SELECT id FROM task")
                .query()
                .listOfRows()
                .forEach(row -> {
                    final Long id = (Long) Objects.requireNonNull(row.get("id"));

                    jdbcClient.sql(taskSql)
                            .param("createdAt", randomizeCreatedAt())
                            .param("id", id)
                            .update();
                });
    }

    private void createSampleSpexCategoriesAndSpex(final Connection connection, final JdbcClient jdbcClient) {
        ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/sampledata/spex.sql"));

        final String categorySql = """
                UPDATE spex_category
                SET
                    logo_id = :imageId
                WHERE
                    id = :id
                """;

        jdbcClient.sql("SELECT id FROM spex_category")
                .query()
                .listOfRows()
                .forEach(row -> {
                    final Long id = (Long) Objects.requireNonNull(row.get("id"));

                    jdbcClient
                            .sql(categorySql)
                            .param("imageId", insertImage(jdbcClient, imageToByteArray(faker.image().base64SVG()), "image/svg+xml"))
                            .param("id", id)
                            .update();
                });

        final String detailsSql = """
                UPDATE spex_details
                SET
                    poster_id = :imageId
                WHERE
                    id = :id
                """;

        jdbcClient.sql("SELECT id FROM spex_details")
                .query()
                .listOfRows()
                .forEach(row ->
                        jdbcClient
                                .sql(detailsSql)
                                .param("imageId", insertImage(jdbcClient, imageToByteArray(faker.image().base64SVG()), "image/svg+xml"))
                                .param("id", row.get("id"))
                                .update()
                );

        final String spexSql = "UPDATE spex SET created_at = :createdAt WHERE id = :id";

        jdbcClient.sql("SELECT id FROM spex")
                .query()
                .listOfRows()
                .forEach(row -> {
                    final Long id = (Long) Objects.requireNonNull(row.get("id"));

                    jdbcClient.sql(spexSql)
                            .param("createdAt", randomizeCreatedAt())
                            .param("id", id)
                            .update();
                });
    }

    private void createSampleNews(final JdbcClient jdbcClient) {
        final String sql = """
                INSERT INTO news
                    (uuid, visible_from, visible_to, subject, text, published, created_by, created_at, version)
                VALUES
                    (UUID(), :visibleFrom, :visibleTo, :subject, :text, :published, :createdBy, :createdAt, 0)
                """;

        IntStream.range(0, NUMBER_OF_SAMPLES_NEWS).forEach(i -> {
            final Instant visibleFrom = faker.timeAndDate().past(10, TimeUnit.DAYS, Instant.now().plus(2, ChronoUnit.DAYS));
            final Instant visibleTo = faker.timeAndDate().future(20, TimeUnit.DAYS, visibleFrom);

            jdbcClient
                    .sql(sql)
                    .param("visibleFrom", visibleFrom)
                    .param("visibleTo", visibleTo)
                    .param("subject", faker.lorem().maxLengthSentence(255))
                    .param("text", faker.lorem().paragraphs(5).stream().collect(Collectors.joining(System.lineSeparator())))
                    .param("published", NewsMapper.NEWS_MAPPER.isPublished(LocalDate.ofInstant(visibleFrom, ZoneId.systemDefault()), LocalDate.ofInstant(visibleTo, ZoneId.systemDefault())))
                    .param("createdBy", SYSTEM_USER)
                    .param("createdAt", LocalDateTime.now().atZone(ZoneId.of("UTC")))
                    .update();
        });
    }

    private void createSampleTags(final JdbcClient jdbcClient) {
        final String sql = """
                INSERT INTO tag
                    (uuid, name, created_by, created_at, version)
                VALUES
                    (UUID(), :name, :createdBy, :createdAt, 0)
                """;

        IntStream.range(0, NUMBER_OF_SAMPLES_TAGS).forEach(_ -> jdbcClient
                .sql(sql)
                .param("name", faker.lorem().word())
                .param("createdBy", SYSTEM_USER)
                .param("createdAt", LocalDateTime.now().atZone(ZoneId.of("UTC")))
                .update());
    }

    private void createSampleSpexare(final JdbcClient jdbcClient) {
        final List<Long> spexareIds = new ArrayList<>();
        final List<TaskCategory> taskCategories = getTaskCategories(jdbcClient);
        final Map<Long, List<Long>> tasksPerTaskCategory = getTasksPerTaskCategory(jdbcClient, taskCategories);
        final List<SpexCategory> spexCategories = getSpexCategories(jdbcClient);
        final Pair<Map<Long, List<Long>>, Map<Long, List<Long>>> spexAndRevivalsPerSpexCategory = getSpexPerSpexCategory(jdbcClient, spexCategories);
        final Map<Long, List<Long>> spexPerSpexCategory = spexAndRevivalsPerSpexCategory.getLeft();
        final Map<Long, List<Long>> revivalsPerSpexCategory = spexAndRevivalsPerSpexCategory.getRight();
        final List<Type> vocals = getVocals(jdbcClient);
        final String sql = """
                INSERT INTO spexare
                    (uuid, first_name, last_name, nick_name, social_security_number, deceased, published, graduation, comment, created_by, created_at, version)
                VALUES
                    (UUID(), :firstName, :lastName, :nickName, :socialSecurityNumber, :deceased, :published, :graduation, :comment, :createdBy, :createdAt, 0)
                """;

        IntStream.range(0, NUMBER_OF_SAMPLES_SPEXARE).forEach(i -> {
            final KeyHolder keyHolder = new GeneratedKeyHolder();
            final boolean published = rnd.nextInt(4) != 0;

            jdbcClient
                    .sql(sql)
                    .param("firstName", faker.name().firstName())
                    .param("lastName", faker.name().lastName())
                    .param("nickName", rnd.nextBoolean() ?
                            faker.collection(
                                            () -> faker.starTrek().character(),
                                            () -> faker.starWars().character(),
                                            () -> faker.doctorWho().character()
                                    )
                                    .maxLen(1)
                                    .generate() :
                            null)
                    .param("socialSecurityNumber", rnd.nextBoolean() ?
                            cryptoConverter.convertToDatabaseColumn(customSwedenIdNumber.generateValid(faker, new IdNumber.IdNumberRequest(SPEXARE_MIN_AGE, SPEXARE_MAX_AGE, IdNumber.GenderRequest.ANY)).idNumber()) :
                            null)
                    .param("deceased", rnd.nextInt(4) == 0)
                    .param("published", published)
                    .param("graduation", rnd.nextBoolean() ? faker.regexify("[ABDEGKMIVT]\\d{2}") : null)
                    .param("comment", rnd.nextBoolean() ? faker.lorem().paragraph() : null)
                    .param("createdBy", SYSTEM_USER)
                    .param("createdAt", randomizeCreatedAt())
                    .update(keyHolder);

            if (keyHolder.getKey() != null) {
                final long spexareId = keyHolder.getKey().longValue();

                if (rnd.nextBoolean()) {
                    createSpexareImage(jdbcClient, spexareId);
                }
                createSpexareAddresses(jdbcClient, spexareId);
                createSpexareConsents(jdbcClient, spexareId);
                createSpexareMemberships(jdbcClient, spexareId);
                createSpexareToggles(jdbcClient, spexareId);
                createSpexareTaggings(jdbcClient, spexareId);
                createSpexareActivities(jdbcClient, spexareId, taskCategories, tasksPerTaskCategory, spexCategories, spexPerSpexCategory, revivalsPerSpexCategory, vocals);
                spexareIds.add(spexareId);
            }
        });

        final String partnerSql = """
                UPDATE spexare
                SET
                    partner_id = :partnerId
                WHERE
                    id = :spexareId
                """;

        Collections.shuffle(spexareIds);
        IntStream.range(0, NUMBER_OF_SAMPLES_SPEXARE_WITH_PARTNER / 2).forEach(i -> {
            jdbcClient
                    .sql(partnerSql)
                    .param("partnerId", spexareIds.get(i))
                    .param("spexareId", spexareIds.get(i + (NUMBER_OF_SAMPLES_SPEXARE_WITH_PARTNER / 2)))
                    .update();
            jdbcClient
                    .sql(partnerSql)
                    .param("partnerId", spexareIds.get(i + (NUMBER_OF_SAMPLES_SPEXARE_WITH_PARTNER / 2)))
                    .param("spexareId", spexareIds.get(i))
                    .update();
        });
    }

    /**
     * Places each living spexare with an address near one of {@link #SAMPLE_PLACES}, as the geocoding job would, since
     * the sample addresses are made up and would not be found. A few are left as not found, as real addresses sometimes
     * are. Each location is stored under the key of its chosen address, so the geocoding job leaves it alone.
     */
    private void createSampleLocations(final JdbcClient jdbcClient) {
        final List<PreferredAddresses.Candidate> candidates = jdbcClient.sql("""
                        SELECT a.spexare_id, a.type_id, a.street_address, a.postal_code, a.city, a.country
                        FROM address a
                        JOIN spexare s ON s.id = a.spexare_id
                        WHERE s.deceased IS NULL OR s.deceased = FALSE
                        """)
                .query((resultSet, rowNum) -> new PreferredAddresses.Candidate(
                        resultSet.getLong("spexare_id"),
                        resultSet.getString("type_id"),
                        new Geocoder.AddressQuery(
                                resultSet.getString("street_address"),
                                resultSet.getString("postal_code"),
                                resultSet.getString("city"),
                                resultSet.getString("country"))))
                .list();
        final String sql = """
                INSERT INTO spexare_location
                    (spexare_id, query, latitude, longitude, geocoded_at)
                VALUES
                    (:spexareId, :query, :latitude, :longitude, :geocodedAt)
                """;
        final double totalWeight = SAMPLE_PLACES.stream().mapToDouble(SamplePlace::weight).sum();

        PreferredAddresses.choose(candidates).forEach((spexareId, address) -> {
            final boolean found = rnd.nextInt(20) != 0;
            final SamplePlace place = pickSamplePlace(totalWeight);

            jdbcClient
                    .sql(sql)
                    .param("spexareId", spexareId)
                    .param("query", address.key())
                    .param("latitude", found ? place.latitude() + rnd.nextGaussian() * place.spread() : null)
                    .param("longitude", found ? place.longitude() + rnd.nextGaussian() * place.spread() * 2 : null)
                    .param("geocodedAt", LocalDateTime.now().atZone(ZoneId.of("UTC")))
                    .update();
        });
    }

    private SamplePlace pickSamplePlace(final double totalWeight) {
        double remaining = rnd.nextDouble() * totalWeight;

        for (final SamplePlace place : SAMPLE_PLACES) {
            remaining -= place.weight();
            if (remaining < 0) {
                return place;
            }
        }
        return SAMPLE_PLACES.getLast();
    }

    private void createSpexareImage(final JdbcClient jdbcClient, final long spexareId) {
        final String sql = """
                UPDATE spexare
                SET
                    image_id = :imageId
                WHERE
                    id = :id
                """;

        jdbcClient
                .sql(sql)
                .param("imageId", insertImage(jdbcClient, imageUrlToByteArray(faker.avatar().image()), "image/png"))
                .param("id", spexareId)
                .update();
    }

    private void createSpexareAddresses(final JdbcClient jdbcClient, final long spexareId) {
        final String sql = """
                INSERT INTO address
                    (uuid, street_address, postal_code, city, country, phone, phone_mobile, email_address, type_id, spexare_id, created_by, created_at, version)
                VALUES
                    (UUID(), :streetAddress, :postalCode, :city, :country, :phone, :phoneMobile, :emailAddress, :typeId, :spexareId, :createdBy, :createdAt, 0)
                """;

        jdbcClient
                .sql("SELECT id FROM type WHERE type = 'ADDRESS'")
                .query(resultSet -> {
                    final String typeId = resultSet.getString("id");

                    if (rnd.nextBoolean()) {
                        jdbcClient
                                .sql(sql)
                                .param("streetAddress", faker.address().streetAddress())
                                .param("postalCode", faker.address().zipCode())
                                .param("city", faker.address().city())
                                .param("country", faker.address().countryCode())
                                .param("phone", rnd.nextBoolean() ? faker.phoneNumber().phoneNumber() : null)
                                .param("phoneMobile", rnd.nextBoolean() ? faker.phoneNumber().cellPhone() : null)
                                .param("emailAddress", rnd.nextBoolean() ? faker.internet().emailAddress() : null)
                                .param("typeId", typeId)
                                .param("spexareId", spexareId)
                                .param("createdBy", SYSTEM_USER)
                                .param("createdAt", LocalDateTime.now().atZone(ZoneId.of("UTC")))
                                .update();
                    }
                });
    }

    private void createSpexareConsents(final JdbcClient jdbcClient, final long spexareId) {
        final String sql = """
                INSERT INTO consent
                    (uuid, value, type_id, spexare_id, created_by, created_at, version)
                VALUES
                    (UUID(), :value, :typeId, :spexareId, :createdBy, :createdAt, 0)
                """;

        jdbcClient
                .sql("SELECT id FROM type WHERE type = 'CONSENT'")
                .query(resultSet -> {
                    final String typeId = resultSet.getString("id");

                    jdbcClient
                            .sql(sql)
                            .param("value", rnd.nextBoolean())
                            .param("typeId", typeId)
                            .param("spexareId", spexareId)
                            .param("createdBy", SYSTEM_USER)
                            .param("createdAt", LocalDateTime.now().atZone(ZoneId.of("UTC")))
                            .update();
                });
    }

    private void createSpexareMemberships(final JdbcClient jdbcClient, final long spexareId) {
        final String sql = """
                INSERT INTO membership
                    (uuid, year, type_id, spexare_id, created_by, created_at, version)
                VALUES
                    (UUID(), :year, :typeId, :spexareId, :createdBy, :createdAt, 0)
                """;

        final Instant startYear = LocalDate.of(1948, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        final Instant endYear = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();

        jdbcClient
                .sql("SELECT id FROM type WHERE type = 'MEMBERSHIP'")
                .query(resultSet -> {
                    final String typeId = resultSet.getString("id");

                    if (rnd.nextBoolean()) {
                        IntStream.range(0, rnd.nextInt(NUMBER_OF_SAMPLES_SPEXARE_MAX_MEMBERSHIPS_OF_EACH_TYPE)).forEach(i ->
                                jdbcClient
                                        .sql(sql)
                                        .param("year", faker.timeAndDate().between(startYear, endYear, "yyyy"))
                                        .param("typeId", typeId)
                                        .param("spexareId", spexareId)
                                        .param("createdBy", SYSTEM_USER)
                                        .param("createdAt", LocalDateTime.now().atZone(ZoneId.of("UTC")))
                                        .update()
                        );
                    }
                });
    }

    private void createSpexareToggles(final JdbcClient jdbcClient, final long spexareId) {
        final String sql = """
                INSERT INTO toggle
                    (uuid, value, type_id, spexare_id, created_by, created_at, version)
                VALUES
                    (UUID(), :value, :typeId, :spexareId, :createdBy, :createdAt, 0)
                """;

        jdbcClient
                .sql("SELECT id FROM type WHERE type = 'TOGGLE'")
                .query(resultSet -> {
                    final String typeId = resultSet.getString("id");

                    jdbcClient
                            .sql(sql)
                            .param("value", rnd.nextBoolean())
                            .param("typeId", typeId)
                            .param("spexareId", spexareId)
                            .param("createdBy", SYSTEM_USER)
                            .param("createdAt", LocalDateTime.now().atZone(ZoneId.of("UTC")))
                            .update();
                });
    }

    private void createSpexareTaggings(final JdbcClient jdbcClient, final long spexareId) {
        final String sql = """
                INSERT INTO tagging
                    (tag_id, spexare_id)
                VALUES
                    (:tagId, :spexareId)
                """;

        jdbcClient
                .sql("SELECT id FROM tag")
                .query(resultSet -> {
                    if (rnd.nextBoolean()) {
                        final String tagId = resultSet.getString("id");

                        jdbcClient
                                .sql(sql)
                                .param("tagId", tagId)
                                .param("spexareId", spexareId)
                                .update();
                    }
                });
    }

    private void createSpexareActivities(final JdbcClient jdbcClient,
                                         final long spexareId,
                                         final List<TaskCategory> taskCategories,
                                         final Map<Long, List<Long>> tasksPerTaskCategory,
                                         final List<SpexCategory> spexCategories,
                                         final Map<Long, List<Long>> spexPerSpexCategory,
                                         final Map<Long, List<Long>> revivalsPerSpexCategory,
                                         final List<Type> vocals) {
        final String activitySql = """
                INSERT INTO activity
                    (uuid, spexare_id, created_by, created_at, version)
                VALUES
                    (UUID(), :spexareId, :createdBy, :createdAt, 0)
                """;
        final String spexActivitySql = """
                INSERT INTO spex_activity
                    (uuid, activity_id, spex_id, created_by, created_at, version)
                VALUES
                    (UUID(), :activityId, :spexId, :createdBy, :createdAt, 0)
                """;
        final String taskActivitySql = """
                INSERT INTO task_activity
                    (uuid, activity_id, task_id, created_by, created_at, version)
                VALUES
                    (UUID(), :activityId, :taskId, :createdBy, :createdAt, 0)
                """;
        final String actorSql = """
                INSERT INTO actor
                    (uuid, task_activity_id, vocal_id, role, created_by, created_at, version)
                VALUES
                    (UUID(), :taskActivityId, :vocalId, :role, :createdBy, :createdAt, 0)
                """;

        IntStream.range(0, rnd.nextInt(NUMBER_OF_SAMPLES_SPEXARE_MAX_ACTIVITIES)).forEach(i -> {
            final KeyHolder activityKeyHolder = new GeneratedKeyHolder();

            jdbcClient
                    .sql(activitySql)
                    .param("spexareId", spexareId)
                    .param("createdBy", SYSTEM_USER)
                    .param("createdAt", LocalDateTime.now().atZone(ZoneId.of("UTC")))
                    .update(activityKeyHolder);

            if (activityKeyHolder.getKey() != null) {
                final long activityId = activityKeyHolder.getKey().longValue();
                final SpexCategory spexCategory = spexCategories.get(rnd.nextInt(spexCategories.size()));
                final List<Long> spex = rnd.nextInt(4) == 0 && revivalsPerSpexCategory.containsKey(spexCategory.getId()) ?
                        revivalsPerSpexCategory.get(spexCategory.getId()) :
                        spexPerSpexCategory.get(spexCategory.getId());
                final long spexId = spex.get(rnd.nextInt(spex.size()));

                jdbcClient
                        .sql(spexActivitySql)
                        .param("activityId", activityId)
                        .param("spexId", spexId)
                        .param("createdBy", SYSTEM_USER)
                        .param("createdAt", LocalDateTime.now().atZone(ZoneId.of("UTC")))
                        .update();

                IntStream.range(0, rnd.nextInt(NUMBER_OF_SAMPLES_SPEXARE_MAX_TASK_ACTIVITIES_PER_ACTIVITY) + 1).forEach(j -> {
                    final KeyHolder taskActivityKeyHolder = new GeneratedKeyHolder();
                    final TaskCategory taskCategory = taskCategories.get(rnd.nextInt(taskCategories.size()));
                    final List<Long> tasks = tasksPerTaskCategory.get(taskCategory.getId());
                    final long taskId = tasks.get(rnd.nextInt(tasks.size()));

                    jdbcClient
                            .sql(taskActivitySql)
                            .param("activityId", activityId)
                            .param("taskId", taskId)
                            .param("createdBy", SYSTEM_USER)
                            .param("createdAt", LocalDateTime.now().atZone(ZoneId.of("UTC")))
                            .update(taskActivityKeyHolder);

                    if (taskActivityKeyHolder.getKey() != null && taskCategory.getActorPresent()) {
                        final long taskActivityId = taskActivityKeyHolder.getKey().longValue();

                        jdbcClient
                                .sql(actorSql)
                                .param("taskActivityId", taskActivityId)
                                .param("vocalId", rnd.nextBoolean() ? vocals.get(rnd.nextInt(vocals.size())).getId() : "UNKNOWN")
                                .param("role", rnd.nextBoolean() ?
                                        faker.collection(
                                                        () -> faker.oscarMovie().actor(),
                                                        () -> faker.oscarMovie().character(),
                                                        () -> faker.ancient().god(),
                                                        () -> faker.ancient().hero())
                                                .maxLen(1)
                                                .generate() :
                                        null)
                                .param("createdBy", SYSTEM_USER)
                                .param("createdAt", LocalDateTime.now().atZone(ZoneId.of("UTC")))
                                .update();
                    }
                });
            }
        });
    }

    private void createFixedSampleUsers() {
        createFixedSampleUser("admin", List.of("USER", "EDITOR", "ADMIN"));
        createFixedSampleUser("editor", List.of("USER", "EDITOR"));
        createFixedSampleUser("user", List.of("USER"));
    }

    private void createFixedSampleUser(final String name, final List<String> authorityIds) {
        final UserRepresentation userRepresentation = new UserRepresentation();

        userRepresentation.setEmail(name + SAMPLE_USER_DOMAIN);
        userRepresentation.setFirstName(StringUtils.capitalize(name));
        userRepresentation.setEnabled(true);
        userRepresentation.setEmailVerified(true);
        userRepresentation.setCredentials(List.of(sampleUserCredential()));

        try (final Response response = keycloakAdminClient
                .realm(keycloakRealm)
                .users()
                .create(userRepresentation)
        ) {
            if (response.getStatus() != HttpStatus.CREATED.value()) {
                throw new IllegalStateException("Could not create user " + userRepresentation.getEmail() + " in Keycloak");
            }

            final String locationPath = response.getLocation().getPath();

            keycloakAdminClient
                    .realm(keycloakRealm)
                    .users()
                    .get(locationPath.substring(locationPath.lastIndexOf('/') + 1))
                    .roles()
                    .clientLevel(keycloakClientId)
                    .add(authorityIds.stream().map(authorityService::getRoleRepresentationById).toList());
        }
    }

    private CredentialRepresentation sampleUserCredential() {
        final CredentialRepresentation credentialRepresentation = new CredentialRepresentation();

        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setValue(sampleUserPassword);
        credentialRepresentation.setTemporary(false);

        return credentialRepresentation;
    }

    private List<String> createSampleUsers(final JdbcClient jdbcClient) {
        final List<RoleRepresentation> authorities = new ArrayList<>();

        jdbcClient.sql("SELECT id FROM authority")
                .query()
                .listOfRows()
                .forEach(row -> authorities.add(authorityService.getRoleRepresentationById((String) Objects.requireNonNull(row.get("id")))));

        final List<Pair<String, Boolean>> states = new ArrayList<>();

        jdbcClient.sql("SELECT id, enabled FROM state")
                .query()
                .listOfRows()
                .forEach(row -> states.add(Pair.of((String) row.get("id"), (Boolean) row.get("enabled"))));

        final List<Long> spexare = new ArrayList<>();

        jdbcClient.sql("SELECT id FROM spexare")
                .query()
                .listOfRows()
                .forEach(row -> spexare.add((Long) Objects.requireNonNull(row.get("id"))));

        final String sql = """
                INSERT INTO user
                    (uuid, external_id, state_id, spexare_id, created_by, created_at, version)
                VALUES
                    (UUID(), :externalId, :stateId, :spexareId, :createdBy, :createdAt, 0)
                """;
        final CredentialRepresentation credentialRepresentation = sampleUserCredential();
        final List<Long> alreadyPickedSpexareIds = new ArrayList<>();
        final List<String> emailAddresses = new ArrayList<>();

        IntStream.range(0, NUMBER_OF_SAMPLES_USERS).forEach(i -> {
            final UserRepresentation userRepresentation = new UserRepresentation();
            final Pair<String, Boolean> state = states.get(rnd.nextInt(states.size()));

            userRepresentation.setEmail(faker.internet().emailAddress());
            userRepresentation.setEnabled(state.getRight());
            userRepresentation.setCredentials(List.of(credentialRepresentation));

            try (final Response response = keycloakAdminClient
                    .realm(keycloakRealm)
                    .users()
                    .create(userRepresentation)
            ) {
                if (response.getStatus() == HttpStatus.CREATED.value()) {
                    final String locationPath = response.getLocation().getPath();
                    final String externalId = locationPath.substring(locationPath.lastIndexOf('/') + 1);

                    try {
                        final UserResource userResource = keycloakAdminClient
                                .realm(keycloakRealm)
                                .users()
                                .get(externalId);

                        final RoleRepresentation authority = authorities.get(rnd.nextInt(authorities.size()));

                        userResource
                                .roles()
                                .clientLevel(keycloakClientId)
                                .add(List.of(authority));

                        final KeyHolder keyHolder = new GeneratedKeyHolder();
                        final long spexareId = getRandomSpexareId(spexare, alreadyPickedSpexareIds);

                        jdbcClient
                                .sql(sql)
                                .param("externalId", externalId)
                                .param("stateId", state.getLeft())
                                .param("spexareId", spexareId)
                                .param("createdBy", SYSTEM_USER)
                                .param("createdAt", randomizeCreatedAt())
                                .update(keyHolder);

                        if (keyHolder.getKey() != null) {
                            final long id = keyHolder.getKey().longValue();

                            emailAddresses.add(userRepresentation.getEmail());
                        }
                    } catch (final Exception e) {
                        throw new IllegalStateException("Could not retrieve newly created user in Keycloak", e);
                    }
                } else {
                    throw new IllegalStateException("Could not create user in Keycloak");
                }
            }
        });

        return List.copyOf(emailAddresses);
    }

    private Pair<Map<Long, List<Long>>, Map<Long, List<Long>>> getSpexPerSpexCategory(final JdbcClient jdbcClient, final List<SpexCategory> spexCategories) {
        final Map<Long, List<Long>> spexPerSpexCategory = new HashMap<>();
        final Map<Long, List<Long>> revivalsPerSpexCategory = new HashMap<>();

        spexCategories.forEach(spexCategory ->
                jdbcClient
                        .sql("SELECT id FROM spex_details WHERE category_id = :spexCategoryId")
                        .param("spexCategoryId", spexCategory.getId())
                        .query(resultSet -> {
                            final long spexDetailsId = resultSet.getLong("id");

                            jdbcClient
                                    .sql("SELECT id FROM spex WHERE details_id = :spexDetailsId AND parent_id IS NULL")
                                    .param("spexDetailsId", spexDetailsId)
                                    .query(spexResultSet -> {
                                        final long spexId = spexResultSet.getLong("id");

                                        spexPerSpexCategory.merge(spexCategory.getId(), new ArrayList<>(List.of(spexId)), (v1, v2) -> {
                                            v1.addAll(v2);
                                            return v1;
                                        });
                                    });
                            jdbcClient
                                    .sql("SELECT id FROM spex WHERE details_id = :spexDetailsId AND parent_id IS NOT NULL")
                                    .param("spexDetailsId", spexDetailsId)
                                    .query(spexResultSet -> {
                                        final long spexId = spexResultSet.getLong("id");

                                        revivalsPerSpexCategory.merge(spexCategory.getId(), new ArrayList<>(List.of(spexId)), (v1, v2) -> {
                                            v1.addAll(v2);
                                            return v1;
                                        });
                                    });
                        })
        );

        return Pair.of(spexPerSpexCategory, revivalsPerSpexCategory);
    }

    private List<SpexCategory> getSpexCategories(final JdbcClient jdbcClient) {
        return jdbcClient
                .sql("SELECT id FROM spex_category")
                .query((resultSet, rowNum) -> {
                    final SpexCategory spexCategory = new SpexCategory();

                    spexCategory.setId(resultSet.getLong("id"));

                    return spexCategory;
                })
                .list();
    }

    private Map<Long, List<Long>> getTasksPerTaskCategory(final JdbcClient jdbcClient, final List<TaskCategory> taskCategories) {
        final Map<Long, List<Long>> tasksPerTaskCategory = new HashMap<>();

        taskCategories.forEach(taskCategory ->
                jdbcClient
                        .sql("SELECT id FROM task WHERE category_id = :taskCategoryId")
                        .param("taskCategoryId", taskCategory.getId())
                        .query(resultSet -> {
                            final long taskId = resultSet.getLong("id");

                            tasksPerTaskCategory.merge(taskCategory.getId(), new ArrayList<>(List.of(taskId)), (v1, v2) -> {
                                v1.addAll(v2);
                                return v1;
                            });
                        }));

        return tasksPerTaskCategory;
    }

    private List<TaskCategory> getTaskCategories(final JdbcClient jdbcClient) {
        return jdbcClient
                .sql("SELECT id, actor_present FROM task_category")
                .query((resultSet, rowNum) -> {
                    final TaskCategory taskCategory = new TaskCategory();

                    taskCategory.setId(resultSet.getLong("id"));
                    taskCategory.setActorPresent(resultSet.getBoolean("actor_present"));

                    return taskCategory;
                })
                .list();
    }

    private List<Type> getVocals(final JdbcClient jdbcClient) {
        return jdbcClient
                .sql("SELECT id FROM type WHERE type = 'VOCAL'")
                .query((resultSet, rowNum) -> {
                    final Type vocal = new Type();

                    vocal.setId(resultSet.getString("id"));

                    return vocal;
                })
                .list();
    }

    private byte[] imageToByteArray(final String base64EncodedImage) {
        return Base64.getDecoder().decode(base64EncodedImage.substring(base64EncodedImage.indexOf(",") + 1));
    }

    private byte[] imageUrlToByteArray(final String urlString) {
        try {
            final URL url = URI.create(urlString).toURL();
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            try (final InputStream inputStream = url.openStream()) {
                final byte[] buffer = new byte[1024];
                int n;

                while (-1 != (n = inputStream.read(buffer))) {
                    outputStream.write(buffer, 0, n);
                }
            }

            return outputStream.toByteArray();
        } catch (final IOException e) {
            if (failedImageDownloads.incrementAndGet() == 1) {
                log.warn("Could not download a sample image from {}, generating it instead: {}", urlString, e.getMessage());
            }
            return imageToByteArray(faker.image().base64PNG());
        }
    }

    private Long getRandomSpexareId(final List<Long> spexareIds, final List<Long> alreadyPickedSpexareIds) {
        final List<Long> availableSpexareIds = spexareIds.stream()
                .filter(id -> !alreadyPickedSpexareIds.contains(id))
                .toList();

        if (availableSpexareIds.isEmpty()) {
            throw new IllegalStateException("No spexare ids available");
        }

        final Long pickedSpexareId = availableSpexareIds.get(rnd.nextInt(availableSpexareIds.size()));

        alreadyPickedSpexareIds.add(pickedSpexareId);

        return pickedSpexareId;
    }

    private OffsetDateTime randomizeCreatedAt() {
        return faker.timeAndDate().past(3 * 365, TimeUnit.DAYS).atZone(ZoneId.of("UTC")).toOffsetDateTime();
    }

    /**
     * A town sample spexare are placed around.
     *
     * @param latitude  the latitude of its centre
     * @param longitude the longitude of its centre
     * @param spread    how far from the centre, in degrees of latitude, most spexare end up
     * @param weight    how many spexare live there, relative to the other towns
     */
    private record SamplePlace(double latitude, double longitude, double spread, double weight) {
    }
}
