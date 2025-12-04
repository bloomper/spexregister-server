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

package nu.fgv.register.server.task;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventApi;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventService;
import nu.fgv.register.server.task.category.TaskCategoryApi;
import nu.fgv.register.server.task.category.TaskCategoryDto;
import nu.fgv.register.server.util.Constants;
import nu.fgv.register.server.util.security.RequiresAdmin;
import nu.fgv.register.server.util.security.RequiresAdminOrEditor;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.SortDefault;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
@RequestMapping(path = "/api/tasks", version = "1.0")
public class TaskApi {

    private final TaskService service;
    private final TaskExportService exportService;
    private final EventService eventService;
    private final PagedResourcesAssembler<TaskDto> pagedResourcesAssembler;
    private final TaskCategoryApi taskCategoryApi;
    private final EventApi eventApi;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<PagedModel<EntityModel<TaskDto>>> retrieve(@SortDefault(sort = Task_.NAME, direction = Sort.Direction.ASC) final Pageable pageable,
                                                                     @RequestParam(required = false, defaultValue = "") final String filter) {
        final PagedModel<EntityModel<TaskDto>> paged = pagedResourcesAssembler.toModel(service.find(filter, pageable));
        paged.getContent().forEach(this::addLinks);

        return ResponseEntity.ok(paged);
    }

    @GetMapping(headers = {
            HttpHeaders.ACCEPT + "=" + Constants.MediaTypes.APPLICATION_XLSX_VALUE,
            HttpHeaders.ACCEPT + "=" + Constants.MediaTypes.APPLICATION_XLS_VALUE
    }, produces = {
            Constants.MediaTypes.APPLICATION_XLSX_VALUE,
            Constants.MediaTypes.APPLICATION_XLS_VALUE
    })
    @RequiresAdmin
    public ResponseEntity<Resource> retrieve(@RequestParam(required = false) final List<Long> ids, @RequestHeader(HttpHeaders.ACCEPT) final String contentType, final Locale locale) {
        final Pair<String, byte[]> export = exportService.doExport(ids, contentType, locale);

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"task" + export.getFirst() + "\"")
                .body(new ByteArrayResource(export.getSecond()));
    }

    @PostMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<EntityModel<TaskDto>> create(@Valid @RequestBody final TaskCreateDto dto) {
        final TaskDto newDto = service.create(dto);

        return ResponseEntity.created(linkTo(methodOn(TaskApi.class).retrieve(newDto.getId())).toUri())
                .body(EntityModel.of(newDto, getLinks(newDto)));
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<TaskDto>> retrieve(@PathVariable final Long id) {
        final TaskDto dto = service.findById(id);

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto)));
    }

    @PutMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditor
    public ResponseEntity<EntityModel<TaskDto>> update(@PathVariable final Long id, @Valid @RequestBody final TaskUpdateDto dto) {
        if (!Objects.equals(id, dto.id())) {
            return ResponseEntity.badRequest().build();
        }

        final TaskDto updatedDto = service.update(dto);

        return ResponseEntity.ok(EntityModel.of(updatedDto, getLinks(updatedDto)));
    }

    @PatchMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditor
    public ResponseEntity<EntityModel<TaskDto>> partialUpdate(@PathVariable final Long id, @Valid @RequestBody final TaskUpdateDto dto) {
        if (!Objects.equals(id, dto.id())) {
            return ResponseEntity.badRequest().build();
        }

        final TaskDto updatedDto = service.partialUpdate(dto);

        return ResponseEntity.ok(EntityModel.of(updatedDto, getLinks(updatedDto)));
    }

    @DeleteMapping("/{id}")
    @RequiresAdmin
    public ResponseEntity<Object> delete(@PathVariable final Long id) {
        service.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{id}/category", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<TaskCategoryDto>> retrieveCategory(@PathVariable final Long id) {
        final TaskCategoryDto dto = service.findCategoryByTask(id);

        return ResponseEntity.ok(EntityModel.of(dto, taskCategoryApi.getLinks(dto)));
    }

    @PutMapping(value = "/{taskId}/category/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<Object> addCategory(@PathVariable final Long taskId, @PathVariable final Long id) {
        service.addCategory(taskId, id);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value = "/{taskId}/category", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<Object> removeCategory(@PathVariable final Long taskId) {
        service.removeCategory(taskId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/events", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<CollectionModel<EntityModel<EventDto>>> retrieveEvents(@RequestParam(defaultValue = "90") final Integer sinceInDays) {
        final List<EntityModel<EventDto>> events = eventService.findBySource(sinceInDays, Event.SourceType.TASK).stream()
                .map(dto -> EntityModel.of(dto, eventApi.getLinks(dto)))
                .toList();

        return ResponseEntity.ok(
                CollectionModel.of(events,
                        linkTo(methodOn(EventApi.class).retrieve(-1)).withSelfRel()));
    }

    private void addLinks(final EntityModel<TaskDto> entity) {
        if (entity.getContent() != null) {
            entity.getContent().add(getLinks(entity.getContent()));
        }
    }

    void addLinks(final TaskDto dto) {
        dto.add(getLinks(dto));
    }

    public List<Link> getLinks(final TaskDto dto) {
        return getLinks(dto, true);
    }

    public List<Link> getLinks(final TaskDto dto, final boolean includeEvents) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(TaskApi.class).retrieve(dto.getId())).withSelfRel());
        links.add(linkTo(methodOn(TaskApi.class).retrieve(Pageable.unpaged(), "")).withRel("tasks"));
        links.add(linkTo(methodOn(TaskApi.class).retrieveCategory(dto.getId())).withRel("category"));
        if (includeEvents) {
            links.add(linkTo(methodOn(TaskApi.class).retrieveEvents(-1)).withRel("events"));
        }

        return links;
    }

}
