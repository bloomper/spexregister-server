package nu.fgv.register.server.news;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventApi;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventService;
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
import org.springframework.security.access.prepost.PreAuthorize;
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
    public ResponseEntity<PagedModel<EntityModel<NewsDto>>> retrieve(@SortDefault(sort = News_.VISIBLE_FROM, direction = Sort.Direction.ASC) final Pageable pageable,
                                                                     @RequestParam(required = false, defaultValue = "") final String filter) {
        final PagedModel<EntityModel<NewsDto>> paged = pagedResourcesAssembler.toModel(service.find(filter, pageable));
        paged.getContent().forEach(this::addLinks);

        return ResponseEntity.ok(paged);
    }

    @PostMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR')")
    public ResponseEntity<EntityModel<NewsDto>> create(@Valid @RequestBody final NewsCreateDto dto) {
        final NewsDto newDto = service.create(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, linkTo(methodOn(NewsApi.class).retrieve(newDto.getId())).toString())
                .body(EntityModel.of(newDto, getLinks(newDto)));
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public ResponseEntity<EntityModel<NewsDto>> retrieve(@PathVariable final Long id) {
        return service
                .findById(id)
                .map(dto -> EntityModel.of(dto, getLinks(dto)))
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR')")
    public ResponseEntity<EntityModel<NewsDto>> update(@PathVariable Long id, @Valid @RequestBody NewsUpdateDto dto) {
        if (dto.getId() == null || !Objects.equals(id, dto.getId())) {
            return ResponseEntity.badRequest().build();
        }
        return service
                .update(dto)
                .map(updatedDto -> ResponseEntity.status(HttpStatus.ACCEPTED).body(EntityModel.of(updatedDto, getLinks(updatedDto))))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PatchMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR')")
    public ResponseEntity<EntityModel<NewsDto>> partialUpdate(@PathVariable final Long id, @Valid @RequestBody final NewsUpdateDto dto) {
        if (dto.getId() == null || !Objects.equals(id, dto.getId())) {
            return ResponseEntity.badRequest().build();
        }
        return service
                .partialUpdate(dto)
                .map(updatedDto -> ResponseEntity.status(HttpStatus.ACCEPTED).body(EntityModel.of(updatedDto, getLinks(updatedDto))))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR')")
    public ResponseEntity<?> delete(@PathVariable final Long id) {
        return service
                .findById(id)
                .map(dto -> {
                    service.deleteById(id);
                    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/events", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public ResponseEntity<CollectionModel<EntityModel<EventDto>>> retrieveEvents(@RequestParam(defaultValue = "90") final Integer sinceInDays) {
        final List<EntityModel<EventDto>> events = eventService.findBySource(sinceInDays, Event.SourceType.NEWS).stream()
                .map(dto -> EntityModel.of(dto, eventApi.getLinks(dto)))
                .toList();

        return ResponseEntity.ok(
                CollectionModel.of(events,
                        linkTo(methodOn(EventApi.class).retrieve(null)).withSelfRel()));
    }

    private void addLinks(final EntityModel<NewsDto> entity) {
        if (entity != null && entity.getContent() != null) {
            entity.getContent().add(getLinks(entity.getContent()));
        }
    }

    List<Link> getLinks(final NewsDto dto) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(NewsApi.class).retrieve(dto.getId())).withSelfRel());
        links.add(linkTo(methodOn(NewsApi.class).retrieve(Pageable.unpaged(), "")).withRel("news"));
        links.add(linkTo(methodOn(NewsApi.class).retrieveEvents(null)).withRel("events"));

        return links;
    }

}
