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

import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.category.SpexCategory;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.task.Task;
import nu.fgv.register.server.task.category.TaskCategory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;
import java.util.Set;

import static nu.fgv.register.server.security.policy.PolicyTestSupport.caller;
import static nu.fgv.register.server.security.policy.PolicyTestSupport.noGrants;
import static nu.fgv.register.server.security.policy.PolicyTestSupport.permissions;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every role against every permission on reference data (data-model.md, Policies).
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
class ReferenceDataPolicyTest {
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final Map<String, AccessPolicy> POLICIES = Map.of(
            "SPEX", new SpexPolicy(noGrants()),
            "SPEX_CATEGORY", new SpexCategoryPolicy(),
            "TASK", new TaskPolicy(),
            "TASK_CATEGORY", new TaskCategoryPolicy(),
            "TAG", new TagPolicy()
    );
    private static final Map<String, Object> RECORDS = Map.of(
            "SPEX", new Spex(),
            "SPEX_CATEGORY", new SpexCategory(),
            "TASK", new Task(),
            "TASK_CATEGORY", new TaskCategory(),
            "TAG", new Tag()
    );

    @ParameterizedTest(name = "{0} {1} holds {2}")
    @CsvSource({
            // type,          role,    READ/WRITE/DELETE/ADMINISTRATION
            "SPEX,            ADMIN,   RWDA",
            "SPEX,            EDITOR,  RW--",
            "SPEX,            USER,    R---",
            "SPEX,            REVIVAL, R---",
            "SPEX,            NONE,    ----",
            "SPEX_CATEGORY,   ADMIN,   RWDA",
            "SPEX_CATEGORY,   EDITOR,  R---",
            "SPEX_CATEGORY,   USER,    R---",
            "SPEX_CATEGORY,   REVIVAL, ----",
            "SPEX_CATEGORY,   NONE,    ----",
            "TASK,            ADMIN,   RWDA",
            "TASK,            EDITOR,  RW--",
            "TASK,            USER,    R---",
            "TASK,            REVIVAL, R---",
            "TASK,            NONE,    ----",
            "TASK_CATEGORY,   ADMIN,   RWDA",
            "TASK_CATEGORY,   EDITOR,  R---",
            "TASK_CATEGORY,   USER,    R---",
            "TASK_CATEGORY,   REVIVAL, R---",
            "TASK_CATEGORY,   NONE,    ----",
            "TAG,             ADMIN,   RWDA",
            "TAG,             EDITOR,  RWD-",
            "TAG,             USER,    R---",
            "TAG,             REVIVAL, ----",
            "TAG,             NONE,    ----"
    })
    @SuppressWarnings("unchecked")
    void should_grant_exactly_the_permissions_in_the_table(final String type, final String role, final String flags) {
        final Set<Permission> expected = permissions(flags);

        for (final Permission permission : Permission.values()) {
            assertThat(POLICIES.get(type).permits(caller(role), RECORDS.get(type), permission))
                    .as("%s %s %s", type, role, permission)
                    .isEqualTo(expected.contains(permission));
        }
    }
}
