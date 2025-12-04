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

package nu.fgv.register.server.spexare.activity;

import graphql.execution.DataFetcherResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.SpexareDto;
import nu.fgv.register.server.util.graphql.GraphqlUtil;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.data.query.ScrollSubrange;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static nu.fgv.register.server.util.graphql.GraphqlUtil.buildDataFetcherResult;
import static nu.fgv.register.server.util.graphql.GraphqlUtil.extractScrollPositionAndLimitAndOrder;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class ActivityGraphqlApi {

    private final ActivityService service;

    @MutationMapping("activityCreate")
    @RequiresAdminOrEditorOrUser
    public DataFetcherResult<ActivityDto> create(@Argument final Long spexareId) {
        return buildDataFetcherResult(
                service.create(spexareId),
                Map.of(
                        "spexareId", spexareId
                )
        );
    }

    @MutationMapping("activityDelete")
    @RequiresAdminOrEditorOrUser
    public void delete(@Argument final Long spexareId, @Argument final Long id) {
        service.deleteById(spexareId, id);
    }

    @SchemaMapping(typeName = "Spexare", field = "activitiesPaged")
    @RequiresAdminOrEditorOrUser
    public DataFetcherResult<Window<ActivityDto>> retrieveBySpexare(final SpexareDto dto, final ScrollSubrange subrange, @Nullable final Sort sort) {
        final GraphqlUtil.ScrollPositionAndLimitHolder holder = extractScrollPositionAndLimitAndOrder(subrange);

        return buildDataFetcherResult(
                service.findBySpexare(dto.getId(), holder.limit(), Optional.ofNullable(sort).orElse(Sort.unsorted()), holder.scrollPosition()),
                Map.of(
                        "spexareId", dto.getId()
                )
        );
    }

    @SchemaMapping(typeName = "Spexare", field = "activities")
    @RequiresAdminOrEditorOrUser
    public DataFetcherResult<List<ActivityDto>> retrieveBySpexare(final SpexareDto dto) {
        return buildDataFetcherResult(
                service.findBySpexare(dto.getId()),
                Map.of(
                        "spexareId", dto.getId()
                )
        );
    }

    @SchemaMapping(typeName = "Activity", field = "id")
    public Long getId(final ActivityDto dto) {
        return dto.getId();
    }

    @SchemaMapping(typeName = "Activity", field = "createdBy")
    public String getCreatedBy(final ActivityDto dto) {
        return dto.getCreatedBy();
    }

    @SchemaMapping(typeName = "Activity", field = "createdAt")
    public Instant getCreatedAt(final ActivityDto dto) {
        return dto.getCreatedAt();
    }

    @SchemaMapping(typeName = "Activity", field = "lastModifiedBy")
    public @Nullable String getLastModifiedBy(final ActivityDto dto) {
        return dto.getLastModifiedBy();
    }

    @SchemaMapping(typeName = "Activity", field = "lastModifiedAt")
    public Instant getLastModifiedAt(final ActivityDto dto) {
        return dto.getLastModifiedAt();
    }
}
