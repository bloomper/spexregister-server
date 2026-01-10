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

package nu.fgv.register.server.spexare;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventApi;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventService;
import nu.fgv.register.server.spexare.activity.ActivityApi;
import nu.fgv.register.server.spexare.address.AddressApi;
import nu.fgv.register.server.spexare.consent.ConsentApi;
import nu.fgv.register.server.spexare.membership.MembershipApi;
import nu.fgv.register.server.spexare.tagging.TaggingApi;
import nu.fgv.register.server.spexare.toggle.ToggleApi;
import nu.fgv.register.server.util.Constants;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.util.error.ResourceNoValueException;
import nu.fgv.register.server.util.search.PagedWithFacetsModel;
import nu.fgv.register.server.util.search.PagedWithFacetsResourcesAssembler;
import nu.fgv.register.server.util.security.RequiresAdmin;
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
import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/spexare", version = "1.0")
public class SpexareApi {

    private final SpexareService service;
    private final SpexareExportService exportService;
    private final EventService eventService;
    private final PagedResourcesAssembler<SpexareDto> pagedResourcesAssembler;
    private final PagedWithFacetsResourcesAssembler<SpexareDto> pagedWithFacetsResourcesAssembler;
    private final EventApi eventApi;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE, params = {"!q"})
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<PagedModel<EntityModel<SpexareDto>>> retrieve(@SortDefault(sort = Spexare_.FIRST_NAME, direction = Sort.Direction.ASC) final Pageable pageable,
                                                                        @RequestParam(required = false, defaultValue = Spexare_.PUBLISHED + ":true") final String filter) {
        final PagedModel<EntityModel<SpexareDto>> paged = pagedResourcesAssembler.toModel(service.find(filter, pageable));

        paged.getContent().forEach(this::addLinks);

        return ResponseEntity.ok(paged);
    }

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE, params = {"q"})
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<PagedWithFacetsModel<EntityModel<SpexareDto>>> search(@RequestParam final String q,
                                                                                @SortDefault(sort = "score", direction = Sort.Direction.DESC) final Pageable pageable) {
        final PagedWithFacetsModel<EntityModel<SpexareDto>> paged = pagedWithFacetsResourcesAssembler.toModel(service.search(q, pageable));

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
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"spexare" + export.getFirst() + "\"")
                .body(new ByteArrayResource(export.getSecond()));
    }

    @PostMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<EntityModel<SpexareDto>> create(@Valid @RequestBody final SpexareCreateDto dto) {
        final SpexareDto newDto = service.create(dto);

        return ResponseEntity.created(linkTo(methodOn(SpexareApi.class).retrieve(newDto.getId())).toUri())
                .body(EntityModel.of(newDto, getLinks(newDto)));
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<SpexareDto>> retrieve(@PathVariable final Long id) {
        final SpexareDto dto = service.findById(id);

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto)));
    }

    @PutMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<SpexareDto>> update(@PathVariable final Long id, @Valid @RequestBody final SpexareUpdateDto dto) {
        if (!Objects.equals(id, dto.id())) {
            return ResponseEntity.badRequest().build();
        }

        final SpexareDto updatedDto = service.update(dto);

        return ResponseEntity.ok(EntityModel.of(updatedDto, getLinks(updatedDto)));
    }

    @PatchMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<SpexareDto>> partialUpdate(@PathVariable final Long id, @Valid @RequestBody final SpexareUpdateDto dto) {
        if (!Objects.equals(id, dto.id())) {
            return ResponseEntity.badRequest().build();
        }

        final SpexareDto updatedDto = service.partialUpdate(dto);

        return ResponseEntity.ok(EntityModel.of(updatedDto, getLinks(updatedDto)));
    }

    @DeleteMapping("/{id}")
    @RequiresAdmin
    public ResponseEntity<Object> delete(@PathVariable final Long id) {
        service.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{spexareId}/image")
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<Resource> downloadImage(@PathVariable final Long spexareId) {
        final Pair<byte[], String> image = service.getImage(spexareId);

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(image.getSecond()))
                .body(new ByteArrayResource(image.getFirst()));
    }

    @RequestMapping(value = "/{spexareId}/image", method = {RequestMethod.POST, RequestMethod.PUT}, consumes = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_GIF_VALUE})
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<Object> uploadImage(@PathVariable final Long spexareId, @RequestBody final byte[] file, @RequestHeader(HttpHeaders.CONTENT_TYPE) @Nullable final String contentType) {
        service.saveImage(spexareId, file, contentType);

        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/{spexareId}/image", method = {RequestMethod.POST, RequestMethod.PUT}, consumes = {"multipart/form-data"})
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<Object> uploadImage(@PathVariable final Long spexareId, @RequestParam("file") final MultipartFile file) {
        try {
            return uploadImage(spexareId, file.getBytes(), file.getContentType());
        } catch (final IOException e) {
            throw new InternalErrorException(e.getMessage());
        }
    }

    @DeleteMapping("/{spexareId}/image")
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<Object> deleteImage(@PathVariable final Long spexareId) {
        service.deleteImage(spexareId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{spexareId}/partner", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<SpexareDto>> retrievePartner(@PathVariable final Long spexareId) {
        final SpexareDto dto = service.findPartnerBySpexare(spexareId)
                .orElseThrow(() -> new ResourceNoValueException(Spexare.class, Spexare_.PARTNER, spexareId));

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto)));
    }

    @PutMapping(value = "/{spexareId}/partner/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<Object> addPartner(@PathVariable final Long spexareId, @PathVariable final Long id) {
        service.addPartner(spexareId, id);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value = "/{spexareId}/partner", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<Object> removePartner(@PathVariable final Long spexareId) {
        service.removePartner(spexareId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/events", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<CollectionModel<EntityModel<EventDto>>> retrieveEvents(@RequestParam(defaultValue = "90") final Integer sinceInDays) {
        final List<EntityModel<EventDto>> events = eventService.findBySource(sinceInDays, Event.SourceType.SPEXARE).stream()
                .map(dto -> EntityModel.of(dto, eventApi.getLinks(dto)))
                .toList();

        return ResponseEntity.ok(
                CollectionModel.of(events,
                        linkTo(methodOn(EventApi.class).retrieve(-1)).withSelfRel()));
    }

    private void addLinks(final EntityModel<SpexareDto> entity) {
        if (entity.getContent() != null) {
            entity.getContent().add(getLinks(entity.getContent()));
        }
    }

    void addLinks(final SpexareDto dto) {
        dto.add(getLinks(dto));
    }

    public List<Link> getLinks(final SpexareDto dto) {
        final List<Link> links = new ArrayList<>();
        links.add(linkTo(methodOn(SpexareApi.class).retrieve(dto.getId())).withSelfRel());
        if (hasText(dto.getImageUrl())) {
            links.add(Link.of(dto.getImageUrl()).withRel("image"));
        } else {
            links.add(linkTo(methodOn(SpexareApi.class).downloadImage(dto.getId())).withRel("image"));
        }
        links.add(linkTo(methodOn(ActivityApi.class).retrieve(dto.getId(), Pageable.unpaged())).withRel("activities"));
        links.add(linkTo(methodOn(MembershipApi.class).retrieve(dto.getId(), Pageable.unpaged(), "")).withRel("memberships"));
        links.add(linkTo(methodOn(ConsentApi.class).retrieve(dto.getId(), Pageable.unpaged())).withRel("consents"));
        links.add(linkTo(methodOn(ToggleApi.class).retrieve(dto.getId(), Pageable.unpaged())).withRel("toggles"));
        links.add(linkTo(methodOn(AddressApi.class).retrieve(dto.getId(), Pageable.unpaged(), "")).withRel("addresses"));
        links.add(linkTo(methodOn(TaggingApi.class).retrieve(dto.getId(), Pageable.unpaged())).withRel("tags"));
        links.add(linkTo(methodOn(SpexareApi.class).retrievePartner(dto.getId())).withRel("partner"));
        links.add(linkTo(methodOn(SpexareApi.class).retrieveEvents(-1)).withRel("events"));

        return links;
    }

}
