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

package nu.fgv.register.server.util.data;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static org.hibernate.engine.internal.Versioning.seed;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
public class R__SeedAuditData extends BaseJavaMigration {
    @Value("${spexregister.data.seed-audit-data:false}")
    private final boolean seedAuditData;
    private final Random rnd = new SecureRandom();

    public R__SeedAuditData(@Value("${spexregister.data.seed-audit-data:false}") final boolean seedAuditData) {
        this.seedAuditData = seedAuditData;
    }

    @Override
    public Integer getChecksum() {
        return rnd.nextInt();
    }

    @Override
    public void migrate(final Context context) {
        if (seedAuditData) {
            final JdbcClient jdbcClient = JdbcClient.create(new SingleConnectionDataSource(context.getConnection(), true));

            purgeAllRelevantTables(jdbcClient);
            seed(jdbcClient);
        }
    }

    private void purgeAllRelevantTables(final JdbcClient jdbcClient) {
        final List<String> tables = List.of(
                "actor_audit",
                "task_activity_audit",
                "spex_activity_audit",
                "activity_audit",
                "address_audit",
                "consent_audit",
                "membership_audit",
                "tagging_audit",
                "toggle_audit",
                "spexare_audit",
                "task_audit",
                "task_category_audit",
                "spex_audit",
                "spex_details_audit",
                "spex_category_audit",
                "news_audit",
                "tag_audit",
                "user_audit",
                "state_audit",
                "type_audit",
                "revchanges",
                "revinfo"
        );

        tables.forEach(table ->
                jdbcClient
                        .sql("TRUNCATE TABLE %s".formatted(table))
                        .update()
        );
    }

