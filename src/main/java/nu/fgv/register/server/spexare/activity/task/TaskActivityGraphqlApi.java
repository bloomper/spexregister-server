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

package nu.fgv.register.server.spexare.activity.task;

import graphql.execution.DataFetcherResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.activity.ActivityDto;
import nu.fgv.register.server.task.TaskDto;
import nu.fgv.register.server.util.graphql.GraphqlUtil;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.LocalContextValue;
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
public class TaskActivityGraphqlApi {

    private final TaskActivityService service;

    @MutationMapping("taskActivityCreate")
    @RequiresAdminOrEditorOrUser
    public DataFetcherResult<TaskActivityDto> create(@Argument final Long spexareId, @Argument final Long activityId, @Argument final Long taskId) {
        return buildDataFetcherResult(
                service.create(spexareId, activityId, taskId),
                Map.of(
                        "spexareId", spexareId,
                        "activityId", activityId
                )
        );
    }

    @MutationMapping("taskActivityUpdate")
    @RequiresAdminOrEditorOrUser
    public DataFetcherResult<TaskActivityDto> update(@Argument final Long spexareId, @Argument final Long activityId, @Argument final Long taskId, @Argument final Long id) {
        return buildDataFetcherResult(
                service.update(spexareId, activityId, taskId, id),
                Map.of(
                        "spexareId", spexareId,
                        "activityId", activityId
                )
        );
    }

    @MutationMapping("taskActivityDelete")
    @RequiresAdminOrEditorOrUser
    public void delete(@Argument final Long spexareId, @Argument final Long activityId, @Argument final Long id) {
        service.deleteById(spexareId, activityId, id);
    }

    @SchemaMapping(typeName = "Activity", field = "taskActivitiesPaged")
    @RequiresAdminOrEditorOrUser
    public DataFetcherResult<Window<TaskActivityDto>> retrieveByActivity(@LocalContextValue("spexareId") final Long spexareId,
                                                                         final ActivityDto dto,
                                                                         final ScrollSubrange subrange,
                                                                         @Nullable final Sort sort) {
        final GraphqlUtil.ScrollPositionAndLimitHolder holder = extractScrollPositionAndLimitAndOrder(subrange);

        return buildDataFetcherResult(
                service.findByActivity(spexareId, dto.getId(), holder.limit(), Optional.ofNullable(sort).orElse(Sort.unsorted()), holder.scrollPosition()),
                Map.of(
                        "spexareId", spexareId,
                        "activityId", dto.getId()
                )
        );
    }

    @SchemaMapping(typeName = "Activity", field = "taskActivities")
    @RequiresAdminOrEditorOrUser
    public DataFetcherResult<List<TaskActivityDto>> retrieveByActivity(@LocalContextValue("spexareId") final Long spexareId,
                                                                       final ActivityDto dto) {
        return buildDataFetcherResult(
                service.findByActivity(spexareId, dto.getId()),
                Map.of(
                        "spexareId", spexareId,
                        "activityId", dto.getId()
                )
        );
    }

    @SchemaMapping(typeName = "TaskActivity", field = "task")
    @RequiresAdminOrEditorOrUser
    public TaskDto retrieveTask(@LocalContextValue("spexareId") final Long spexareId,
                                @LocalContextValue("activityId") final Long activityId,
                                final TaskActivityDto dto) {
        return service.findTaskByTaskActivity(spexareId, activityId, dto.getId());
    }

    @SchemaMapping(typeName = "TaskActivity", field = "id")
    public Long getId(final TaskActivityDto dto) {
        return dto.getId();
    }

    @SchemaMapping(typeName = "TaskActivity", field = "createdBy")
    public String getCreatedBy(final TaskActivityDto dto) {
        return dto.getCreatedBy();
    }

    @SchemaMapping(typeName = "TaskActivity", field = "createdAt")
    public Instant getCreatedAt(final TaskActivityDto dto) {
        return dto.getCreatedAt();
    }

    @SchemaMapping(typeName = "TaskActivity", field = "lastModifiedBy")
    public @Nullable String getLastModifiedBy(final TaskActivityDto dto) {
        return dto.getLastModifiedBy();
    }

    @SchemaMapping(typeName = "TaskActivity", field = "lastModifiedAt")
    public Instant getLastModifiedAt(final TaskActivityDto dto) {
        return dto.getLastModifiedAt();
    }
}
