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
import nu.fgv.register.server.util.filter.FilterOperation;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
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
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
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
    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public ResponseEntity<Resource> retrieve(@RequestParam(required = false) final List<Long> ids, @RequestHeader(HttpHeaders.ACCEPT) final String contentType, final Locale locale) {
        try {
            final Pair<String, byte[]> export = exportService.doExport(ids, contentType, locale);
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"spex" + export.getFirst() + "\"")
                    .body(new ByteArrayResource(export.getSecond()));
        } catch (final Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not export spex", e);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public ResponseEntity<EntityModel<SpexDto>> create(@Valid @RequestBody final SpexCreateDto dto) {
        final SpexDto newDto = service.create(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, linkTo(methodOn(SpexApi.class).retrieve(newDto.getId())).toString())
                .body(EntityModel.of(newDto, getLinks(newDto, true)));
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public ResponseEntity<EntityModel<SpexDto>> retrieve(@PathVariable final Long id) {
        return service
                .findById(id)
                .map(dto -> EntityModel.of(dto, getLinks(dto)))
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('spexregister_ADMIN') or hasRole('spexregister_EDITOR')")
    public ResponseEntity<EntityModel<SpexDto>> update(@PathVariable final Long id, @Valid @RequestBody final SpexUpdateDto dto) {
        if (dto.getId() == null || !Objects.equals(id, dto.getId())) {
            return ResponseEntity.badRequest().build();
        }
        return service
                .update(dto)
                .map(updatedDto -> ResponseEntity.status(HttpStatus.ACCEPTED).body(EntityModel.of(updatedDto, getLinks(updatedDto))))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PatchMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('spexregister_ADMIN') or hasRole('spexregister_EDITOR')")
    public ResponseEntity<EntityModel<SpexDto>> partialUpdate(@PathVariable final Long id, @Valid @RequestBody final SpexUpdateDto dto) {
        if (dto.getId() == null || !Objects.equals(id, dto.getId())) {
            return ResponseEntity.badRequest().build();
        }
        return service
                .partialUpdate(dto)
                .map(updatedDto -> ResponseEntity.status(HttpStatus.ACCEPTED).body(EntityModel.of(updatedDto, getLinks(updatedDto))))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public ResponseEntity<?> delete(@PathVariable final Long id) {
        return service
                .findById(id)
                .map(dto -> {
                    service.deleteById(id);
                    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/poster")
    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public ResponseEntity<Resource> downloadPoster(@PathVariable final Long id) {
        return service.getPoster(id)
                .map(tuple -> {
                    final Resource resource = new ByteArrayResource(tuple.getFirst());
                    return ResponseEntity.ok()
                            .contentType(MediaType.valueOf(tuple.getSecond()))
                            .body(resource);
                })
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @RequestMapping(value = "/{id}/poster", method = {RequestMethod.POST, RequestMethod.PUT}, consumes = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_GIF_VALUE})
    @PreAuthorize("hasRole('spexregister_ADMIN') or hasRole('spexregister_EDITOR')")
    public ResponseEntity<?> uploadPoster(@PathVariable final Long id, @RequestBody final byte[] file, @RequestHeader(HttpHeaders.CONTENT_TYPE) final String contentType) {
        return service.savePoster(id, file, contentType)
                .map(entity -> ResponseEntity.status(HttpStatus.NO_CONTENT).build())
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @RequestMapping(value = "/{id}/poster", method = {RequestMethod.POST, RequestMethod.PUT}, consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('spexregister_ADMIN') or hasRole('spexregister_EDITOR')")
    public ResponseEntity<?> uploadPoster(@PathVariable final Long id, @RequestParam("file") final MultipartFile file) {
        try {
            return uploadPoster(id, file.getBytes(), file.getContentType());
        } catch (final IOException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not save poster for spex {}", id, e);
            }
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
    }

    @DeleteMapping("/{id}/poster")
    @PreAuthorize("hasRole('spexregister_ADMIN') or hasRole('spexregister_EDITOR')")
    public ResponseEntity<?> deletePoster(@PathVariable final Long id) {
        return service.deletePoster(id)
                .map(entity -> ResponseEntity.status(HttpStatus.NO_CONTENT).build())
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping(value = "/{spexId}/parent", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public ResponseEntity<EntityModel<SpexDto>> retrieveParent(@PathVariable final Long spexId) {
        try {
            return service
                    .findById(spexId)
                    .map(dto -> ResponseEntity.status(HttpStatus.OK).body(EntityModel.of(dto, getLinks(dto))))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.CONFLICT)); // Unreachable
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not retrieve parent for spex", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value = "/{spexId}/revivals/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public ResponseEntity<EntityModel<SpexDto>> retrieveRevival(@PathVariable final Long spexId, @PathVariable final Long id) {
        try {
            return service
                    .findRevivalById(spexId, id)
                    .map(dto -> ResponseEntity.status(HttpStatus.OK).body(EntityModel.of(dto, getLinks(dto))))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not retrieve parent for spex", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value = "/revivals", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public ResponseEntity<PagedModel<EntityModel<SpexDto>>> retrieveRevivals(@SortDefault(sort = Spex_.YEAR, direction = Sort.Direction.ASC) final Pageable pageable) {
        final PagedModel<EntityModel<SpexDto>> paged = pagedResourcesAssembler.toModel(service.findRevivals(pageable));
        paged.getContent().forEach(this::addLinks);

        return ResponseEntity.ok(paged);
    }

    @GetMapping(value = "/{spexId}/revivals", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public ResponseEntity<PagedModel<EntityModel<SpexDto>>> retrieveRevivalsByParent(@PathVariable final Long spexId,
                                                                                     @SortDefault(sort = Spex_.YEAR, direction = Sort.Direction.ASC) final Pageable pageable) {
        try {
            final PagedModel<EntityModel<SpexDto>> paged = pagedResourcesAssembler.toModel(service.findRevivalsByParent(spexId, pageable));
            paged.getContent().forEach(this::addLinks);

            return ResponseEntity.ok(paged);
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not retrieve revivals", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping(value = "/{spexId}/revivals/{year}", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR')")
    public ResponseEntity<EntityModel<SpexDto>> createRevival(@PathVariable final Long spexId, @PathVariable final String year) {
        try {
            return service
                    .addRevival(spexId, year)
                    .map(dto -> ResponseEntity
                            .status(HttpStatus.CREATED)
                            .header(HttpHeaders.LOCATION, linkTo(methodOn(SpexApi.class).retrieveRevival(spexId, dto.getId())).toString())
                            .body(EntityModel.of(dto, getLinks(dto)))
                    )
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.CONFLICT));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not create year {} for revivals for spex {}", year, spexId, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping(value = "/{spexId}/revivals/{year}", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR')")
    public ResponseEntity<?> deleteRevival(@PathVariable final Long spexId, @PathVariable final String year) {
        try {
            return service.deleteRevival(spexId, year) ? ResponseEntity.status(HttpStatus.NO_CONTENT).build() : ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not delete year {} from revivals for spex {}", year, spexId, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value = "/{spexId}/category", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public ResponseEntity<EntityModel<SpexCategoryDto>> retrieveCategory(@PathVariable final Long spexId) {
        try {
            return service
                    .findCategoryBySpex(spexId)
                    .map(dto -> ResponseEntity.status(HttpStatus.OK).body(EntityModel.of(dto, spexCategoryApi.getLinks(dto))))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not retrieve category for spex", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(value = "/{spexId}/category/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public ResponseEntity<?> addCategory(@PathVariable final Long spexId, @PathVariable final Long id) {
        try {
            return service.addCategory(spexId, id) ? ResponseEntity.status(HttpStatus.ACCEPTED).build() : ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not add category {} for spex {}", id, spexId, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping(value = "/{spexId}/category", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public ResponseEntity<?> removeCategory(@PathVariable final Long spexId) {
        try {
            return service.removeCategory(spexId) ? ResponseEntity.status(HttpStatus.NO_CONTENT).build() : ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not remove category for spex {}", spexId, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value = "/events", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public ResponseEntity<CollectionModel<EntityModel<EventDto>>> retrieveEvents(@RequestParam(defaultValue = "90") final Integer sinceInDays) {
        final List<EntityModel<EventDto>> events = eventService.findBySource(sinceInDays, Event.SourceType.SPEX).stream()
                .map(dto -> EntityModel.of(dto, eventApi.getLinks(dto)))
                .toList();

        return ResponseEntity.ok(
                CollectionModel.of(events,
                        linkTo(methodOn(EventApi.class).retrieve(null)).withSelfRel()));
    }

    private void addLinks(final EntityModel<SpexDto> entity) {
        if (entity != null && entity.getContent() != null) {
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
            links.add(linkTo(methodOn(SpexApi.class).retrieveEvents(null)).withRel("events"));
        }

        return links;
    }

}