    private void seed(final JdbcClient jdbcClient) {
        final KeyHolder revIdHolder = new GeneratedKeyHolder();
        jdbcClient
                .sql("INSERT INTO revinfo (modified_by, modified_at) VALUES ('system', UNIX_TIMESTAMP() * 1000)")
                .update(revIdHolder);

        final long revId = Objects.requireNonNull(revIdHolder.getKey()).longValue();

        insertIntoRevChanges(jdbcClient, revId, "nu.fgv.register.server.settings.Type");
        jdbcClient
                .sql("INSERT INTO type_audit (id, type, labels, rev, revend, revtype, revend_tstmp) SELECT id, type, labels, ?, NULL, 0, NULL FROM type")
                .param(revId)
                .update();

        insertIntoRevChanges(jdbcClient, revId, "nu.fgv.register.server.user.state.State");
        jdbcClient
                .sql("INSERT INTO state_audit (id, labels, initial, enabled, rev, revend, revtype, revend_tstmp) SELECT id, labels, initial, enabled, ?, NULL, 0, NULL FROM state")
                .param(revId)
                .update();

        insertIntoRevChanges(jdbcClient, revId, "nu.fgv.register.server.user.User");
        jdbcClient
                .sql("INSERT INTO user_audit (id, external_id, state_id, spexare_id, rev, revend, revtype, revend_tstmp) SELECT id, external_id, state_id, spexare_id, ?, NULL, 0, NULL FROM user")
                .param(revId)
                .update();

        insertIntoRevChanges(jdbcClient, revId, "nu.fgv.register.server.tag.Tag");
        jdbcClient
                .sql("INSERT INTO tag_audit (id, name, rev, revend, revtype, revend_tstmp) SELECT id, name, ?, NULL, 0, NULL FROM tag")
                .param(revId)
                .update();

        insertIntoRevChanges(jdbcClient, revId, "nu.fgv.register.server.news.News");
        jdbcClient
                .sql("INSERT INTO news_audit (id, visible_from, visible_to, subject, text, published, rev, revend, revtype, revend_tstmp) SELECT id, visible_from, visible_to, subject, text, published, ?, NULL, 0, NULL FROM news")
                .param(revId)
                .update();

        insertIntoRevChanges(jdbcClient, revId, "nu.fgv.register.server.spex.category.SpexCategory");
        jdbcClient
                .sql("INSERT INTO spex_category_audit (id, name, first_year, logo, logo_content_type, rev, revend, revtype, revend_tstmp) SELECT id, name, first_year, logo, logo_content_type, ?, NULL, 0, NULL FROM spex_category")
                .param(revId)
                .update();

        insertIntoRevChanges(jdbcClient, revId, "nu.fgv.register.server.spex.SpexDetails");
        jdbcClient
                .sql("INSERT INTO spex_details_audit (id, title, poster, poster_content_type, category_id, rev, revend, revtype, revend_tstmp) SELECT id, title, poster, poster_content_type, category_id, ?, NULL, 0, NULL FROM spex_details")
                .param(revId)
                .update();

        insertIntoRevChanges(jdbcClient, revId, "nu.fgv.register.server.spex.Spex");
        jdbcClient
                .sql("INSERT INTO spex_audit (id, year, parent_id, details_id, rev, revend, revtype, revend_tstmp) SELECT id, year, parent_id, details_id, ?, NULL, 0, NULL FROM spex")
                .param(revId)
                .update();

        insertIntoRevChanges(jdbcClient, revId, "nu.fgv.register.server.task.category.TaskCategory");
        jdbcClient
                .sql("INSERT INTO task_category_audit (id, name, actor_present, rev, revend, revtype, revend_tstmp) SELECT id, name, actor_present, ?, NULL, 0, NULL FROM task_category")
                .param(revId)
                .update();

        insertIntoRevChanges(jdbcClient, revId, "nu.fgv.register.server.task.Task");
        jdbcClient
                .sql("INSERT INTO task_audit (id, name, category_id, rev, revend, revtype, revend_tstmp) SELECT id, name, category_id, ?, NULL, 0, NULL FROM task")
                .param(revId)
                .update();

        insertIntoRevChanges(jdbcClient, revId, "nu.fgv.register.server.spexare.Spexare");
        jdbcClient
                .sql("INSERT INTO spexare_audit (id, first_name, last_name, nick_name, social_security_number, deceased, published, graduation, comment, image, image_content_type, partner_id, rev, revend, revtype, revend_tstmp) SELECT id, first_name, last_name, nick_name, social_security_number, deceased, published, graduation, comment, image, image_content_type, partner_id, ?, NULL, 0, NULL FROM spexare")
                .param(revId)
                .update();

        insertIntoRevChanges(jdbcClient, revId, "nu.fgv.register.server.spexare.toggle.Toggle");
        jdbcClient
                .sql("INSERT INTO toggle_audit (id, value, type_id, spexare_id, rev, revend, revtype, revend_tstmp) SELECT id, value, type_id, spexare_id, ?, NULL, 0, NULL FROM toggle")
                .param(revId)
                .update();

        insertIntoRevChanges(jdbcClient, revId, "nu.fgv.register.server.spexare.tagging.Tagging");
        jdbcClient
                .sql("INSERT INTO tagging_audit (tag_id, spexare_id, rev, revend, revtype, revend_tstmp) SELECT tag_id, spexare_id, ?, NULL, 0, NULL FROM tagging")
                .param(revId)
                .update();

        insertIntoRevChanges(jdbcClient, revId, "nu.fgv.register.server.spexare.membership.Membership");
        jdbcClient
                .sql("INSERT INTO membership_audit (id, year, type_id, spexare_id, rev, revend, revtype, revend_tstmp) SELECT id, year, type_id, spexare_id, ?, NULL, 0, NULL FROM membership")
                .param(revId)
                .update();

        insertIntoRevChanges(jdbcClient, revId, "nu.fgv.register.server.spexare.consent.Consent");
        jdbcClient
                .sql("INSERT INTO consent_audit (id, value, type_id, spexare_id, rev, revend, revtype, revend_tstmp) SELECT id, value, type_id, spexare_id, ?, NULL, 0, NULL FROM consent")
                .param(revId)
                .update();

        insertIntoRevChanges(jdbcClient, revId, "nu.fgv.register.server.spexare.address.Address");
        jdbcClient
                .sql("INSERT INTO address_audit (id, street_address, postal_code, city, country, phone, phone_mobile, email_address, type_id, spexare_id, rev, revend, revtype, revend_tstmp) SELECT id, street_address, postal_code, city, country, phone, phone_mobile, email_address, type_id, spexare_id, ?, NULL, 0, NULL FROM address")
                .param(revId)
                .update();

        insertIntoRevChanges(jdbcClient, revId, "nu.fgv.register.server.spexare.activity.Activity");
        jdbcClient
                .sql("INSERT INTO activity_audit (id, spexare_id, rev, revend, revtype, revend_tstmp) SELECT id, spexare_id, ?, NULL, 0, NULL FROM activity")
                .param(revId)
                .update();

        insertIntoRevChanges(jdbcClient, revId, "nu.fgv.register.server.spexare.activity.spex.SpexActivity");
        jdbcClient
                .sql("INSERT INTO spex_activity_audit (id, activity_id, spex_id, rev, revend, revtype, revend_tstmp) SELECT id, activity_id, spex_id, ?, NULL, 0, NULL FROM spex_activity")
                .param(revId)
                .update();

        insertIntoRevChanges(jdbcClient, revId, "nu.fgv.register.server.spexare.activity.task.TaskActivity");
        jdbcClient
                .sql("INSERT INTO task_activity_audit (id, activity_id, task_id, rev, revend, revtype, revend_tstmp) SELECT id, activity_id, task_id, ?, NULL, 0, NULL FROM task_activity")
                .param(revId)
                .update();

        insertIntoRevChanges(jdbcClient, revId, "nu.fgv.register.server.spexare.activity.task.actor.Actor");
        jdbcClient
                .sql("INSERT INTO actor_audit (id, role, vocal_id, task_activity_id, rev, revend, revtype, revend_tstmp) SELECT id, role, vocal_id, task_activity_id, ?, NULL, 0, NULL FROM actor")
                .param(revId)
                .update();
    }

    private void insertIntoRevChanges(final JdbcClient jdbcClient, final long revId, final String entityName) {
        jdbcClient
                .sql("INSERT INTO revchanges (rev, entityname) VALUES (?, ?)")
                .param(revId)
                .param(entityName)
                .update();
    }
}
