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

package nu.fgv.register.server.util.migration;

import jakarta.ws.rs.core.Response;
import net.datafaker.Faker;
import net.datafaker.providers.base.IdNumber;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.news.News;
import nu.fgv.register.server.news.NewsMapper;
import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.category.SpexCategory;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.task.Task;
import nu.fgv.register.server.task.category.TaskCategory;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.user.authority.AuthorityService;
import nu.fgv.register.server.util.security.CryptoConverter;
import org.apache.commons.lang3.tuple.Pair;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
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
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.ObjectIdentity;
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
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_ADMIN_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_EDITOR_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_USER_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
public class R__ImportSampleData extends BaseJavaMigration {
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
    private static final String SAMPLE_PASSWORD = "s3cr3t";
    protected static final Authentication AUTH = new TestingAuthenticationToken(SYSTEM_USER, "ignored", "ROLE_ADMIN");

    private final PermissionService permissionService;
    private final AuthorityService authorityService;
    @Value("${spexregister.sample-data.import:false}")
    private final boolean importSampleData;
    private final Keycloak keycloakAdminClient;
    private final String keycloakClientId;
    @Value("${spexregister.keycloak.realm}")
    private String keycloakRealm;

    private final Random rnd = new SecureRandom();
    private final Faker faker = new Faker(Locale.of("sv", "SE"));
    private final CustomSwedenIdNumber customSwedenIdNumber = new CustomSwedenIdNumber();
    private final CryptoConverter cryptoConverter;

    public R__ImportSampleData(final PermissionService permissionService,
                               final AuthorityService authorityService,
                               final Keycloak keycloakAdminClient,
                               final String keycloakClientId,
                               @Value("${spexregister.sample-data.import:false}") final boolean importSampleData,
                               @Value("${spexregister.crypto.algorithm}") final String algorithm,
                               @Value("${spexregister.crypto.secret-key}") final String secretKey,
                               @Value("${spexregister.crypto.initialization-vector}") final String iv) {
        this.permissionService = permissionService;
        this.authorityService = authorityService;
        this.keycloakAdminClient = keycloakAdminClient;
        this.keycloakClientId = keycloakClientId;
        this.importSampleData = importSampleData;
        cryptoConverter = new CryptoConverter(algorithm, secretKey, iv);
    }

    @Override
    public Integer getChecksum() {
        return rnd.nextInt();
    }

    @Override
    public void migrate(final Context context) {
        if (importSampleData) {
            final JdbcClient jdbcClient = JdbcClient.create(new SingleConnectionDataSource(context.getConnection(), true));

            purgeAllRelevantTables(jdbcClient);
            purgeKeycloak();
            SecurityContextHolder.getContext().setAuthentication(AUTH);

            createSampleTaskCategoriesAndTasks(context.getConnection(), jdbcClient);
            createSampleSpexCategoriesAndSpex(context.getConnection(), jdbcClient);
            createSampleNews(jdbcClient);
            createSampleTags(jdbcClient);
            createSampleSpexare(jdbcClient);
            createSampleUsers(jdbcClient);

            SecurityContextHolder.clearContext();
        }
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

        final List<String> tables = List.of(
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
                "news",
                "tag",
                "user",
                "acl_entry",
                "acl_object_identity",
                "acl_class",
                "acl_sid"
        );

        tables.forEach(table ->
                jdbcClient
                        .sql("DELETE FROM %s".formatted(table))
                        .update()
        );
    }

