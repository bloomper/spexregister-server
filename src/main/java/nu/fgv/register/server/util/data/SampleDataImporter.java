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
import nu.fgv.register.server.util.security.CryptoConverter;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import nu.fgv.register.server.spexare.activity.task.actor.ActorCharacterService;
import nu.fgv.register.server.sync.SyncService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

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
 * Generates sample news, tags, spexare with their records and partners, and users. The Keycloak accounts are
 * fgv-infra-deploy's sample users (bin/seed-users.sh); this import never changes Keycloak.
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
    // fgv-infra-deploy's sample members (sample-users.tsv): member01 to member50, the last five disabled
    private static final int NUMBER_OF_SAMPLES_USERS = 50;
    private static final int NUMBER_OF_SAMPLES_INACTIVE_USERS = 5;
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
    private static final int SPEXIPEDIA_ATTEMPTS = 60;
    private static final Duration SPEXIPEDIA_RETRY_INTERVAL = Duration.ofSeconds(10);
    private final PermissionService permissionService;
    private final AuditDataSeeder auditDataSeeder;
    private final Random rnd = new SecureRandom();
    private final AtomicInteger failedImageDownloads = new AtomicInteger();
    private final Faker faker = new Faker(Locale.of("sv", "SE"));
    private final CustomSwedenIdNumber customSwedenIdNumber = new CustomSwedenIdNumber();
    private final CryptoConverter cryptoConverter;
    private final ObjectProvider<SyncService> syncService;
    private final ObjectProvider<ActorCharacterService> actorCharacterService;
    private final ObjectProvider<DataSource> dataSource;
    private final AtomicBoolean pending = new AtomicBoolean();
    // Spex key to the names of its characters, and its parent's for a revival, for sample actors' roles
    private Map<Long, List<String>> characterNames = Map.of();

    public SampleDataImporter(final PermissionService permissionService,
                              final AuditDataSeeder auditDataSeeder,
                              @Value("${spexregister.crypto.secret-key}") final String secretKey,
                              final ObjectProvider<SyncService> syncService,
                              final ObjectProvider<ActorCharacterService> actorCharacterService,
                              final ObjectProvider<DataSource> dataSource) {
        this.permissionService = permissionService;
        this.auditDataSeeder = auditDataSeeder;
        cryptoConverter = new CryptoConverter(secretKey);
        this.syncService = syncService;
        this.actorCharacterService = actorCharacterService;
        this.dataSource = dataSource;
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

        log.info("Importing sample data");
        timed("Emptied the tables", () -> purgeAllRelevantTables(jdbcClient));
        SecurityContextHolder.getContext().setAuthentication(AUTH);
        timed("Created the news", () -> createSampleNews(jdbcClient));
        timed("Created the tags", () -> createSampleTags(jdbcClient));
        SecurityContextHolder.clearContext();
        // Spex and tasks come from Spexipedia: the rest is made once the application runs and has pulled its feed
        pending.set(true);
    }

    /**
     * The second half of the sample data: waits for Spexipedia, pulls its change feed from the start (spex categories,
     * spex, revivals, task categories, tasks and characters with Spexipedia's sample ids), then makes the spexare, with
     * activities on those spex and actors playing their characters, and the users.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void completeSampleData() {
        if (!pending.getAndSet(false)) {
            return;
        }

        final JdbcClient jdbcClient = JdbcClient.create(dataSource.getObject());
        final long started = System.nanoTime();

        timed("Copied base data from Spexipedia", this::pullBaseData);
        SecurityContextHolder.getContext().setAuthentication(AUTH);
        try {
            characterNames = characterNames(jdbcClient);
            failedImageDownloads.set(0);
            timed("Created the spexare", () -> createSampleSpexare(jdbcClient));
            timed("Created Spexipedia's fixed spexare", () -> createFixedSpexare(jdbcClient));
            timed("Placed the spexare on the map", () -> createSampleLocations(jdbcClient));
            if (failedImageDownloads.get() > 0) {
                log.warn("{} sample spexare images could not be downloaded and were generated instead", failedImageDownloads.get());
            }

            final List<String> users = timedResult("Linked the sample members to spexare", () -> createSampleUsers(jdbcClient));

            timed("Seeded the sample history", () -> auditDataSeeder.seedSampleHistory(jdbcClient, users));
            timed("Linked actors to characters", () -> actorCharacterService.getObject().linkByRole());
        } finally {
            SecurityContextHolder.clearContext();
        }
        log.info("Imported the rest of the sample data in {} s", Duration.ofNanos(System.nanoTime() - started).toSeconds());
    }

    /**
     * Pulls Spexipedia's feed from the start, waiting for Spexipedia for up to ten minutes; refuses without it.
     */
    private void pullBaseData() {
        final SyncService sync = syncService.getObject();

        for (int attempt = 1; attempt <= SPEXIPEDIA_ATTEMPTS; attempt++) {
            final SyncService.Result result = sync.pull(true);

            if (result != null && result.error() == null && result.applied() > 0) {
                log.info("Copied {} changes from Spexipedia", result.applied());
                return;
            }
            log.info("Waiting for Spexipedia's sample data ({}/{}): {}", attempt, SPEXIPEDIA_ATTEMPTS,
                    result == null ? "a pull is running" : Objects.requireNonNullElse(result.error(), "no changes yet"));
            try {
                Thread.sleep(SPEXIPEDIA_RETRY_INTERVAL);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for Spexipedia", e);
            }
        }
        throw new IllegalStateException("Spexipedia's base data is needed for the sample data: start Spexipedia with its sample data first");
    }

    private static Map<Long, List<String>> characterNames(final JdbcClient jdbcClient) {
        final Map<Long, List<String>> names = new HashMap<>();

        jdbcClient.sql("""
                        SELECT s.id, c.name FROM spex s
                        JOIN spex_character c ON c.spex_id = s.id OR c.spex_id = s.parent_id
                        WHERE c.retired = 0""")
                .query((rs, _) -> Map.entry(rs.getLong(1), rs.getString(2)))
                .list()
                .forEach(entry -> names.computeIfAbsent(entry.getKey(), _ -> new ArrayList<>()).add(entry.getValue()));
        return names;
    }

    /**
     * The spexare Spexipedia's sample credits refer to, with the same ids: published and unpublished, with and without
     * nicknames. The first two also played in, and sat on the board of, the earliest sample spex.
     */
    private void createFixedSpexare(final JdbcClient jdbcClient) {
        final String[][] fixed = {
                {"00000000-0000-7000-a000-000000000001", "Bertil", "Basson", "Basen", "true"},
                {"00000000-0000-7000-a000-000000000002", "Cecilia", "Cellodal", "Cellan", "true"},
                {"00000000-0000-7000-a000-000000000003", "David", "Dirigentsson", null, "true"},
                {"00000000-0000-7000-a000-000000000004", "Eva", "Ensemblé", "Evita", "false"},
                {"00000000-0000-7000-a000-000000000005", "Fredrik", "Fiolin", "Fiolen", "true"},
                {"00000000-0000-7000-a000-000000000006", "Greta", "Gitarrqvist", null, "false"}};
        final Long earliest = jdbcClient.sql("SELECT id FROM spex WHERE parent_id IS NULL ORDER BY year, id LIMIT 1").query(Long.class).optional().orElse(null);
        final Long ensemble = jdbcClient.sql("SELECT t.id FROM task t JOIN task_category c ON c.id = t.category_id WHERE c.actor_present = 1 ORDER BY t.id LIMIT 1")
                .query(Long.class).optional().orElse(null);
        final Long board = jdbcClient.sql("SELECT t.id FROM task t JOIN task_category c ON c.id = t.category_id WHERE c.board = 1 ORDER BY t.name LIMIT 1")
                .query(Long.class).optional().orElse(null);

        for (int i = 0; i < fixed.length; i++) {
            final String[] row = fixed[i];
            final KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcClient.sql("""
                            INSERT INTO spexare (uuid, first_name, last_name, nick_name, deceased, published, created_by, created_at, version)
                            VALUES (:uuid, :first, :last, :nick, 0, :published, :createdBy, :createdAt, 0)""")
                    .param("uuid", row[0]).param("first", row[1]).param("last", row[2]).param("nick", row[3])
                    .param("published", Boolean.parseBoolean(row[4]))
                    .param("createdBy", SYSTEM_USER).param("createdAt", LocalDateTime.now().atZone(ZoneId.of("UTC")))
                    .update(keyHolder);
            if (earliest == null || keyHolder.getKey() == null || i > 3) {
                continue;
            }

            final long spexareId = keyHolder.getKey().longValue();
            final long activityId = insertActivity(jdbcClient, spexareId, earliest);

            if (ensemble != null) {
                final List<String> characters = characterNames.getOrDefault(earliest, List.of());
                final long taskActivityId = insertTaskActivity(jdbcClient, activityId, ensemble);

                jdbcClient.sql("""
                                INSERT INTO actor (uuid, task_activity_id, vocal_id, role, created_by, created_at, version)
                                VALUES (UUID(), :ta, 'UNKNOWN', :role, :createdBy, :createdAt, 0)""")
                        .param("ta", taskActivityId).param("role", characters.isEmpty() ? null : characters.get(i % characters.size()))
                        .param("createdBy", SYSTEM_USER).param("createdAt", LocalDateTime.now().atZone(ZoneId.of("UTC")))
                        .update();
            }
            if (board != null && i < 2) {
                insertTaskActivity(jdbcClient, activityId, board);
            }
        }
    }

    private long insertActivity(final JdbcClient jdbcClient, final long spexareId, final long spexId) {
        final KeyHolder activity = new GeneratedKeyHolder();

        jdbcClient.sql("INSERT INTO activity (uuid, spexare_id, created_by, created_at, version) VALUES (UUID(), :s, :by, :at, 0)")
                .param("s", spexareId).param("by", SYSTEM_USER).param("at", LocalDateTime.now().atZone(ZoneId.of("UTC"))).update(activity);

        final long activityId = Objects.requireNonNull(activity.getKey()).longValue();

        jdbcClient.sql("INSERT INTO spex_activity (uuid, activity_id, spex_id, created_by, created_at, version) VALUES (UUID(), :a, :s, :by, :at, 0)")
                .param("a", activityId).param("s", spexId).param("by", SYSTEM_USER).param("at", LocalDateTime.now().atZone(ZoneId.of("UTC"))).update();
        return activityId;
    }

    private long insertTaskActivity(final JdbcClient jdbcClient, final long activityId, final long taskId) {
        final KeyHolder taskActivity = new GeneratedKeyHolder();

        jdbcClient.sql("INSERT INTO task_activity (uuid, activity_id, task_id, created_by, created_at, version) VALUES (UUID(), :a, :t, :by, :at, 0)")
                .param("a", activityId).param("t", taskId).param("by", SYSTEM_USER).param("at", LocalDateTime.now().atZone(ZoneId.of("UTC")))
                .update(taskActivity);
        return Objects.requireNonNull(taskActivity.getKey()).longValue();
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
                "spex_character",
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

        jdbcClient.sql("UPDATE spex SET parent_id = NULL").update();
        tables.forEach(table ->
                jdbcClient
                        .sql("DELETE FROM %s".formatted(table))
                        .update()
        );
        // The copy of base data is pulled again from the start of Spexipedia's feed
        jdbcClient.sql("UPDATE sync_source SET cursor_value = NULL, applied = 0, last_error = NULL").update();
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
                                .param("role", !characterNames.getOrDefault(spexId, List.of()).isEmpty() && rnd.nextInt(5) != 0
                                        ? characterNames.get(spexId).get(rnd.nextInt(characterNames.get(spexId).size()))
                                        : rnd.nextBoolean() ?
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

    /**
     * Spexregister's users for fgv-infra-deploy's sample members (bin/seed-users.sh, sample-users.tsv), which have their
     * Keycloak accounts, roles and enabled flags there: {@code member01@fgv.nu} to {@code member50@fgv.nu}, with fixed
     * ids, the last five inactive. Each is linked to a random spexare; Keycloak is never asked. The other sample users
     * ({@code admin@fgv.nu} and so on) become users through the user sync, as real ones do.
     *
     * @return the members' email addresses
     */
    private List<String> createSampleUsers(final JdbcClient jdbcClient) {
        final List<Long> spexare = jdbcClient.sql("SELECT id FROM spexare").query(Long.class).list();
        final List<Long> alreadyPickedSpexareIds = new ArrayList<>();
        final List<String> emailAddresses = new ArrayList<>();
        final String sql = """
                INSERT INTO user
                    (uuid, external_id, state_id, spexare_id, created_by, created_at, version)
                VALUES
                    (UUID(), :externalId, :stateId, :spexareId, :createdBy, :createdAt, 0)
                """;

        IntStream.rangeClosed(1, NUMBER_OF_SAMPLES_USERS).forEach(i -> {
            jdbcClient
                    .sql(sql)
                    .param("externalId", "00000000-0000-4000-8000-%012d".formatted(1000 + i))
                    .param("stateId", i > NUMBER_OF_SAMPLES_USERS - NUMBER_OF_SAMPLES_INACTIVE_USERS ? "INACTIVE" : "ACTIVE")
                    .param("spexareId", getRandomSpexareId(spexare, alreadyPickedSpexareIds))
                    .param("createdBy", SYSTEM_USER)
                    .param("createdAt", randomizeCreatedAt())
                    .update();
            emailAddresses.add("member%02d%s".formatted(i, SAMPLE_USER_DOMAIN));
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
