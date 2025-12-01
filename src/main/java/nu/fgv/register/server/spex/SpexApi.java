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

package nu.fgv.register.server.spex;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventApi;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventService;
import nu.fgv.register.server.spex.category.SpexCategoryApi;
import nu.fgv.register.server.spex.category.SpexCategoryDto;
import nu.fgv.register.server.util.Constants;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.util.filter.FilterOperation;
import nu.fgv.register.server.util.security.RequiresAdmin;
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
@RequestMapping("/api/v1/spex")
public class SpexApi {

    private final SpexService service;
    private final SpexExportService exportService;
    private final EventService eventService;
    private final PagedResourcesAssembler<SpexDto> pagedResourcesAssembler;
    private final SpexCategoryApi spexCategoryApi;
    private final EventApi eventApi;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<PagedModel<EntityModel<SpexDto>>> retrieve(@SortDefault(sort = Spex_.YEAR, direction = Sort.Direction.ASC) final Pageable pageable,
                                                                     @RequestParam(required = false, defaultValue = Spex_.PARENT + ":" + FilterOperation.NULL) final String filter) {
        final PagedModel<EntityModel<SpexDto>> paged = pagedResourcesAssembler.toModel(service.find(filter, pageable));

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
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<Resource> retrieve(@RequestParam(required = false) final List<Long> ids, @RequestHeader(HttpHeaders.ACCEPT) final String contentType, final Locale locale) {
        final Pair<String, byte[]> export = exportService.doExport(ids, contentType, locale);

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"spex" + export.getFirst() + "\"")
                .body(new ByteArrayResource(export.getSecond()));
    }

