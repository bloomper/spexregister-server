package nu.fgv.register.server.util.migration;

import net.datafaker.Faker;
import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.spex.category.SpexCategory;
import nu.fgv.register.server.task.category.TaskCategory;
import org.apache.commons.lang3.tuple.Pair;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class R__InsertSampleData extends BaseJavaMigration {
    private static final int NUMBER_OF_SAMPLES_NEWS = 20;
    private static final int NUMBER_OF_SAMPLES_TAGS = 5;
    private static final int NUMBER_OF_SAMPLES_SPEXARE = 500;
    private static final int NUMBER_OF_SAMPLES_SPEXARE_WITH_PARTNER = NUMBER_OF_SAMPLES_SPEXARE / 10;
    private static final int NUMBER_OF_SAMPLES_SPEXARE_MAX_MEMBERSHIPS_OF_EACH_TYPE = 3;
    private static final int NUMBER_OF_SAMPLES_SPEXARE_MAX_ACTIVITIES = 5;
    private static final int NUMBER_OF_SAMPLES_SPEXARE_MAX_TASK_ACTIVITIES_PER_ACTIVITY = 3;
    private static final String SYSTEM_USER = "system";

    private final Random rnd = new SecureRandom();
    private final Faker faker = new Faker(Locale.of("sv", "SE"));

    @Override
    public void migrate(final Context context) {
        if (Boolean.parseBoolean(System.getenv("spexregister-insert-sample-data"))) {
            final JdbcClient jdbcClient = JdbcClient.create(new SingleConnectionDataSource(context.getConnection(), true));

            purgeAllRelevantTables(jdbcClient);

            ScriptUtils.executeSqlScript(context.getConnection(), new ClassPathResource("db/sampledata/tasks.sql"));
            ScriptUtils.executeSqlScript(context.getConnection(), new ClassPathResource("db/sampledata/spex.sql"));

            createSampleSpexCategoryLogos(jdbcClient);
            createSampleSpexDetailsPosters(jdbcClient);
            createSampleNews(jdbcClient);
            createSampleTags(jdbcClient);
            createSampleSpexare(jdbcClient);
        }
    }

    private void purgeAllRelevantTables(final JdbcClient jdbcClient) {
        final List<String> tables = List.of(
                "actor",
                "task_activity",
                "spex_activity",
                "activity",
                "tagging",
                "toggle",
                "membership",
                "consent",
                "spexare",
                "task_category",
                "task",
                "spex",
                "spex_details",
                "spex_category",
                "news",
                "tag"
        );

        tables.forEach(table ->
                jdbcClient
                        .sql("DELETE FROM %s".formatted(table))
                        .update()
        );
    }

    private void createSampleSpexCategoryLogos(final JdbcClient jdbcClient) {
        final String sql = """
                UPDATE spex_category
                SET
                    logo = :logo,
                    logo_content_type = :logoContentType
                WHERE
                    id = :id
                """;

        jdbcClient.sql("SELECT id FROM spex_category")
                .query()
                .listOfRows()
                .forEach(row ->
                        jdbcClient
                                .sql(sql)
                                .param("logo", imageToByteArray(faker.image().base64SVG()))
                                .param("logoContentType", "image/svg+xml")
                                .param("id", row.get("id"))
                                .update());
    }

    private void createSampleSpexDetailsPosters(final JdbcClient jdbcClient) {
        final String sql = """
                UPDATE spex_details
                SET
                    poster = :poster,
                    poster_content_type = :posterContentType
                WHERE
                    id = :id
                """;

        jdbcClient.sql("SELECT id FROM spex_details")
                .query()
                .listOfRows()
                .forEach(row ->
                        jdbcClient
                                .sql(sql)
                                .param("poster", imageToByteArray(faker.image().base64SVG()))
                                .param("posterContentType", "image/svg+xml")
                                .param("id", row.get("id"))
                                .update());
    }

    private void createSampleNews(final JdbcClient jdbcClient) {
        final String sql = """
                INSERT INTO news
                    (visible_from, visible_to, subject, text, created_by, created_at)
                VALUES
                    (:visibleFrom, :visibleTo, :subject, :text, :createdBy, :createdAt)
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
                    .param("createdBy", SYSTEM_USER)
                    .param("createdAt", LocalDateTime.now())
                    .update();
        });
    }

    private void createSampleTags(final JdbcClient jdbcClient) {
        final String sql = """
                INSERT INTO tag
                    (name, created_by, created_at)
                VALUES
                    (:name, :createdBy, :createdAt)
                """;

        IntStream.range(0, NUMBER_OF_SAMPLES_TAGS).forEach(i ->
                jdbcClient
                        .sql(sql)
                        .param("name", faker.lorem().word())
                        .param("createdBy", SYSTEM_USER)
                        .param("createdAt", LocalDateTime.now())
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
                    (first_name, last_name, nick_name, social_security_number, graduation, comment, created_by, created_at)
                VALUES
                    (:firstName, :lastName, :nickName, :socialSecurityNumber, :graduation, :comment, :createdBy, :createdAt)
                """;

        IntStream.range(0, NUMBER_OF_SAMPLES_SPEXARE).forEach(i -> {
            final KeyHolder keyHolder = new GeneratedKeyHolder();

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
                    .param("socialSecurityNumber", rnd.nextBoolean() ? faker.idNumber().valid() : null)
                    .param("graduation", rnd.nextBoolean() ? faker.regexify("[A|B|D|E|G|K|M|I|V|T]\\d{2}") : null)
                    .param("comment", rnd.nextBoolean() ? faker.lorem().paragraph() : null)
                    .param("createdBy", SYSTEM_USER)
                    .param("createdAt", LocalDateTime.now())
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

    private void createSpexareImage(final JdbcClient jdbcClient, final long spexareId) {
        final String sql = """
                UPDATE spexare
                SET
                    image = :image,
                    image_content_type = :imageContentType
                WHERE
                    id = :id
                """;

        jdbcClient
                .sql(sql)
                .param("image", imageUrlToByteArray(faker.avatar().image()))
                .param("imageContentType", "image/png")
                .param("id", spexareId)
                .update();
    }

    private void createSpexareAddresses(final JdbcClient jdbcClient, final long spexareId) {
        final String sql = """
                INSERT INTO address
                    (street_address, postal_code, city, country, phone, phone_mobile, email_address, type_id, spexare_id, created_by, created_at)
                VALUES
                    (:streetAddress, :postalCode, :city, :country, :phone, :phoneMobile, :emailAddress, :typeId, :spexareId, :createdBy, :createdAt)
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
                                .param("country", faker.address().country())
                                .param("phone", rnd.nextBoolean() ? faker.phoneNumber().phoneNumber() : null)
                                .param("phoneMobile", rnd.nextBoolean() ? faker.phoneNumber().cellPhone() : null)
                                .param("emailAddress", rnd.nextBoolean() ? faker.internet().emailAddress() : null)
                                .param("typeId", typeId)
                                .param("spexareId", spexareId)
                                .param("createdBy", SYSTEM_USER)
                                .param("createdAt", LocalDateTime.now())
                                .update();
                    }
                });
    }

    private void createSpexareConsents(final JdbcClient jdbcClient, final long spexareId) {
        final String sql = """
                INSERT INTO consent
                    (value, type_id, spexare_id, created_by, created_at)
                VALUES
                    (:value, :typeId, :spexareId, :createdBy, :createdAt)
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
                            .param("createdAt", LocalDateTime.now())
                            .update();
                });
    }

    private void createSpexareMemberships(final JdbcClient jdbcClient, final long spexareId) {
        final String sql = """
                INSERT INTO membership
                    (year, type_id, spexare_id, created_by, created_at)
                VALUES
                    (:year, :typeId, :spexareId, :createdBy, :createdAt)
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
                                        .param("createdAt", LocalDateTime.now())
                                        .update()
                        );
                    }
                });
    }

    private void createSpexareToggles(final JdbcClient jdbcClient, final long spexareId) {
        final String sql = """
                INSERT INTO toggle
                    (value, type_id, spexare_id, created_by, created_at)
                VALUES
                    (:value, :typeId, :spexareId, :createdBy, :createdAt)
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
                            .param("createdAt", LocalDateTime.now())
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
                    (spexare_id, created_by, created_at)
                VALUES
                    (:spexareId, :createdBy, :createdAt)
                """;
        final String spexActivitySql = """
                INSERT INTO spex_activity
                    (activity_id, spex_id, created_by, created_at)
                VALUES
                    (:activityId, :spexId, :createdBy, :createdAt)
                """;
        final String taskActivitySql = """
                INSERT INTO task_activity
                    (activity_id, task_id, created_by, created_at)
                VALUES
                    (:activityId, :taskId, :createdBy, :createdAt)
                """;
        final String actorSql = """
                INSERT INTO actor
                    (task_activity_id, vocal_id, role, created_by, created_at)
                VALUES
                    (:taskActivityId, :vocalId, :role, :createdBy, :createdAt)
                """;

        IntStream.range(0, rnd.nextInt(NUMBER_OF_SAMPLES_SPEXARE_MAX_ACTIVITIES)).forEach(i -> {
            final KeyHolder activityKeyHolder = new GeneratedKeyHolder();

            jdbcClient
                    .sql(activitySql)
                    .param("spexareId", spexareId)
                    .param("createdBy", SYSTEM_USER)
                    .param("createdAt", LocalDateTime.now())
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
                        .param("createdAt", LocalDateTime.now())
                        .update();

                IntStream.range(0, rnd.nextInt(NUMBER_OF_SAMPLES_SPEXARE_MAX_TASK_ACTIVITIES_PER_ACTIVITY)).forEach(j -> {
                    final KeyHolder taskActivityKeyHolder = new GeneratedKeyHolder();
                    final TaskCategory taskCategory = taskCategories.get(rnd.nextInt(taskCategories.size()));
                    final List<Long> tasks = tasksPerTaskCategory.get(taskCategory.getId());
                    final long taskId = tasks.get(rnd.nextInt(tasks.size()));

                    jdbcClient
                            .sql(taskActivitySql)
                            .param("activityId", activityId)
                            .param("taskId", taskId)
                            .param("createdBy", SYSTEM_USER)
                            .param("createdAt", LocalDateTime.now())
                            .update(taskActivityKeyHolder);

                    if (taskActivityKeyHolder.getKey() != null && taskCategory.getHasActor()) {
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
                                .param("createdAt", LocalDateTime.now())
                                .update();
                    }
                });
            }
        });
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
                                        final long spexId = resultSet.getLong("id");

                                        spexPerSpexCategory.merge(spexCategory.getId(), new ArrayList<>(List.of(spexId)), (v1, v2) -> {
                                            v1.addAll(v2);
                                            return v1;
                                        });
                                    });
                            jdbcClient
                                    .sql("SELECT id FROM spex WHERE details_id = :spexDetailsId AND parent_id IS NOT NULL")
                                    .param("spexDetailsId", spexDetailsId)
                                    .query(spexResultSet -> {
                                        final long spexId = resultSet.getLong("id");

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
                .sql("SELECT id, has_actor FROM task_category")
                .query((resultSet, rowNum) -> {
                    final TaskCategory taskCategory = new TaskCategory();

                    taskCategory.setId(resultSet.getLong("id"));
                    taskCategory.setHasActor(resultSet.getBoolean("has_actor"));

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
            return imageToByteArray(faker.image().base64PNG());
        }
    }
}
