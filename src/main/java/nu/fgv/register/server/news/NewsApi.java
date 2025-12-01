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

package nu.fgv.register.server.news;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventApi;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventService;
import nu.fgv.register.server.util.filter.FilterOperation;
import nu.fgv.register.server.util.security.RequiresAdmin;
import nu.fgv.register.server.util.security.RequiresAdminOrEditor;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.SortDefault;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/news")
public class NewsApi {

    private final NewsService service;
    private final EventService eventService;
    private final PagedResourcesAssembler<NewsDto> pagedResourcesAssembler;
    private final EventApi eventApi;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<PagedModel<EntityModel<NewsDto>>> retrieve(@SortDefault(sort = News_.VISIBLE_FROM, direction = Sort.Direction.ASC) final Pageable pageable,
                                                                     @RequestParam(required = false, defaultValue = News_.PUBLISHED + ":" + FilterOperation.TRUE) final String filter) {
        final PagedModel<EntityModel<NewsDto>> paged = pagedResourcesAssembler.toModel(service.find(filter, pageable));

        paged.getContent().forEach(this::addLinks);

        return ResponseEntity.ok(paged);
    }

    @PostMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditor
    public ResponseEntity<EntityModel<NewsDto>> create(@Valid @RequestBody final NewsCreateDto dto) {
        final NewsDto newDto = service.create(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, linkTo(methodOn(NewsApi.class).retrieve(newDto.getId())).toString())
                .body(EntityModel.of(newDto, getLinks(newDto)));
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<NewsDto>> retrieve(@PathVariable final Long id) {
        final NewsDto dto = service.findById(id);

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto)));
    }

    @PutMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditor
    public ResponseEntity<EntityModel<NewsDto>> update(@PathVariable final Long id, @Valid @RequestBody final NewsUpdateDto dto) {
        if (!Objects.equals(id, dto.id())) {
            return ResponseEntity.badRequest().build();
        }

        final NewsDto updatedDto = service.update(dto);

        return ResponseEntity.ok(EntityModel.of(updatedDto, getLinks(updatedDto)));
    }

    @PatchMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditor
    public ResponseEntity<EntityModel<NewsDto>> partialUpdate(@PathVariable final Long id, @Valid @RequestBody final NewsUpdateDto dto) {
        if (!Objects.equals(id, dto.id())) {
            return ResponseEntity.badRequest().build();
        }

        final NewsDto updatedDto = service.partialUpdate(dto);

        return ResponseEntity.ok(EntityModel.of(updatedDto, getLinks(updatedDto)));
    }

    @DeleteMapping("/{id}")
    @RequiresAdminOrEditor
    public ResponseEntity<Object> delete(@PathVariable final Long id) {
        service.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/events", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<CollectionModel<EntityModel<EventDto>>> retrieveEvents(@RequestParam(defaultValue = "90") final Integer sinceInDays) {
        final List<EntityModel<EventDto>> events = eventService.findBySource(sinceInDays, Event.SourceType.NEWS).stream()
                .map(dto -> EntityModel.of(dto, eventApi.getLinks(dto)))
                .toList();

        return ResponseEntity.ok(
                CollectionModel.of(events,
                        linkTo(methodOn(EventApi.class).retrieve(-1)).withSelfRel()));
    }

    private void addLinks(final EntityModel<NewsDto> entity) {
        if (entity.getContent() != null) {
            entity.getContent().add(getLinks(entity.getContent()));
        }
    }

    List<Link> getLinks(final NewsDto dto) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(NewsApi.class).retrieve(dto.getId())).withSelfRel());
        links.add(linkTo(methodOn(NewsApi.class).retrieve(Pageable.unpaged(), "")).withRel("news"));
        links.add(linkTo(methodOn(NewsApi.class).retrieveEvents(-1)).withRel("events"));

        return links;
    }

}
