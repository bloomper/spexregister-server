package nu.fgv.register.server.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventApi;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventService;
import nu.fgv.register.server.spexare.SpexareApi;
import nu.fgv.register.server.spexare.SpexareDto;
import nu.fgv.register.server.user.authority.AuthorityApi;
import nu.fgv.register.server.user.authority.AuthorityDto;
import nu.fgv.register.server.user.state.StateApi;
import nu.fgv.register.server.user.state.StateDto;
import nu.fgv.register.server.util.ResourceAlreadyExistsException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserApi {

    private final UserService service;
    private final EventService eventService;
    private final PagedResourcesAssembler<UserDto> pagedResourcesAssembler;
    private final AuthorityApi authorityApi;
    private final StateApi stateApi;
    private final SpexareApi spexareApi;
    private final EventApi eventApi;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public ResponseEntity<PagedModel<EntityModel<UserDto>>> retrieve(@SortDefault(sort = User_.ID, direction = Sort.Direction.ASC) final Pageable pageable,
                                                                     @RequestParam(required = false, defaultValue = "") final String filter) {
        final PagedModel<EntityModel<UserDto>> paged = pagedResourcesAssembler.toModel(service.find(filter, pageable));
        paged.getContent().forEach(this::addLinks);

        return ResponseEntity.ok(paged);
    }

    @PostMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public ResponseEntity<EntityModel<UserDto>> create(@Valid @RequestBody final UserCreateDto dto) {
        try {
            return service.create(dto)
                    .map(newDto -> ResponseEntity
                            .status(HttpStatus.CREATED)
                            .header(HttpHeaders.LOCATION, linkTo(methodOn(UserApi.class).retrieve(newDto.getId())).toString())
                            .body(EntityModel.of(newDto, getLinks(newDto))))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY));
        } catch(final ResourceAlreadyExistsException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not create user", e);
            }
            throw e;
        }
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public ResponseEntity<EntityModel<UserDto>> retrieve(@PathVariable final Long id) {
        return service
                .findById(id)
                .map(dto -> EntityModel.of(dto, getLinks(dto)))
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public ResponseEntity<EntityModel<UserDto>> update(@PathVariable final Long id, @Valid @RequestBody final UserUpdateDto dto) {
        if (dto.getId() == null || !Objects.equals(id, dto.getId())) {
            return ResponseEntity.badRequest().build();
        }
        return service
                .update(dto)
                .map(updatedDto -> ResponseEntity.status(HttpStatus.ACCEPTED).body(EntityModel.of(updatedDto, getLinks(updatedDto))))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PatchMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public ResponseEntity<EntityModel<UserDto>> partialUpdate(@PathVariable final Long id, @Valid @RequestBody final UserUpdateDto dto) {
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

    @GetMapping(value = "/{userId}/authorities", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public ResponseEntity<CollectionModel<EntityModel<AuthorityDto>>> retrieveAuthorities(@PathVariable final Long userId) {
        try {
            final Set<EntityModel<AuthorityDto>> authorities = service.getAuthoritiesByUser(userId).stream()
                    .map(dto -> EntityModel.of(dto, authorityApi.getLinks(dto)))
                    .collect(Collectors.toSet());

            return ResponseEntity.ok(
                    CollectionModel.of(authorities,
                            linkTo(methodOn(AuthorityApi.class).retrieve(Sort.unsorted())).withSelfRel()));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not retrieve authorities for user", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(value = "/{userId}/authorities/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public ResponseEntity<Serializable> addAuthority(@PathVariable final Long userId, @PathVariable final String id) {
        try {
            return service.addAuthority(userId, id) ? ResponseEntity.status(HttpStatus.ACCEPTED).build() : ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not add authority {} for user {}", id, userId, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(value = "/{userId}/authorities", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public ResponseEntity<Serializable> addAuthorities(@PathVariable final Long userId, @RequestParam final List<String> ids) {
        try {
            return service.addAuthorities(userId, ids) ? ResponseEntity.status(HttpStatus.ACCEPTED).build() : ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not add authorities {} for user {}", String.join(",", ids), userId, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping(value = "/{userId}/authorities/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public ResponseEntity<Serializable> removeAuthority(@PathVariable final Long userId, @PathVariable final String id) {
        try {
            return service.removeAuthority(userId, id) ? ResponseEntity.status(HttpStatus.NO_CONTENT).build() : ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not remove authority {} for user {}", id, userId, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping(value = "/{userId}/authorities", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<Serializable> removeAuthorities(@PathVariable final Long userId, @RequestParam final List<String> ids) {
        try {
            return service.removeAuthorities(userId, ids) ? ResponseEntity.status(HttpStatus.NO_CONTENT).build() : ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not remove authorities {} for user {}", String.join(",", ids), userId, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value = "/{userId}/state", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public ResponseEntity<EntityModel<StateDto>> retrieveState(@PathVariable final Long userId) {
        try {
            return Optional.of(service.getStateByUser(userId))
                    .map(dto -> ResponseEntity.status(HttpStatus.OK).body(EntityModel.of(dto, stateApi.getLinks(dto))))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not retrieve state for user {}", userId, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(value = "/{userId}/state/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public ResponseEntity<Serializable> setState(@PathVariable final Long userId, @PathVariable final String id) {
        try {
            return service.setState(userId, id) ? ResponseEntity.status(HttpStatus.ACCEPTED).build() : ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not set state {} for user {}", id, userId, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value = "/{userId}/spexare", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public ResponseEntity<EntityModel<SpexareDto>> retrieveSpexare(@PathVariable final Long userId) {
        try {
            return service.findSpexareByUser(userId)
                    .map(dto -> ResponseEntity.status(HttpStatus.OK).body(EntityModel.of(dto, spexareApi.getLinks(dto))))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not retrieve spexare for user {}", userId, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(value = "/{userId}/spexare/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public ResponseEntity<Serializable> addSpexare(@PathVariable final Long userId, @PathVariable final Long id) {
        try {
            return service.addSpexare(userId, id) ? ResponseEntity.status(HttpStatus.ACCEPTED).build() : ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not add spexare {} for user {}", id, userId, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping(value = "/{userId}/spexare", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public ResponseEntity<Serializable> removeSpexare(@PathVariable final Long userId) {
        try {
            return service.removeSpexare(userId) ? ResponseEntity.status(HttpStatus.NO_CONTENT).build() : ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not remove spexare for user {}", userId, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value = "/events", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public ResponseEntity<CollectionModel<EntityModel<EventDto>>> retrieveEvents(@RequestParam(defaultValue = "90") final Integer sinceInDays) {
        final List<EntityModel<EventDto>> events = eventService.findBySource(sinceInDays, Event.SourceType.USER).stream()
                .map(dto -> EntityModel.of(dto, eventApi.getLinks(dto)))
                .toList();

        return ResponseEntity.ok(
                CollectionModel.of(events,
                        linkTo(methodOn(EventApi.class).retrieve(null)).withSelfRel()));
    }

    private void addLinks(final EntityModel<UserDto> entity) {
        if (entity != null && entity.getContent() != null) {
            entity.getContent().add(getLinks(entity.getContent()));
        }
    }

    void addLinks(final UserDto dto) {
        dto.add(getLinks(dto));
    }

    public List<Link> getLinks(final UserDto dto) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(UserApi.class).retrieve(dto.getId())).withSelfRel());
        links.add(linkTo(methodOn(UserApi.class).retrieve(Pageable.unpaged(), "")).withRel("users"));
        links.add(linkTo(methodOn(UserApi.class).retrieveSpexare(dto.getId())).withRel("spexare"));
        links.add(linkTo(methodOn(UserApi.class).retrieveState(dto.getId())).withRel("state"));
        links.add(linkTo(methodOn(UserApi.class).retrieveAuthorities(dto.getId())).withRel("authorities"));
        links.add(linkTo(methodOn(UserApi.class).retrieveEvents(null)).withRel("events"));

        return links;
    }

}