    private void purgeKeycloak() {
        keycloakAdminClient
                .realm(keycloakRealm)
                .users()
                .list()
                .stream()
                .filter(r -> !r.getEmail().endsWith("@spexregister.com"))
                .forEach(representation -> {
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

        jdbcClient.sql("SELECT id FROM task_category")
                .query()
                .listOfRows()
                .forEach(row -> {
                    final Long id = (Long) row.get("id");
                    final ObjectIdentity oid = toObjectIdentity(TaskCategory.class, id);

                    permissionService.grantPermission(oid, BasePermission.ADMINISTRATION, ROLE_ADMIN_SID);
                    permissionService.grantPermission(oid, BasePermission.READ, ROLE_EDITOR_SID, ROLE_USER_SID);
                    permissionService.grantPermission(oid, BasePermission.WRITE, ROLE_EDITOR_SID);
                });

        final String taskSql = "UPDATE task SET created_at = :createdAt WHERE id = :id";

        jdbcClient.sql("SELECT id FROM task")
                .query()
                .listOfRows()
                .forEach(row -> {
                    final Long id = (Long) row.get("id");
                    final ObjectIdentity oid = toObjectIdentity(Task.class, id);

                    permissionService.grantPermission(oid, BasePermission.ADMINISTRATION, ROLE_ADMIN_SID);
                    permissionService.grantPermission(oid, BasePermission.READ, ROLE_EDITOR_SID, ROLE_USER_SID);
                    permissionService.grantPermission(oid, BasePermission.WRITE, ROLE_EDITOR_SID);

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
                    logo = :logo,
                    logo_content_type = :logoContentType
                WHERE
                    id = :id
                """;

        jdbcClient.sql("SELECT id FROM spex_category")
                .query()
                .listOfRows()
                .forEach(row -> {
                    final Long id = (Long) row.get("id");
                    final ObjectIdentity oid = toObjectIdentity(SpexCategory.class, id);

                    permissionService.grantPermission(oid, BasePermission.ADMINISTRATION, ROLE_ADMIN_SID);
                    permissionService.grantPermission(oid, BasePermission.READ, ROLE_EDITOR_SID, ROLE_USER_SID);

                    jdbcClient
                            .sql(categorySql)
                            .param("logo", imageToByteArray(faker.image().base64SVG()))
                            .param("logoContentType", "image/svg+xml")
                            .param("id", id)
                            .update();
                });

        final String detailsSql = """
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
                                .sql(detailsSql)
                                .param("poster", imageToByteArray(faker.image().base64SVG()))
                                .param("posterContentType", "image/svg+xml")
                                .param("id", row.get("id"))
                                .update()
                );

        final String spexSql = "UPDATE spex SET created_at = :createdAt WHERE id = :id";

        jdbcClient.sql("SELECT id FROM spex")
                .query()
                .listOfRows()
                .forEach(row -> {
                    final Long id = (Long) row.get("id");
                    final ObjectIdentity oid = toObjectIdentity(Spex.class, id);

                    permissionService.grantPermission(oid, BasePermission.ADMINISTRATION, ROLE_ADMIN_SID);
                    permissionService.grantPermission(oid, BasePermission.READ, ROLE_EDITOR_SID, ROLE_USER_SID);
                    permissionService.grantPermission(oid, BasePermission.WRITE, ROLE_EDITOR_SID);

                    jdbcClient.sql(spexSql)
                            .param("createdAt", randomizeCreatedAt())
                            .param("id", id)
                            .update();
                });
    }

    private void createSampleNews(final JdbcClient jdbcClient) {
        final String sql = """
                INSERT INTO news
                    (visible_from, visible_to, subject, text, published, created_by, created_at)
                VALUES
                    (:visibleFrom, :visibleTo, :subject, :text, :published, :createdBy, :createdAt)
                """;

        IntStream.range(0, NUMBER_OF_SAMPLES_NEWS).forEach(i -> {
            final Instant visibleFrom = faker.timeAndDate().past(10, TimeUnit.DAYS, Instant.now().plus(2, ChronoUnit.DAYS));
            final Instant visibleTo = faker.timeAndDate().future(20, TimeUnit.DAYS, visibleFrom);
            final KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcClient
                    .sql(sql)
                    .param("visibleFrom", visibleFrom)
                    .param("visibleTo", visibleTo)
                    .param("subject", faker.lorem().maxLengthSentence(255))
                    .param("text", faker.lorem().paragraphs(5).stream().collect(Collectors.joining(System.lineSeparator())))
                    .param("published", NewsMapper.NEWS_MAPPER.isPublished(LocalDate.ofInstant(visibleFrom, ZoneId.systemDefault()), LocalDate.ofInstant(visibleTo, ZoneId.systemDefault())))
                    .param("createdBy", SYSTEM_USER)
                    .param("createdAt", LocalDateTime.now().atZone(ZoneId.of("UTC")))
                    .update(keyHolder);

            if (keyHolder.getKey() != null) {
                final long id = keyHolder.getKey().longValue();
                final ObjectIdentity oid = toObjectIdentity(News.class, id);

                permissionService.grantPermission(oid, BasePermission.ADMINISTRATION, ROLE_ADMIN_SID);
                permissionService.grantPermission(oid, BasePermission.READ, ROLE_EDITOR_SID);
                permissionService.grantPermission(oid, BasePermission.WRITE, ROLE_EDITOR_SID);
                if (NewsMapper.NEWS_MAPPER.isPublished(LocalDate.ofInstant(visibleFrom, ZoneId.systemDefault()), LocalDate.ofInstant(visibleTo, ZoneId.systemDefault()))) {
                    permissionService.grantPermission(oid, ROLE_USER_SID, BasePermission.READ);
                }
            }
        });
    }

    private void createSampleTags(final JdbcClient jdbcClient) {
        final String sql = """
                INSERT INTO tag
                    (name, created_by, created_at)
                VALUES
                    (:name, :createdBy, :createdAt)
                """;

        IntStream.range(0, NUMBER_OF_SAMPLES_TAGS).forEach(i -> {
            final KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcClient
                    .sql(sql)
                    .param("name", faker.lorem().word())
                    .param("createdBy", SYSTEM_USER)
                    .param("createdAt", LocalDateTime.now().atZone(ZoneId.of("UTC")))
                    .update(keyHolder);

            if (keyHolder.getKey() != null) {
                final long id = keyHolder.getKey().longValue();
                final ObjectIdentity oid = toObjectIdentity(Tag.class, id);

                permissionService.grantPermission(oid, BasePermission.ADMINISTRATION, ROLE_ADMIN_SID);
                permissionService.grantPermission(oid, BasePermission.READ, ROLE_ADMIN_SID, ROLE_USER_SID);
                permissionService.grantPermission(oid, BasePermission.WRITE, ROLE_EDITOR_SID);
            }
        });
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
                    (first_name, last_name, nick_name, social_security_number, deceased, published, graduation, comment, created_by, created_at)
                VALUES
                    (:firstName, :lastName, :nickName, :socialSecurityNumber, :deceased, :published, :graduation, :comment, :createdBy, :createdAt)
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
                final ObjectIdentity oid = toObjectIdentity(Spexare.class, spexareId);

                permissionService.grantPermission(oid, BasePermission.ADMINISTRATION, ROLE_ADMIN_SID);
                if (published) {
                    permissionService.grantPermission(oid, BasePermission.READ, ROLE_EDITOR_SID, ROLE_USER_SID);
                    permissionService.grantPermission(oid, BasePermission.WRITE, ROLE_EDITOR_SID);
                }

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
                            .param("createdAt", LocalDateTime.now().atZone(ZoneId.of("UTC")))
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
                                        .param("createdAt", LocalDateTime.now().atZone(ZoneId.of("UTC")))
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

    private void createSampleUsers(final JdbcClient jdbcClient) {
        final List<RoleRepresentation> authorities = new ArrayList<>();

        jdbcClient.sql("SELECT id FROM authority")
                .query()
                .listOfRows()
                .forEach(row -> authorities.add(authorityService.getRoleRepresentationById((String) row.get("id"))));

        final List<String> states = new ArrayList<>();

        jdbcClient.sql("SELECT id FROM state")
                .query()
                .listOfRows()
                .forEach(row -> states.add((String) row.get("id")));

        final List<Long> spexare = new ArrayList<>();

        jdbcClient.sql("SELECT id FROM spexare")
                .query()
                .listOfRows()
                .forEach(row -> spexare.add((Long) row.get("id")));

        final String sql = """
                INSERT INTO user
                    (external_id, state_id, spexare_id, created_by, created_at)
                VALUES
                    (:externalId, :stateId, :spexareId, :createdBy, :createdAt)
                """;
        final CredentialRepresentation credentialRepresentation = new CredentialRepresentation();

        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setValue(SAMPLE_PASSWORD);
        credentialRepresentation.setTemporary(false);

        final List<Long> alreadyPickedSpexareIds = new ArrayList<>();

        IntStream.range(0, NUMBER_OF_SAMPLES_USERS).forEach(i -> {
            final UserRepresentation userRepresentation = new UserRepresentation();

            userRepresentation.setEmail(faker.internet().emailAddress());
            userRepresentation.setEnabled(true);
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
                                .param("stateId", states.get(rnd.nextInt(states.size())))
                                .param("spexareId", spexareId)
                                .param("createdBy", SYSTEM_USER)
                                .param("createdAt", randomizeCreatedAt())
                                .update(keyHolder);

                        if (keyHolder.getKey() != null) {
                            final long id = keyHolder.getKey().longValue();
                            final ObjectIdentity oid = toObjectIdentity(User.class, id);
                            final ObjectIdentity spexareOid = toObjectIdentity(Spexare.class, spexareId);

                            permissionService.grantPermission(oid, BasePermission.ADMINISTRATION, ROLE_ADMIN_SID);
                            permissionService.grantPermission(spexareOid, BasePermission.WRITE, new PrincipalSid(externalId));
                        }
                    } catch (final Exception e) {
                        throw new IllegalStateException("Could not retrieve newly created user in Keycloak", e);
                    }
                } else {
                    throw new IllegalStateException("Could not create user in Keycloak");
                }
            }
        });

        jdbcClient
                .sql("SELECT u.external_id, s.id FROM user u LEFT JOIN spexare s ON s.id = u.spexare_id WHERE s.partner_id IS NOT NULL")
                .query()
                .listOfRows()
                .forEach(row -> {
                    final String externalId = (String) row.get("external_id");
                    final Long spexareId = (Long) row.get("id");

                    final ObjectIdentity oid = toObjectIdentity(Spexare.class, spexareId);

                    permissionService.grantPermission(oid, BasePermission.WRITE, new PrincipalSid(externalId));
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
        } catch (final IOException _) {
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
}
