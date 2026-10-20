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

package nu.fgv.register.server.security.policy;

import nu.fgv.register.server.news.News;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.user.User;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static nu.fgv.register.server.security.policy.PolicyTestSupport.OWN_SUBJECT;
import static nu.fgv.register.server.security.policy.PolicyTestSupport.caller;
import static nu.fgv.register.server.security.policy.PolicyTestSupport.noGrants;
import static nu.fgv.register.server.security.policy.PolicyTestSupport.permissions;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every role against every permission on published and unpublished spexare and news (data-model.md, Policies).
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
class SpexareAndNewsPolicyTest {
    private final SpexarePolicy spexarePolicy = new SpexarePolicy(noGrants());
    private final NewsPolicy newsPolicy = new NewsPolicy();

    @ParameterizedTest(name = "spexare, published={1}: {0} holds {2}")
    @CsvSource({
            // role,   published, READ/WRITE/DELETE/ADMINISTRATION
            "ADMIN,    true,      RWDA",
            "ADMIN,    false,     RWDA",
            "EDITOR,   true,      RW--",
            "EDITOR,   false,     RW--",
            "USER,     true,      R---",
            "USER,     false,     ----",
            "REVIVAL,  true,      ----",
            "REVIVAL,  false,     ----",
            "NONE,     true,      ----"
    })
    void should_decide_spexare(final String role, final boolean published, final String flags) {
        final Spexare spexare = new Spexare();
        spexare.setPublished(published);
        final Set<Permission> expected = permissions(flags);

        for (final Permission permission : Permission.values()) {
            assertThat(spexarePolicy.permits(caller(role), spexare, permission))
                    .as("%s %s published=%s", role, permission, published)
                    .isEqualTo(expected.contains(permission));
        }
    }

    @ParameterizedTest(name = "spexare, published={1}, relation={2}: {0} holds {3}")
    @CsvSource({
            // role,   published, relation, READ/WRITE/DELETE/ADMINISTRATION
            "USER,     false,     OWN,      RW--",
            "USER,     true,      OWN,      RW--",
            "USER,     false,     PARTNER,  RW--",
            "USER,     false,     OTHER,    ----",
            "EDITOR,   false,     OWN,      RW--",
            "ADMIN,    false,     OWN,      RWDA"
    })
    void should_let_a_user_read_and_change_their_own_and_their_partners_spexare(final String role,
                                                                              final boolean published,
                                                                              final String relation,
                                                                              final String flags) {
        final Spexare spexare = new Spexare();
        spexare.setPublished(published);
        switch (relation) {
            case "OWN" -> spexare.setUser(user(OWN_SUBJECT));
            case "PARTNER" -> {
                final Spexare partner = new Spexare();
                partner.setUser(user(OWN_SUBJECT));
                spexare.setPartner(partner);
                spexare.setUser(user("partner-subject"));
            }
            default -> spexare.setUser(user("someone-else"));
        }
        final Set<Permission> expected = permissions(flags);

        for (final Permission permission : Permission.values()) {
            assertThat(spexarePolicy.permits(caller(role), spexare, permission))
                    .as("%s %s %s published=%s", role, relation, permission, published)
                    .isEqualTo(expected.contains(permission));
        }
    }

    private static User user(final String subject) {
        final User user = new User();
        user.setExternalId(subject);
        return user;
    }

    @ParameterizedTest(name = "news, published={1}: {0} holds {2}")
    @CsvSource({
            "ADMIN,    true,      RWDA",
            "ADMIN,    false,     RWDA",
            "EDITOR,   true,      RWD-",
            "EDITOR,   false,     RWD-",
            "USER,     true,      R---",
            "USER,     false,     ----",
            "REVIVAL,  true,      ----",
            "NONE,     true,      ----"
    })
    void should_decide_news(final String role, final boolean published, final String flags) {
        final News news = new News();
        news.setPublished(published);
        final Set<Permission> expected = permissions(flags);

        for (final Permission permission : Permission.values()) {
            assertThat(newsPolicy.permits(caller(role), news, permission))
                    .as("%s %s published=%s", role, permission, published)
                    .isEqualTo(expected.contains(permission));
        }
    }
}
