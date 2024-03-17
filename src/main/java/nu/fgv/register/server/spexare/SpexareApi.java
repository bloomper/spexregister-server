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
import nu.fgv.register.server.spexare.tag.TaggingApi;
import nu.fgv.register.server.spexare.toggle.ToggleApi;
import nu.fgv.register.server.util.Constants;
import nu.fgv.register.server.util.search.PagedWithFacetsModel;
import nu.fgv.register.server.util.search.PagedWithFacetsResourcesAssembler;
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
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/spexare")
public class SpexareApi {

    private final SpexareService service;
    private final SpexareExportService exportService;
    private final EventService eventService;
    private final PagedResourcesAssembler<SpexareDto> pagedResourcesAssembler;
    private final PagedWithFacetsResourcesAssembler<SpexareDto> pagedWithFacetsResourcesAssembler;
    private final EventApi eventApi;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE, params = {"!q"})
    public ResponseEntity<PagedModel<EntityModel<SpexareDto>>> retrieve(@SortDefault(sort = Spexare_.FIRST_NAME, direction = Sort.Direction.ASC) final Pageable pageable,
                                                                        @RequestParam(required = false, defaultValue = "") final String filter) {
        final PagedModel<EntityModel<SpexareDto>> paged = pagedResourcesAssembler.toModel(service.find(filter, pageable));
        paged.getContent().forEach(this::addLinks);

        return ResponseEntity.ok(paged);
    }

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE, params = {"q"})
    public ResponseEntity<PagedWithFacetsModel<EntityModel<SpexareDto>>> search(@RequestParam final String q,
                                                                                @SortDefault(sort = "score", direction = Sort.Direction.ASC) final Pageable pageable) {
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
    public ResponseEntity<Resource> retrieve(@RequestParam(required = false) final List<Long> ids, @RequestHeader(HttpHeaders.ACCEPT) final String contentType, final Locale locale) {
        try {
            final Pair<String, byte[]> export = exportService.doExport(ids, contentType, locale);
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"spexare" + export.getFirst() + "\"")
                    .body(new ByteArrayResource(export.getSecond()));
        } catch (final Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not export spexare", e);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<SpexareDto>> create(@Valid @RequestBody final SpexareCreateDto dto) {
        final SpexareDto newDto = service.create(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, linkTo(methodOn(SpexareApi.class).retrieve(newDto.getId())).toString())
                .body(EntityModel.of(newDto, getLinks(newDto)));
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<SpexareDto>> retrieve(@PathVariable final Long id) {
        return service
                .findById(id)
                .map(dto -> EntityModel.of(dto, getLinks(dto)))
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<SpexareDto>> update(@PathVariable final Long id, @Valid @RequestBody final SpexareUpdateDto dto) {
        if (dto.getId() == null || !Objects.equals(id, dto.getId())) {
            return ResponseEntity.badRequest().build();
        }
        return service
                .update(dto)
                .map(updatedDto -> ResponseEntity.status(HttpStatus.ACCEPTED).body(EntityModel.of(updatedDto, getLinks(updatedDto))))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PatchMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<SpexareDto>> partialUpdate(@PathVariable final Long id, @Valid @RequestBody final SpexareUpdateDto dto) {
        if (dto.getId() == null || !Objects.equals(id, dto.getId())) {
            return ResponseEntity.badRequest().build();
        }
        return service
                .partialUpdate(dto)
                .map(updatedDto -> ResponseEntity.status(HttpStatus.ACCEPTED).body(EntityModel.of(updatedDto, getLinks(updatedDto))))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable final Long id) {
        return service
                .findById(id)
                .map(dto -> {
                    service.deleteById(id);
                    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> downloadImage(@PathVariable final Long id) {
        return service.getImage(id)
                .map(tuple -> {
                    final Resource resource = new ByteArrayResource(tuple.getFirst());
                    return ResponseEntity.ok()
                            .contentType(MediaType.valueOf(tuple.getSecond()))
                            .body(resource);
                })
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @RequestMapping(value = "/{id}/image", method = {RequestMethod.POST, RequestMethod.PUT}, consumes = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_GIF_VALUE})
    public ResponseEntity<?> uploadImage(@PathVariable final Long id, @RequestBody final byte[] file, @RequestHeader(HttpHeaders.CONTENT_TYPE) final String contentType) {
        return service.saveImage(id, file, contentType)
                .map(entity -> ResponseEntity.status(HttpStatus.NO_CONTENT).build())
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @RequestMapping(value = "/{id}/image", method = {RequestMethod.POST, RequestMethod.PUT}, consumes = {"multipart/form-data"})
    public ResponseEntity<?> uploadImage(@PathVariable final Long id, @RequestParam("file") final MultipartFile file) {
        try {
            return uploadImage(id, file.getBytes(), file.getContentType());
        } catch (final IOException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not save image for spexare {}", id, e);
            }
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
    }

    @DeleteMapping("/{id}/image")
    public ResponseEntity<?> deleteImage(@PathVariable final Long id) {
        return service.deleteImage(id)
                .map(entity -> ResponseEntity.status(HttpStatus.NO_CONTENT).build())
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping(value = "/{spexareId}/partner", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<SpexareDto>> retrievePartner(@PathVariable final Long spexareId) {
        try {
            return service
                    .findPartnerBySpexare(spexareId)
                    .map(dto -> ResponseEntity.status(HttpStatus.OK).body(EntityModel.of(dto, getLinks(dto))))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not retrieve partner for spexare", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(value = "/{spexareId}/partner/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<SpexareDto>> updatePartner(@PathVariable final Long spexareId, @PathVariable final Long id) {
        try {
            return service
                    .updatePartner(spexareId, id)
                    .map(dto -> ResponseEntity.status(HttpStatus.ACCEPTED).body(EntityModel.of(dto, getLinks(dto))))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.CONFLICT)); // Unreachable
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not update partner for spexare {}", spexareId, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping(value = "/{spexareId}/partner", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<?> deletePartner(@PathVariable final Long spexareId) {
        try {
            return service.deletePartner(spexareId) ? ResponseEntity.status(HttpStatus.NO_CONTENT).build() : ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not delete partner for spexare {}", spexareId, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value = "/events", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public ResponseEntity<CollectionModel<EntityModel<EventDto>>> retrieveEvents(@RequestParam(defaultValue = "90") final Integer sinceInDays) {
        final List<EntityModel<EventDto>> events = eventService.findBySource(sinceInDays, Event.SourceType.SPEXARE).stream()
                .map(dto -> EntityModel.of(dto, eventApi.getLinks(dto)))
                .toList();

        return ResponseEntity.ok(
                CollectionModel.of(events,
                        linkTo(methodOn(EventApi.class).retrieve(null)).withSelfRel()));
    }

    private void addLinks(final EntityModel<SpexareDto> entity) {
        if (entity != null && entity.getContent() != null) {
            entity.getContent().add(getLinks(entity.getContent()));
        }
    }

    void addLinks(final SpexareDto dto) {
        dto.add(getLinks(dto));
    }

    public List<Link> getLinks(final SpexareDto dto) {
        final List<Link> links = new ArrayList<>();
        links.add(linkTo(methodOn(SpexareApi.class).retrieve(dto.getId())).withSelfRel());
        if (hasText(dto.getImage())) {
            links.add(Link.of(dto.getImage()).withRel("image"));
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
        links.add(linkTo(methodOn(SpexareApi.class).retrieveEvents(null)).withRel("events"));

        return links;
    }

}
