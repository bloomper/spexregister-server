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

package nu.fgv.register.server.spexare.activity.task.actor;

import graphql.execution.DataFetcherResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.activity.task.TaskActivityDto;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.LocalContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.data.query.ScrollSubrange;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static nu.fgv.register.server.util.graphql.GraphqlUtil.buildDataFetcherResult;
import static nu.fgv.register.server.util.graphql.GraphqlUtil.extractScrollRequest;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class ActorGraphqlApi {

    private final ActorService service;

    @MutationMapping("actorCreate")
    @RequiresAdminOrEditorOrUser
    public DataFetcherResult<ActorDto> create(@Argument final Long spexareId, @Argument final Long activityId, @Argument final Long taskActivityId, @Argument final String vocalId, @Valid @Argument final ActorCreateDto input) {
        return buildDataFetcherResult(
                service.create(spexareId, activityId, taskActivityId, vocalId, input),
                Map.of(
                        "spexareId", spexareId,
                        "activityId", activityId,
                        "taskActivityId", taskActivityId,
                        "vocalId", vocalId
                )
        );
    }

    @MutationMapping("actorUpdate")
    @RequiresAdminOrEditorOrUser
    public DataFetcherResult<ActorDto> update(@Argument final Long spexareId, @Argument final Long activityId, @Argument final Long taskActivityId, @Argument final String vocalId, @Valid @Argument final ActorUpdateDto input) {
        return buildDataFetcherResult(
                service.update(spexareId, activityId, taskActivityId, vocalId, input.id(), input),
                Map.of(
                        "spexareId", spexareId,
                        "activityId", activityId,
                        "taskActivityId", taskActivityId,
                        "vocalId", vocalId
                )
        );
    }

    @MutationMapping("actorDelete")
    @RequiresAdminOrEditorOrUser
    public void delete(@Argument final Long spexareId, @Argument final Long activityId, @Argument final Long taskActivityId, @Argument final String vocalId, @Argument final Long id) {
        service.deleteById(spexareId, activityId, taskActivityId, vocalId, id);
    }

    @SchemaMapping(typeName = "TaskActivity", field = "actorsPaged")
    @RequiresAdminOrEditorOrUser
    public CountedWindow<ActorDto> retrieveByTaskActivity(@LocalContextValue("spexareId") final Long spexareId,
                                                          @LocalContextValue("activityId") final Long activityId,
                                                          final TaskActivityDto dto,
                                                          final ScrollSubrange subrange,
                                                          @Argument final Optional<String> filter,
                                                          final Optional<Sort> sort) {
        return service.findByTaskActivity(
                spexareId,
                activityId,
                dto.getId(),
                filter.orElse(""),
                extractScrollRequest(subrange),
                sort.orElse(Sort.unsorted())
        );
    }

    @SchemaMapping(typeName = "TaskActivity", field = "actors")
    @RequiresAdminOrEditorOrUser
    public List<ActorDto> retrieveByTaskActivity(@LocalContextValue("spexareId") final Long spexareId,
                                                 @LocalContextValue("activityId") final Long activityId,
                                                 final TaskActivityDto dto) {
        return service.findByTaskActivity(spexareId, activityId, dto.getId());
    }

}
