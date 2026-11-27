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

package nu.fgv.register.server.imports;

import nu.fgv.register.server.imports.ImportEngine.Batch;
import nu.fgv.register.server.imports.ImportEngine.Handler;
import nu.fgv.register.server.imports.ImportEngine.ItemResult;
import nu.fgv.register.server.news.NewsCreateDto;
import nu.fgv.register.server.news.NewsService;
import nu.fgv.register.server.news.NewsUpdateDto;
import jakarta.persistence.EntityManager;
import nu.fgv.register.server.spexare.SpexareCreateDto;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.SpexareService;
import nu.fgv.register.server.spexare.SpexareUpdateDto;
import nu.fgv.register.server.spexare.activity.ActivityService;
import nu.fgv.register.server.spexare.activity.spex.SpexActivityService;
import nu.fgv.register.server.spexare.activity.task.TaskActivityService;
import nu.fgv.register.server.spexare.activity.task.actor.ActorCharacterService;
import nu.fgv.register.server.spexare.activity.task.actor.ActorCreateDto;
import nu.fgv.register.server.spexare.activity.task.actor.ActorService;
import nu.fgv.register.server.spexare.address.AddressCreateDto;
import nu.fgv.register.server.spexare.address.AddressService;
import nu.fgv.register.server.spexare.consent.ConsentCreateDto;
import nu.fgv.register.server.spexare.consent.ConsentService;
import nu.fgv.register.server.spexare.membership.MembershipCreateDto;
import nu.fgv.register.server.spexare.membership.MembershipService;
import nu.fgv.register.server.spexare.tagging.TaggingService;
import nu.fgv.register.server.spexare.toggle.ToggleCreateDto;
import nu.fgv.register.server.spexare.toggle.ToggleService;
import nu.fgv.register.server.sync.MedlemskapProperties;
import nu.fgv.register.server.tag.TagCreateDto;
import nu.fgv.register.server.tag.TagService;
import nu.fgv.register.server.tag.TagUpdateDto;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.user.UserRepository;
import nu.fgv.register.server.user.UserService;
import nu.fgv.register.server.user.state.StateRepository;
import nu.fgv.register.server.util.error.BadRequestException;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static nu.fgv.register.server.user.UserMapper.USER_MAPPER;

