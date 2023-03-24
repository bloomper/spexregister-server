package nu.fgv.register.server.spexare.toggle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spex.SpexApi;
import nu.fgv.register.server.spexare.SpexareApi;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.SortDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/spexare/{spexareId}/toggles")
public class ToggleApi {

    private final ToggleService service;
    private final PagedResourcesAssembler<ToggleDto> pagedResourcesAssembler;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<ToggleDto>>> retrieve(@PathVariable final Long spexareId, @SortDefault(sort = "type", direction = Sort.Direction.ASC) final Pageable pageable) {
        try {
            final PagedModel<EntityModel<ToggleDto>> paged = pagedResourcesAssembler.toModel(service.findBySpexare(spexareId, pageable));
            paged.getContent().forEach(p -> addLinks(p, spexareId));

            return ResponseEntity.ok(paged);
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not retrieve toggles", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<ToggleDto>> retrieve(@PathVariable final Long spexareId, @PathVariable final Long id) {
        try {
            return service
                    .findById(spexareId, id)
                    .map(dto -> EntityModel.of(dto, getLinks(dto, spexareId)))
                    .map(ResponseEntity::ok)
                    .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not retrieve toggle", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping(value = "/{typeId}/{value}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<ToggleDto>> create(@PathVariable final Long spexareId, @PathVariable final String typeId, @PathVariable final Boolean value) {
        try {
            return service
                    .create(spexareId, typeId, value)
                    .map(dto -> ResponseEntity
                            .status(HttpStatus.CREATED)
                            .header(HttpHeaders.LOCATION, linkTo(methodOn(ToggleApi.class).retrieve(spexareId, dto.getId())).toString())
                            .body(EntityModel.of(dto, getLinks(dto, spexareId)))
                    )
                    .orElse(new ResponseEntity<>(HttpStatus.CONFLICT));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not create toggle", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(value = "/{typeId}/{id}/{value}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<ToggleDto>> update(@PathVariable final Long spexareId, @PathVariable final String typeId, @PathVariable final Long id, @PathVariable final Boolean value) {
        try {
            return service
                    .update(spexareId, typeId, id, value)
                    .map(dto -> ResponseEntity.status(HttpStatus.ACCEPTED).body(EntityModel.of(dto, getLinks(dto, spexareId))))
                    .orElse(new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not update toggle {}", id, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping(value = "/{typeId}/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<?> delete(@PathVariable final Long spexareId, @PathVariable final String typeId, @PathVariable final Long id) {
        try {
            return service.deleteById(spexareId, typeId, id) ? ResponseEntity.status(HttpStatus.NO_CONTENT).build() : ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not delete toggle {}", id, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    private void addLinks(final EntityModel<ToggleDto> entity, final Long spexareId) {
        if (entity != null && entity.getContent() != null) {
            entity.getContent().add(getLinks(entity.getContent(), spexareId));
        }
    }

    void addLinks(final ToggleDto dto, final Long spexareId) {
        dto.add(getLinks(dto, spexareId));
    }

    List<Link> getLinks(final ToggleDto dto, final Long spexareId) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(ToggleApi.class).retrieve(spexareId, dto.getId())).withSelfRel());
        links.add(linkTo(methodOn(ToggleApi.class).retrieve(spexareId, Pageable.unpaged())).withRel("toggles"));
        links.add(linkTo(methodOn(SpexareApi.class).retrieve(spexareId)).withRel("spexare"));

        return links;
    }

}
