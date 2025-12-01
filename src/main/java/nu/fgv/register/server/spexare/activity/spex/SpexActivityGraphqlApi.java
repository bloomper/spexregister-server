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

package nu.fgv.register.server.spexare.activity.spex;

import graphql.execution.DataFetcherResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spex.SpexDto;
import nu.fgv.register.server.spexare.activity.ActivityDto;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.LocalContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;

import static nu.fgv.register.server.util.graphql.GraphqlUtil.buildDataFetcherResult;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class SpexActivityGraphqlApi {

    private final SpexActivityService service;

    @MutationMapping("spexActivityCreate")
    @RequiresAdminOrEditorOrUser
    public DataFetcherResult<SpexActivityDto> create(@Argument final Long spexareId, @Argument final Long activityId, @Argument final Long spexId) {
        return buildDataFetcherResult(
                service.create(spexareId, activityId, spexId),
                Map.of(
                        "spexareId", spexareId,
                        "activityId", activityId
                )
        );
    }

    @MutationMapping("spexActivityUpdate")
    @RequiresAdminOrEditorOrUser
    public DataFetcherResult<SpexActivityDto> update(@Argument final Long spexareId, @Argument final Long activityId, @Argument final Long spexId, @Argument final Long id) {
        return buildDataFetcherResult(
                service.update(spexareId, activityId, spexId, id),
                Map.of(
                        "spexareId", spexareId,
                        "activityId", activityId
                )
        );
    }

    @MutationMapping("spexActivityDelete")
    @RequiresAdminOrEditorOrUser
    public void delete(@Argument final Long spexareId, @Argument final Long activityId, @Argument final Long id) {
        service.deleteById(spexareId, activityId, id);
    }

    @SchemaMapping(typeName = "Activity", field = "spexActivity")
    @RequiresAdminOrEditorOrUser
    @Nullable
    public DataFetcherResult<SpexActivityDto> retrieveByActivity(@LocalContextValue("spexareId") final Long spexareId,
                                                                 final ActivityDto dto) {
        try {
            return buildDataFetcherResult(
                    service.findByActivity(spexareId, dto.getId()),
                    Map.of(
                            "spexareId", spexareId,
                            "activityId", dto.getId()
                    )
            );
        } catch (final ResourceNotFoundException e) {
            return null;
        }
    }

    @SchemaMapping(typeName = "SpexActivity", field = "spex")
    @RequiresAdminOrEditorOrUser
    public SpexDto retrieveSpex(@LocalContextValue("spexareId") final Long spexareId,
                                @LocalContextValue("activityId") final Long activityId,
                                final SpexActivityDto dto) {
        return service.findSpexBySpexActivity(spexareId, activityId, dto.getId());
    }

}
