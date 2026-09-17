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
import nu.fgv.register.server.util.graphql.GraphqlUtil;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/graph", version = "1.0")
public class GraphApi {

    private static final int DEFAULT_SEARCH_LIMIT = 10;
    private static final int DEFAULT_NEIGHBOUR_LIMIT = 25;

    private final GraphService service;

    @GetMapping(value = "/search", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<CollectionModel<EntityModel<GraphNodeDto>>> search(
            @RequestParam(defaultValue = "") final String q,
            @RequestParam(defaultValue = "" + DEFAULT_SEARCH_LIMIT) final int first) {
        final List<EntityModel<GraphNodeDto>> models = service.search(q, first)
                .stream()
                .map(dto -> EntityModel.of(dto, getLinks(dto)))
                .toList();

        return ResponseEntity.ok(CollectionModel.of(models,
                linkTo(methodOn(GraphApi.class).search(q, first)).withSelfRel()));
    }

    @GetMapping(value = "/{type}/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<GraphNeighbourhoodDto>> retrieveNeighbourhood(
            @PathVariable final GraphNodeType type,
            @PathVariable final Long id,
            @RequestParam(defaultValue = "" + DEFAULT_NEIGHBOUR_LIMIT) final int first) {
        return service.findNeighbourhood(type, id, first)
                .map(dto -> ResponseEntity.ok(EntityModel.of(dto,
                        linkTo(methodOn(GraphApi.class).retrieveNeighbourhood(type, id, first)).withSelfRel())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/{type}/{id}/neighbours", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<CollectionModel<EntityModel<GraphNodeDto>>> retrieveNeighbours(
            @PathVariable final GraphNodeType type,
            @PathVariable final Long id,
            @RequestParam final GraphEdgeType edge,
            @RequestParam(defaultValue = "0") final long offset,
            @RequestParam(defaultValue = "" + DEFAULT_NEIGHBOUR_LIMIT) final int first) {
        // Offset paging rather than cursors: the GraphQL side gets cursors from Spring's connection
        // support, but a plain REST caller is better served by a number it can compute itself.
        final GraphqlUtil.ScrollRequest scroll = new GraphqlUtil.ScrollRequest(
                offset == 0L ? Optional.empty() : Optional.of(ScrollPosition.offset(offset - 1)), first, false);
        final var window = service.findNeighbours(type, id, edge, scroll);
        final List<EntityModel<GraphNodeDto>> models = window.getContent()
                .stream()
                .map(dto -> EntityModel.of(dto, getLinks(dto)))
                .toList();

        return ResponseEntity.ok(CollectionModel.of(models,
                linkTo(methodOn(GraphApi.class).retrieveNeighbours(type, id, edge, offset, first)).withSelfRel()));
    }

    private List<Link> getLinks(final GraphNodeDto dto) {
        return List.of(
                linkTo(methodOn(GraphApi.class).retrieveNeighbourhood(dto.type(), dto.entityId(), DEFAULT_NEIGHBOUR_LIMIT))
                        .withRel("neighbourhood")
        );
    }

}
