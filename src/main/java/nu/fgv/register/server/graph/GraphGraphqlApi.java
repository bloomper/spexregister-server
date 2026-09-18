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

package nu.fgv.register.server.graph;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.query.ScrollSubrange;
import org.springframework.stereotype.Controller;

import java.util.List;

import static nu.fgv.register.server.util.graphql.GraphqlUtil.extractScrollRequest;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class GraphGraphqlApi {

    private final GraphService service;

    @QueryMapping("graphSearch")
    @RequiresAdminOrEditorOrUser
    public List<GraphNodeDto> search(@Argument final String q, @Argument final int first) {
        return service.search(q, first);
    }

    @QueryMapping("graphNeighbourhood")
    @RequiresAdminOrEditorOrUser
    public @Nullable GraphNeighbourhoodDto retrieveNeighbourhood(@Argument final GraphNodeType type,
                                                                 @Argument final Long id,
                                                                 @Argument final int first) {
        return service.findNeighbourhood(type, id, first).orElse(null);
    }

    @QueryMapping("graphNeighboursPaged")
    @RequiresAdminOrEditorOrUser
    public CountedWindow<GraphNodeDto> retrieveNeighbours(@Argument final GraphNodeType type,
                                                          @Argument final Long id,
                                                          @Argument final GraphEdgeType edge,
                                                          final ScrollSubrange subrange) {
        return service.findNeighbours(type, id, edge, extractScrollRequest(subrange));
    }

}