/**
 * The register's import records and how each is written (spexregister-spec, docs/migration.md, 3.1). Everything goes
 * through the register's own services, so validation, the encryption of personal identity numbers, the search index
 * and the audit work as for any other change. FGV memberships are refused by {@link MembershipService} as everywhere.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Service
public class SpexregisterImport {

    static final String TAG = "TAG";
    static final String SPEXARE = "SPEXARE";
    static final String PARTNER = "PARTNER";
    static final String ACTIVITY = "ACTIVITY";
    static final String NEWS = "NEWS";
    static final String USER = "USER";

    private final ImportEngine engine;
    private final ImportFileStore files;
    private final JdbcClient jdbc;
    private final MedlemskapProperties medlemskap;
    private final TagService tagService;
    private final SpexareService spexareService;
    private final AddressService addressService;
    private final ConsentService consentService;
    private final ToggleService toggleService;
    private final TaggingService taggingService;
    private final MembershipService membershipService;
    private final ActivityService activityService;
    private final SpexActivityService spexActivityService;
    private final TaskActivityService taskActivityService;
    private final ActorService actorService;
    private final ActorCharacterService actorCharacterService;
    private final NewsService newsService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final StateRepository stateRepository;
    private final SpexareRepository spexareRepository;
    private final EntityManager entityManager;
    private final List<Handler<?>> handlers;

    @SuppressWarnings("java:S107") // One collaborator per kind of record
    public SpexregisterImport(final ImportEngine engine, final ImportFileStore files, final JdbcClient jdbc,
                              final MedlemskapProperties medlemskap, final TagService tagService,
                              final SpexareService spexareService, final AddressService addressService,
                              final ConsentService consentService, final ToggleService toggleService,
                              final TaggingService taggingService, final MembershipService membershipService,
                              final ActivityService activityService, final SpexActivityService spexActivityService,
                              final TaskActivityService taskActivityService, final ActorService actorService,
                              final ActorCharacterService actorCharacterService, final NewsService newsService,
                              final UserRepository userRepository, final UserService userService,
                              final StateRepository stateRepository, final SpexareRepository spexareRepository,
                              final EntityManager entityManager) {
        this.engine = engine;
        this.files = files;
        this.jdbc = jdbc;
        this.medlemskap = medlemskap;
        this.tagService = tagService;
        this.spexareService = spexareService;
        this.addressService = addressService;
        this.consentService = consentService;
        this.toggleService = toggleService;
        this.taggingService = taggingService;
        this.membershipService = membershipService;
        this.activityService = activityService;
        this.spexActivityService = spexActivityService;
        this.taskActivityService = taskActivityService;
        this.actorService = actorService;
        this.actorCharacterService = actorCharacterService;
        this.newsService = newsService;
        this.userRepository = userRepository;
        this.userService = userService;
        this.stateRepository = stateRepository;
        this.spexareRepository = spexareRepository;
        this.entityManager = entityManager;
        this.handlers = List.of(new TagHandler(), new SpexareHandler(), new PartnerHandler(), new ActivityHandler(), new NewsHandler(), new UserHandler());
    }

    /** Imports a batch, kind by kind in the order records refer to each other. */
    public List<ItemResult> run(final ImportRecords.Batch input, final boolean dryRun) {
        if (input.size() > ImportEngine.MAX_RECORDS) {
            throw new BadRequestException("import.batch.tooLarge");
        }

        final Batch batch = new Batch(dryRun);
        final String importedBy = ImportEngine.caller();
        final List<ItemResult> results = new ArrayList<>();

        results.addAll(engine.run(new TagHandler(), input.tags(), batch, importedBy));
        results.addAll(engine.run(new SpexareHandler(), input.spexare(), batch, importedBy));
        results.addAll(engine.run(new PartnerHandler(), input.partners(), batch, importedBy));
        results.addAll(engine.run(new ActivityHandler(), input.activities(), batch, importedBy));
        results.addAll(engine.run(new NewsHandler(), input.news(), batch, importedBy));
        results.addAll(engine.run(new UserHandler(), input.users(), batch, importedBy));
        return results;
    }

    public int forget(final String sourcePrefix) {
        if (sourcePrefix.isBlank()) {
            throw new BadRequestException("import.forget.prefix");
        }
        return engine.forget(sourcePrefix, handlers);
    }

    /**
     * The spexare a source key made. In a dry run, one an earlier record of the call would have made does not exist,
     * so null is returned and the record checks what it can without it.
     */
    private @Nullable UUID spexare(final String sourceKey, final Batch batch) {
        final UUID id = engine.recordOf(SPEXARE, sourceKey);

        if (id != null) {
            return id;
        }
        if (batch.dryRun() && batch.planned(sourceKey)) {
            return null;
        }
        throw new BadRequestException("import.reference.spexare");
    }

    private static UUID placeholder(final String sourceKey) {
        return UUID.nameUUIDFromBytes(sourceKey.getBytes(StandardCharsets.UTF_8));
    }

    private @Nullable Long fingerprint(final String sql, final UUID id) {
        final List<String> rows = jdbc.sql(sql).param("id", id.toString()).query(String.class).list();

        return rows.isEmpty() ? null : ImportEngine.fingerprint(String.join("\n", rows));
    }

    private static String text(final @Nullable String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private final class TagHandler implements Handler<ImportRecords.Tag> {

        @Override
        public String kind() {
            return TAG;
        }

        @Override
        public String sourceKey(final ImportRecords.Tag record) {
            return record.sourceKey();
        }

        @Override
        public UUID apply(final ImportRecords.Tag record, final @Nullable UUID existing, final Batch batch) {
            if (existing == null) {
                return tagService.create(TagCreateDto.builder().name(record.name().strip()).build()).getId();
            }
            return tagService.partialUpdate(TagUpdateDto.builder().id(existing).name(record.name().strip()).build()).getId();
        }

        @Override
        public @Nullable Long version(final UUID id) {
            return fingerprint("SELECT name FROM tag WHERE uuid = :id", id);
        }

        @Override
        public boolean forget(final UUID id) {
            tagService.deleteById(id);
            return true;
        }
    }

    /**
     * A spexare with its addresses, consents, toggles, taggings and external memberships; an update replaces them. The
     * partner is a record of its own and is not part of the fingerprint.
     */
    private final class SpexareHandler implements Handler<ImportRecords.Spexare> {

        @Override
        public String kind() {
            return SPEXARE;
        }

        @Override
        public String sourceKey(final ImportRecords.Spexare record) {
            return record.sourceKey();
        }

        @Override
        public UUID apply(final ImportRecords.Spexare r, final @Nullable UUID existing, final Batch batch) {
            final UUID id;

            if (existing == null) {
                id = spexareService.create(SpexareCreateDto.builder()
                        .firstName(r.firstName().strip()).lastName(r.lastName().strip()).nickName(text(r.nickName()))
                        .socialSecurityNumber(text(r.socialSecurityNumber())).deceased(r.deceased()).published(r.published())
                        .graduation(text(r.graduation())).comment(text(r.comment()))
                        .build()).getId();
            } else {
                removeParts(existing);
                // What was removed must not linger in the spexare loaded before
                entityManager.flush();
                entityManager.clear();
                id = spexareService.partialUpdate(SpexareUpdateDto.builder().id(existing)
                        .firstName(r.firstName().strip()).lastName(r.lastName().strip()).nickName(text(r.nickName()))
                        .socialSecurityNumber(text(r.socialSecurityNumber())).deceased(r.deceased()).published(r.published())
                        .graduation(text(r.graduation())).comment(text(r.comment()))
                        .build()).getId();
            }
            if (r.image() != null) {
                spexareService.saveImage(id, files.require(r.image().sha256()));
            }
            for (final ImportRecords.Address a : Objects.requireNonNullElse(r.addresses(), List.<ImportRecords.Address>of())) {
                addressService.create(id, a.type(), AddressCreateDto.builder()
                        .streetAddress(text(a.streetAddress())).postalCode(text(a.postalCode())).city(text(a.city()))
                        .country(text(a.country())).phone(text(a.phone())).phoneMobile(text(a.phoneMobile()))
                        .emailAddress(text(a.emailAddress()))
                        .build());
            }
            for (final ImportRecords.TypeValue c : Objects.requireNonNullElse(r.consents(), List.<ImportRecords.TypeValue>of())) {
                consentService.create(id, c.type(), ConsentCreateDto.builder().value(c.value()).build());
            }
            for (final ImportRecords.TypeValue t : Objects.requireNonNullElse(r.toggles(), List.<ImportRecords.TypeValue>of())) {
                toggleService.create(id, t.type(), ToggleCreateDto.builder().value(t.value()).build());
            }
            for (final String tag : Objects.requireNonNullElse(r.tags(), List.<String>of())) {
                final UUID tagId = engine.recordOf(TAG, tag);

                if (tagId != null) {
                    taggingService.create(id, tagId);
                } else if (!(batch.dryRun() && batch.planned(tag))) {
                    throw new BadRequestException("import.reference.tag");
                }
            }
            for (final ImportRecords.Membership m : Objects.requireNonNullElse(r.memberships(), List.<ImportRecords.Membership>of())) {
                membershipService.create(id, m.type(), MembershipCreateDto.builder().year(m.year().strip()).build());
            }
            return id;
        }

        private void removeParts(final UUID id) {
            parts("address", id).forEach(p -> addressService.deleteById(id, p[1], UUID.fromString(p[0])));
            parts("consent", id).forEach(p -> consentService.deleteById(id, p[1], UUID.fromString(p[0])));
            parts("toggle", id).forEach(p -> toggleService.deleteById(id, p[1], UUID.fromString(p[0])));
            parts("membership", id).stream()
                    .filter(p -> !medlemskap.isInternal(p[1]))
                    .forEach(p -> membershipService.deleteById(id, p[1], UUID.fromString(p[0])));
            jdbc.sql("SELECT t.uuid FROM tagging g JOIN tag t ON t.id = g.tag_id JOIN spexare s ON s.id = g.spexare_id WHERE s.uuid = :id")
                    .param("id", id.toString()).query(String.class).list()
                    .forEach(tag -> taggingService.deleteById(id, UUID.fromString(tag)));
        }

        private List<String[]> parts(final String table, final UUID id) {
            return jdbc.sql("SELECT x.uuid, x.type_id FROM " + table + " x JOIN spexare s ON s.id = x.spexare_id WHERE s.uuid = :id")
                    .param("id", id.toString())
                    .query((rs, _) -> new String[]{rs.getString(1), rs.getString(2)})
                    .list();
        }

        @Override
        public @Nullable Long version(final UUID id) {
            final Long own = fingerprint("""
                    SELECT CONCAT_WS('|', first_name, last_name, nick_name, deceased, published, graduation, comment, image_id)
                    FROM spexare WHERE uuid = :id""", id);

            if (own == null) {
                return null;
            }

            final List<String> rows = new ArrayList<>();

            rows.add(Long.toString(own));
            // In clear: the stored value is encrypted anew, with a new IV, every time the spexare is saved
            rows.add("ssn:" + spexareRepository.findByUuid(id).map(s -> Objects.requireNonNullElse(s.getSocialSecurityNumber(), "")).orElse(""));
            rows.addAll(jdbc.sql("""
                    SELECT CONCAT_WS(',', 'a', x.type_id, x.street_address, x.postal_code, x.city, x.country, x.phone,
                                     x.phone_mobile, x.email_address)
                    FROM address x JOIN spexare s ON s.id = x.spexare_id WHERE s.uuid = :id
                    UNION ALL SELECT CONCAT_WS(',', 'c', x.type_id, x.value) FROM consent x JOIN spexare s ON s.id = x.spexare_id WHERE s.uuid = :id
                    UNION ALL SELECT CONCAT_WS(',', 't', x.type_id, x.value) FROM toggle x JOIN spexare s ON s.id = x.spexare_id WHERE s.uuid = :id
                    UNION ALL SELECT CONCAT_WS(',', 'g', x.tag_id) FROM tagging x JOIN spexare s ON s.id = x.spexare_id WHERE s.uuid = :id
                    UNION ALL SELECT CONCAT_WS(',', 'm', x.type_id, x.year) FROM membership x JOIN spexare s ON s.id = x.spexare_id
                              WHERE s.uuid = :id AND x.type_id NOT IN (:internal)""")
                    .param("id", id.toString())
                    .param("internal", medlemskap.internalTypes())
                    .query(String.class).list().stream().sorted().toList());
            return ImportEngine.fingerprint(String.join("\n", rows));
        }

        @Override
        public boolean forget(final UUID id) {
            spexareService.deleteById(id);
            return true;
        }
    }

    /** Two spexare who are each other's partners; the record's id is the first one's. */
    private final class PartnerHandler implements Handler<ImportRecords.Partner> {

        @Override
        public String kind() {
            return PARTNER;
        }

        @Override
        public String sourceKey(final ImportRecords.Partner record) {
            return record.sourceKey();
        }

        @Override
        public UUID apply(final ImportRecords.Partner record, final @Nullable UUID existing, final Batch batch) {
            final UUID spexare = spexare(record.spexare(), batch);
            final UUID partner = spexare(record.partner(), batch);

            if (spexare == null || partner == null) {
                return placeholder(record.sourceKey());
            }
            spexareService.addPartner(spexare, partner);
            return spexare;
        }

        @Override
        public @Nullable Long version(final UUID id) {
            return fingerprint("""
                    SELECT COALESCE(p.uuid, '') FROM spexare s LEFT JOIN spexare p ON p.id = s.partner_id WHERE s.uuid = :id""", id);
        }

        @Override
        public boolean forget(final UUID id) {
            spexareService.removePartner(id);
            return true;
        }
    }

    /**
     * One task of one spexare in one spex, or the spex alone. Records of the same spexare and spex share the activity,
     * and records of the same task share the task activity, so two sources never give a spexare a spex or a task twice.
     * Actors are replaced when the record lists them and left alone when it does not.
     */
    private final class ActivityHandler implements Handler<ImportRecords.Activity> {

        @Override
        public String kind() {
            return ACTIVITY;
        }

        @Override
        public String sourceKey(final ImportRecords.Activity record) {
            return record.sourceKey();
        }

        @Override
        public UUID apply(final ImportRecords.Activity r, final @Nullable UUID existing, final Batch batch) {
            final UUID spexare = spexare(r.spexare(), batch);

            if (spexare == null) {
                return placeholder(r.sourceKey());
            }

            final UUID activity = jdbc.sql("""
                            SELECT a.uuid FROM activity a
                            JOIN spexare p ON p.id = a.spexare_id
                            JOIN spex_activity sa ON sa.activity_id = a.id
                            JOIN spex s ON s.id = sa.spex_id
                            WHERE p.uuid = :spexare AND s.uuid = :spex
                            ORDER BY a.id LIMIT 1""")
                    .param("spexare", spexare.toString()).param("spex", r.spexId().toString())
                    .query(String.class).optional().map(UUID::fromString)
                    .orElseGet(() -> {
                        final UUID created = activityService.create(spexare).getId();

                        spexActivityService.create(spexare, created, r.spexId());
                        return created;
                    });
            final UUID result;

            if (r.taskId() == null) {
                result = activity;
            } else {
                result = jdbc.sql("""
                                SELECT ta.uuid FROM task_activity ta
                                JOIN activity a ON a.id = ta.activity_id
                                JOIN task t ON t.id = ta.task_id
                                WHERE a.uuid = :activity AND t.uuid = :task
                                ORDER BY ta.id LIMIT 1""")
                        .param("activity", activity.toString()).param("task", r.taskId().toString())
                        .query(String.class).optional().map(UUID::fromString)
                        .orElseGet(() -> taskActivityService.create(spexare, activity, r.taskId()).getId());
                if (r.actors() != null) {
                    actors(result).forEach(a -> actorService.deleteById(spexare, activity, result, a[1], UUID.fromString(a[0])));
                    for (final ImportRecords.Actor actor : r.actors()) {
                        final UUID created = actorService.create(spexare, activity, result, actor.vocal(),
                                ActorCreateDto.builder().role(text(actor.role())).build()).getId();

                        if (actor.characterId() != null) {
                            actorCharacterService.setCharacter(created, actor.characterId());
                        }
                    }
                }
            }
            // A record that now points elsewhere gives up what it made before
            if (existing != null && !existing.equals(result)) {
                forget(existing);
            }
            return result;
        }

        private List<String[]> actors(final UUID taskActivity) {
            return jdbc.sql("SELECT ac.uuid, ac.vocal_id FROM actor ac JOIN task_activity ta ON ta.id = ac.task_activity_id WHERE ta.uuid = :id")
                    .param("id", taskActivity.toString())
                    .query((rs, _) -> new String[]{rs.getString(1), rs.getString(2)})
                    .list();
        }

        @Override
        public @Nullable Long version(final UUID id) {
            final Long task = fingerprint("""
                    SELECT CONCAT_WS('|', t.uuid, COALESCE(GROUP_CONCAT(CONCAT_WS(',', ac.vocal_id, ac.role, ch.uuid) ORDER BY ac.id), ''))
                    FROM task_activity ta
                    JOIN task t ON t.id = ta.task_id
                    LEFT JOIN actor ac ON ac.task_activity_id = ta.id
                    LEFT JOIN spex_character ch ON ch.id = ac.character_id
                    WHERE ta.uuid = :id
                    GROUP BY ta.id, t.uuid""", id);

            return task != null ? task : fingerprint("""
                    SELECT s.uuid FROM activity a
                    JOIN spex_activity sa ON sa.activity_id = a.id
                    JOIN spex s ON s.id = sa.spex_id
                    WHERE a.uuid = :id""", id);
        }

        /** Removes the task activity, and its activity when nothing is left in it; or an activity with no tasks. */
        @Override
        public boolean forget(final UUID id) {
            final List<String[]> taskActivity = jdbc.sql("""
                            SELECT a.uuid, p.uuid FROM task_activity ta
                            JOIN activity a ON a.id = ta.activity_id
                            JOIN spexare p ON p.id = a.spexare_id
                            WHERE ta.uuid = :id""")
                    .param("id", id.toString())
                    .query((rs, _) -> new String[]{rs.getString(1), rs.getString(2)})
                    .list();
            final UUID activity;
            final UUID spexare;

            if (!taskActivity.isEmpty()) {
                activity = UUID.fromString(taskActivity.getFirst()[0]);
                spexare = UUID.fromString(taskActivity.getFirst()[1]);
                taskActivityService.deleteById(spexare, activity, id);
            } else {
                final List<String> owner = jdbc.sql("SELECT p.uuid FROM activity a JOIN spexare p ON p.id = a.spexare_id WHERE a.uuid = :id")
                        .param("id", id.toString()).query(String.class).list();

                if (owner.isEmpty()) {
                    return false;
                }
                activity = id;
                spexare = UUID.fromString(owner.getFirst());
            }

            final int tasksLeft = jdbc.sql("SELECT COUNT(*) FROM task_activity ta JOIN activity a ON a.id = ta.activity_id WHERE a.uuid = :id")
                    .param("id", activity.toString()).query(Integer.class).single();

            if (tasksLeft == 0) {
                activityService.deleteById(spexare, activity);
            }
            return true;
        }
    }

    private final class NewsHandler implements Handler<ImportRecords.News> {

        @Override
        public String kind() {
            return NEWS;
        }

        @Override
        public String sourceKey(final ImportRecords.News record) {
            return record.sourceKey();
        }

        @Override
        public UUID apply(final ImportRecords.News r, final @Nullable UUID existing, final Batch batch) {
            if (existing == null) {
                return newsService.create(NewsCreateDto.builder().subject(r.subject().strip()).text(r.text())
                        .visibleFrom(r.visibleFrom()).visibleTo(r.visibleTo()).build()).getId();
            }
            return newsService.partialUpdate(NewsUpdateDto.builder().id(existing).subject(r.subject().strip()).text(r.text())
                    .visibleFrom(r.visibleFrom()).visibleTo(r.visibleTo()).build()).getId();
        }

        @Override
        public @Nullable Long version(final UUID id) {
            return fingerprint("SELECT CONCAT_WS('|', subject, text, visible_from, visible_to) FROM news WHERE uuid = :id", id);
        }

        @Override
        public boolean forget(final UUID id) {
            newsService.deleteById(id);
            return true;
        }
    }

    /**
     * A user: the Keycloak account (made by the migration) known to the register, with its state and spexare. The
     * account is not touched; its roles are Keycloak's. A user the Keycloak sync has added already is taken over.
     */
    private final class UserHandler implements Handler<ImportRecords.User> {

        @Override
        public String kind() {
            return USER;
        }

        @Override
        public String sourceKey(final ImportRecords.User record) {
            return record.sourceKey();
        }

        @Override
        public UUID apply(final ImportRecords.User r, final @Nullable UUID existing, final Batch batch) {
            final var state = stateRepository.findById(r.state())
                    .orElseThrow(() -> new BadRequestException("import.user.state"));
            final User user = userRepository.findByExternalId(r.externalId().strip())
                    .orElseGet(() -> USER_MAPPER.toModel(r.externalId().strip(), state));

            user.setState(state);

            final User saved = userRepository.save(user);

            if (r.spexare() != null) {
                final UUID spexare = spexare(r.spexare(), batch);

                if (spexare != null && (saved.getSpexare() == null || !spexare.equals(saved.getSpexare().getUuid()))) {
                    userService.addSpexare(saved.getUuid(), spexare);
                }
            }
            return saved.getUuid();
        }

        @Override
        public @Nullable Long version(final UUID id) {
            return fingerprint("""
                    SELECT CONCAT_WS('|', u.external_id, u.state_id, COALESCE(s.uuid, ''))
                    FROM user u LEFT JOIN spexare s ON s.id = u.spexare_id WHERE u.uuid = :id""", id);
        }

        /** Removes the register's user; the Keycloak account stays, and the sync adds it again if it has a role. */
        @Override
        public boolean forget(final UUID id) {
            userRepository.findByUuid(id).ifPresent(userRepository::delete);
            return true;
        }
    }
}
