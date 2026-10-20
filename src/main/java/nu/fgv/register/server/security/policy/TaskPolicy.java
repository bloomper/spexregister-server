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

import nu.fgv.register.server.task.Task;
import org.springframework.stereotype.Component;

import static nu.fgv.register.server.security.policy.Caller.ADMIN;
import static nu.fgv.register.server.security.policy.Caller.EDITOR;
import static nu.fgv.register.server.security.policy.Caller.REVIVAL;
import static nu.fgv.register.server.security.policy.Caller.USER;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
public class TaskPolicy extends ReferenceDataPolicy<Task> {

    public TaskPolicy() {
        super(Task.class, RoleRules.builder()
                .read(USER, EDITOR, ADMIN, REVIVAL)
                .write(EDITOR)
                .administer(ADMIN)
                .build());
    }
}
