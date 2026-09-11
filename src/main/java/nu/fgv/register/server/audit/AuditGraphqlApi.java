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

package nu.fgv.register.server.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.security.RequiresAdmin;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.query.ScrollSubrange;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;

import static nu.fgv.register.server.util.graphql.GraphqlUtil.extractScrollRequest;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class AuditGraphqlApi {

    private final AuditService service;

    @QueryMapping("revisions")
    @RequiresAdminOrEditorOrUser
    public List<RevisionDto> revisions(@Argument final AuditedType type, @Argument final String id) {
        return service.findRevisions(type, id);
    }

    @QueryMapping("relatedRevisions")
    @RequiresAdminOrEditorOrUser
    public List<RevisionDto> relatedRevisions(@Argument final AuditedType type,
                                              @Argument final String id,
                                              @Argument final AuditedType relatedType) {
        return service.findRelatedRevisions(type, id, relatedType);
    }

    @QueryMapping("revision")
    @RequiresAdminOrEditorOrUser
    public RevisionDto revision(@Argument final AuditedType type, @Argument final String id, @Argument final Long revision) {
        return service.findRevision(type, id, revision);
    }

    @QueryMapping("revisionFeedPaged")
    @RequiresAdmin
    public CountedWindow<RevisionFeedEntryDto> revisionFeed(final ScrollSubrange subrange,
                                                            @Nullable @Argument final AuditedType type,
                                                            @Nullable @Argument final Integer sinceInDays) {
        return service.findFeed(type, sinceInDays, extractScrollRequest(subrange));
    }

    @QueryMapping("restorePreview")
    @RequiresAdmin
    public RestorePreviewDto restorePreview(@Argument final AuditedType type,
                                            @Argument final String id,
                                            @Argument final Long revision,
                                            @Nullable @Argument final Boolean cascade) {
        return service.preview(type, id, revision, Optional.ofNullable(cascade).orElse(false));
    }

    @MutationMapping("restore")
    @RequiresAdmin
    public RestoreResultDto restore(@Argument final AuditedType type,
                                    @Argument final String id,
                                    @Argument final Long revision,
                                    @Nullable @Argument final Boolean cascade) {
        return service.restore(type, id, revision, Optional.ofNullable(cascade).orElse(false));
    }
}