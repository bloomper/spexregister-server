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

import nu.fgv.register.server.util.AbstractApiTest;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.graphql.GraphqlUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.OffsetScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@WebMvcTest(value = GraphApi.class)
class GraphApiTest extends AbstractApiTest {

    @MockitoBean
    private GraphService service;

    private static GraphNodeDto node(final GraphNodeType type, final Long id, final String label, final String sublabel) {
        return new GraphNodeDto(GraphNodeDto.idOf(type, id), type, label, sublabel, null, false, id);
    }

    @Test
    void should_search() throws Exception {
        when(service.search(anyString(), anyInt()))
                .thenReturn(List.of(node(GraphNodeType.SPEXARE, 1L, "Ada Lovelace", "Countess")));

        mockMvc
                .perform(
                        get("/api/graph/search?q=ada&first=10")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(
                        document(
                                "graph-search",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                queryParameters(
                                        parameterWithName("q").description("The term to match against names and titles. Leave it empty to be given a random starting point."),
                                        parameterWithName("first").description("The maximum number of nodes to return per type").optional()
                                ),
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(GraphApi.class, "search", String.class, int.class))
                        )
                );
    }

    @Test
    void should_get_neighbourhood() throws Exception {
        final var origin = node(GraphNodeType.SPEXARE, 1L, "Ada Lovelace", "Countess");
        final var spex = node(GraphNodeType.SPEX, 100L, "Bacchus", "1973");

        when(service.findNeighbourhood(any(GraphNodeType.class), anyLong(), anyInt()))
                .thenReturn(Optional.of(new GraphNeighbourhoodDto(origin, List.of(
                        new GraphGroupDto(GraphEdgeType.PARTICIPATION, 1L, List.of(spex),
                                List.of(GraphEdgeDto.of(origin.id(), spex.id(), GraphEdgeType.PARTICIPATION, "1973")))
                ))));

        mockMvc
                .perform(
                        get("/api/graph/{type}/{id}?first=25", "SPEXARE", 1L)
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(
                        document(
                                "graph-get-neighbourhood",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("type").description("The type of the node to expand"),
                                        parameterWithName("id").description("The id of the node to expand")
                                ),
                                queryParameters(
                                        parameterWithName("first").description("The maximum number of neighbours to return per relation").optional()
                                ),
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(GraphApi.class, "retrieveNeighbourhood", GraphNodeType.class, Long.class, int.class))
                        )
                );
    }

    @Test
    void should_get_neighbours() throws Exception {
        final var spexare = node(GraphNodeType.SPEXARE, 1L, "Ada Lovelace", "Countess");
        final CountedWindow<GraphNodeDto> window = CountedWindow.of(
                Window.from(List.of(spexare), OffsetScrollPosition.positionFunction(0), true), 275L);

        when(service.findNeighbours(any(GraphNodeType.class), anyLong(), any(GraphEdgeType.class), any(GraphqlUtil.ScrollRequest.class)))
                .thenReturn(window);

        mockMvc
                .perform(
                        get("/api/graph/{type}/{id}/neighbours?edge=TAG&offset=0&first=25", "TAG", 300L)
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(
                        document(
                                "graph-get-neighbours",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("type").description("The type of the node whose neighbours are wanted"),
                                        parameterWithName("id").description("The id of the node whose neighbours are wanted")
                                ),
                                queryParameters(
                                        parameterWithName("edge").description("The relation to walk"),
                                        parameterWithName("offset").description("The number of neighbours to skip").optional(),
                                        parameterWithName("first").description("The maximum number of neighbours to return").optional()
                                ),
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(GraphApi.class, "retrieveNeighbours", GraphNodeType.class, Long.class, GraphEdgeType.class, long.class, int.class))
                        )
                );
    }

}
