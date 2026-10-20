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
import nu.fgv.register.server.news.News_;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import static nu.fgv.register.server.security.policy.Caller.ADMIN;
import static nu.fgv.register.server.security.policy.Caller.EDITOR;
import static nu.fgv.register.server.security.policy.Caller.USER;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
public class NewsPolicy implements AccessPolicy<News> {

    private static final RoleRules RULES = RoleRules.builder()
            .read(EDITOR, ADMIN)
            .write(EDITOR)
            .delete(EDITOR)
            .administer(ADMIN)
            .build();

    @Override
    public Class<News> type() {
        return News.class;
    }

    @Override
    public Specification<News> permitted(final Caller caller, final Permission permission) {
        if (RULES.allows(caller, permission)) {
            return RoleRules.all();
        }
        if (permission == Permission.READ && caller.hasRole(USER)) {
            return isPublished();
        }

        return RoleRules.none();
    }

    @Override
    public boolean permits(final Caller caller, final News news, final Permission permission) {
        return RULES.allows(caller, permission)
                || (permission == Permission.READ && caller.hasRole(USER) && Boolean.TRUE.equals(news.getPublished()));
    }

    private static Specification<News> isPublished() {
        return (root, _, criteriaBuilder) -> criteriaBuilder.isTrue(root.get(News_.published));
    }
}
