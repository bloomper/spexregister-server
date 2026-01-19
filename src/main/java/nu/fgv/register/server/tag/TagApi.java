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

package nu.fgv.register.server.tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventApi;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventService;
import nu.fgv.register.server.util.Constants;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.impex.model.ImportResultDto;
import nu.fgv.register.server.util.security.RequiresAdminOrEditor;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.jspecify.annotations.Nullable;
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
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
@RequestMapping(path = "/api/tags", version = "1.0")
public class TagApi {

    private final TagService service;
    private final TagExportService exportService;
    private final TagImportService importService;
    private final EventService eventService;
    private final PagedResourcesAssembler<TagDto> pagedResourcesAssembler;
    private final EventApi eventApi;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<PagedModel<EntityModel<TagDto>>> retrieve(@SortDefault(sort = Tag_.NAME, direction = Sort.Direction.ASC) final Pageable pageable,
                                                                    @RequestParam(required = false, defaultValue = "") final String filter) {
        final PagedModel<EntityModel<TagDto>> paged = pagedResourcesAssembler.toModel(service.find(filter, pageable));

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
    @RequiresAdminOrEditor
    public ResponseEntity<Resource> retrieve(@Nullable @RequestParam(required = false) final List<Long> ids,
                                             @RequestParam(required = false, defaultValue = "") final String filter,
                                             @RequestHeader(HttpHeaders.ACCEPT) final String contentType,
                                             final Locale locale) {
        final Pair<String, byte[]> export = exportService.doExport(Optional.ofNullable(ids).orElse(Collections.emptyList()), filter, contentType, locale);

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tags" + export.getFirst() + "\"")
                .body(new ByteArrayResource(export.getSecond()));
    }

    @PostMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditor
    public ResponseEntity<EntityModel<TagDto>> create(@Valid @RequestBody final TagCreateDto dto) {
        final TagDto newDto = service.create(dto);

        return ResponseEntity.created(linkTo(methodOn(TagApi.class).retrieve(newDto.getId())).toUri())
                .body(EntityModel.of(newDto, getLinks(newDto)));
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<TagDto>> retrieve(@PathVariable final Long id) {
        final TagDto dto = service.findById(id);

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto)));
    }

    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT},
            consumes = {
                    Constants.MediaTypes.APPLICATION_XLSX_VALUE,
                    Constants.MediaTypes.APPLICATION_XLS_VALUE
            })
    @RequiresAdminOrEditor
    public ResponseEntity<ImportResultDto> createAndUpdate(@RequestBody final byte[] file, @RequestHeader(HttpHeaders.CONTENT_TYPE) @Nullable final String contentType, final Locale locale) {
        final ImportResultDto result = importService.doImport(file, contentType, locale);

        return ResponseEntity
                .status(result.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(result);
    }

    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT}, consumes = {"multipart/form-data"})
    @RequiresAdminOrEditor
    public ResponseEntity<ImportResultDto> createAndUpdate(@RequestParam("file") final MultipartFile file, final Locale locale) {
        try {
            return createAndUpdate(file.getBytes(), file.getContentType(), locale);
        } catch (final IOException e) {
            throw new InternalErrorException(e.getMessage());
        }
    }

    @PutMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditor
    public ResponseEntity<EntityModel<TagDto>> update(@PathVariable final Long id, @Valid @RequestBody final TagUpdateDto dto) {
        if (!Objects.equals(id, dto.id())) {
            return ResponseEntity.badRequest().build();
        }

        final TagDto updatedDto = service.update(dto);

        return ResponseEntity.ok(EntityModel.of(updatedDto, getLinks(updatedDto)));
    }

    @PatchMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditor
    public ResponseEntity<EntityModel<TagDto>> partialUpdate(@PathVariable final Long id, @RequestBody final TagUpdateDto dto) {
        if (!Objects.equals(id, dto.id())) {
            return ResponseEntity.badRequest().build();
        }

        final TagDto updatedDto = service.partialUpdate(dto);

        return ResponseEntity.ok(EntityModel.of(updatedDto, getLinks(updatedDto)));
    }

    @DeleteMapping("/{id}")
    @RequiresAdminOrEditor
    public ResponseEntity<Object> delete(@PathVariable final Long id) {
        service.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/events/{sourceId}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<CollectionModel<EntityModel<EventDto>>> retrieveEvents(@PathVariable final Long sourceId, @RequestParam(defaultValue = "90") final Integer sinceInDays) {
        final List<EntityModel<EventDto>> events = eventService.findBySourceTypeAndId(Event.SourceType.TAG, sourceId, sinceInDays).stream()
                .map(dto -> EntityModel.of(dto, eventApi.getLinks(dto)))
                .toList();

        return ResponseEntity.ok(
                CollectionModel.of(events,
                        linkTo(methodOn(EventApi.class).retrieve(Event.SourceType.TAG, -1)).withSelfRel()));
    }

    private void addLinks(final EntityModel<TagDto> entity) {
        if (entity.getContent() != null) {
            addLinks(entity.getContent());
        }
    }

    void addLinks(final TagDto dto) {
        dto.add(getLinks(dto));
    }

    List<Link> getLinks(final TagDto dto) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(TagApi.class).retrieve(dto.getId())).withSelfRel());
        links.add(linkTo(methodOn(TagApi.class).retrieve(Pageable.unpaged(), "")).withRel("tags"));
        links.add(linkTo(methodOn(TagApi.class).retrieveEvents(dto.getId(), -1)).withRel("events"));

        return links;
    }
}