    @PostMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<EntityModel<SpexDto>> create(@Valid @RequestBody final SpexCreateDto dto) {
        final SpexDto newDto = service.create(dto);

        return ResponseEntity.created(linkTo(methodOn(SpexApi.class).retrieve(newDto.getId())).toUri())
                .body(EntityModel.of(newDto, getLinks(newDto, true)));
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<SpexDto>> retrieve(@PathVariable final Long id) {
        final SpexDto dto = service.findById(id);

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto)));
    }

    @PutMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditor
    public ResponseEntity<EntityModel<SpexDto>> update(@PathVariable final Long id, @Valid @RequestBody final SpexUpdateDto dto) {
        if (!Objects.equals(id, dto.id())) {
            return ResponseEntity.badRequest().build();
        }

        final SpexDto updatedDto = service.update(dto);

        return ResponseEntity.ok(EntityModel.of(updatedDto, getLinks(updatedDto)));
    }

    @PatchMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditor
    public ResponseEntity<EntityModel<SpexDto>> partialUpdate(@PathVariable final Long id, @Valid @RequestBody final SpexUpdateDto dto) {
        if (!Objects.equals(id, dto.id())) {
            return ResponseEntity.badRequest().build();
        }

        final SpexDto updatedDto = service.partialUpdate(dto);

        return ResponseEntity.ok(EntityModel.of(updatedDto, getLinks(updatedDto)));
    }

    @DeleteMapping("/{id}")
    @RequiresAdmin
    public ResponseEntity<Object> delete(@PathVariable final Long id) {
        service.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{spexId}/poster")
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<Resource> downloadPoster(@PathVariable final Long spexId) {
        final Pair<byte[], String> poster = service.getPoster(spexId);

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(poster.getSecond()))
                .body(new ByteArrayResource(poster.getFirst()));
    }

    @RequestMapping(value = "/{spexId}/poster", method = {RequestMethod.POST, RequestMethod.PUT}, consumes = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_GIF_VALUE})
    @RequiresAdminOrEditor
    public ResponseEntity<Object> uploadPoster(@PathVariable final Long spexId, @RequestBody final byte[] file, @RequestHeader(HttpHeaders.CONTENT_TYPE) @Nullable final String contentType) {
        service.savePoster(spexId, file, contentType);

        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/{spexId}/poster", method = {RequestMethod.POST, RequestMethod.PUT}, consumes = {"multipart/form-data"})
    @RequiresAdminOrEditor
    public ResponseEntity<Object> uploadPoster(@PathVariable final Long spexId, @RequestParam("file") final MultipartFile file) {
        try {
            return uploadPoster(spexId, file.getBytes(), file.getContentType());
        } catch (final IOException e) {
            throw new InternalErrorException(e.getMessage());
        }
    }

    @DeleteMapping("/{spexId}/poster")
    @RequiresAdminOrEditor
    public ResponseEntity<Object> deletePoster(@PathVariable final Long spexId) {
        service.deletePoster(spexId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{spexId}/parent", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<SpexDto>> retrieveParent(@PathVariable final Long spexId) {
        final SpexDto dto = service.findParentById(spexId);

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto)));
    }

    @GetMapping(value = "/{spexId}/revivals/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<SpexDto>> retrieveRevival(@PathVariable final Long spexId, @PathVariable final Long id) {
        final SpexDto dto = service.findRevivalById(spexId, id);

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto)));
    }

    @GetMapping(value = "/{spexId}/revivals", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<PagedModel<EntityModel<SpexDto>>> retrieveRevivalsByParent(@PathVariable final Long spexId,
                                                                                     @SortDefault(sort = Spex_.YEAR, direction = Sort.Direction.ASC) final Pageable pageable) {
        final PagedModel<EntityModel<SpexDto>> paged = pagedResourcesAssembler.toModel(service.findRevivalsByParent(spexId, pageable));
        paged.getContent().forEach(this::addLinks);

        return ResponseEntity.ok(paged);
    }

    @PostMapping(value = "/{spexId}/revivals/{year}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditor
    public ResponseEntity<EntityModel<SpexDto>> createRevival(@PathVariable final Long spexId, @PathVariable final String year) {
        final SpexDto dto = service.addRevival(spexId, year);

        return ResponseEntity.created(linkTo(methodOn(SpexApi.class).retrieveRevival(spexId, dto.getId())).toUri())
                .body(EntityModel.of(dto, getLinks(dto)));
    }

    @DeleteMapping(value = "/{spexId}/revivals/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditor
    public ResponseEntity<Object> deleteRevival(@PathVariable final Long spexId, @PathVariable final Long id) {
        service.deleteRevival(spexId, id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{spexId}/category", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<SpexCategoryDto>> retrieveCategory(@PathVariable final Long spexId) {
        final SpexCategoryDto dto = service.findCategoryBySpex(spexId);

        return ResponseEntity.ok(EntityModel.of(dto, spexCategoryApi.getLinks(dto)));
    }

    @PutMapping(value = "/{spexId}/category/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<Object> addCategory(@PathVariable final Long spexId, @PathVariable final Long id) {
        service.addCategory(spexId, id);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value = "/{spexId}/category", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<Object> removeCategory(@PathVariable final Long spexId) {
        service.removeCategory(spexId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/events", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<CollectionModel<EntityModel<EventDto>>> retrieveEvents(@RequestParam(defaultValue = "90") final Integer sinceInDays) {
        final List<EntityModel<EventDto>> events = eventService.findBySource(sinceInDays, Event.SourceType.SPEX).stream()
                .map(dto -> EntityModel.of(dto, eventApi.getLinks(dto)))
                .toList();

        return ResponseEntity.ok(
                CollectionModel.of(events,
                        linkTo(methodOn(EventApi.class).retrieve(-1)).withSelfRel()));
    }

    private void addLinks(final EntityModel<SpexDto> entity) {
        if (entity.getContent() != null) {
            entity.getContent().add(getLinks(entity.getContent()));
        }
    }

    void addLinks(final SpexDto dto) {
        dto.add(getLinks(dto));
    }

    public List<Link> getLinks(final SpexDto dto) {
        return getLinks(dto, true);
    }

    public List<Link> getLinks(final SpexDto dto, final boolean includeEvents) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(SpexApi.class).retrieve(dto.getId())).withSelfRel());
        links.add(linkTo(methodOn(SpexApi.class).retrieve(Pageable.unpaged(), Spex_.PARENT + ":NULL")).withRel("spex"));
        links.add(linkTo(methodOn(SpexApi.class).retrieve(Pageable.unpaged(), Spex_.PARENT + "!NULL")).withRel("spex-including-revivals"));
        links.add(linkTo(methodOn(SpexApi.class).downloadPoster(dto.getId())).withRel("poster"));
        links.add(linkTo(methodOn(SpexApi.class).retrieveCategory(dto.getId())).withRel("category"));
        if (dto.isRevival()) {
            links.add(linkTo(methodOn(SpexApi.class).retrieveParent(dto.getId())).withRel("parent"));
        } else {
            links.add(linkTo(methodOn(SpexApi.class).retrieveRevivalsByParent(dto.getId(), Pageable.unpaged())).withRel("revivals"));
        }
        if (includeEvents) {
            links.add(linkTo(methodOn(SpexApi.class).retrieveEvents(-1)).withRel("events"));
        }

        return links;
    }

}
