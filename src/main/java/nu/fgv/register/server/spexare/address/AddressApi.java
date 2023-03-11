package nu.fgv.register.server.spexare.address;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/spexare/{spexareId}/addresses")
public class AddressApi {

    private final AddressService service;
    private final PagedResourcesAssembler<AddressDto> pagedResourcesAssembler;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<AddressDto>>> retrieve(@PathVariable final Long spexareId, @SortDefault(sort = "type", direction = Sort.Direction.ASC) final Pageable pageable) {
        try {
            final PagedModel<EntityModel<AddressDto>> paged = pagedResourcesAssembler.toModel(service.findBySpexare(spexareId, pageable));
            paged.getContent().forEach(p -> addLinks(p, spexareId));

            return ResponseEntity.ok(paged);
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not retrieve addresses", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<AddressDto>> retrieve(@PathVariable final Long spexareId, @PathVariable final Long id) {
        return service
                .findById(id)
                .map(dto -> EntityModel.of(dto, getLinks(dto, spexareId)))
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping(value = "/{typeId}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<AddressDto>> create(@PathVariable final Long spexareId, @PathVariable final String typeId, @Valid @RequestBody final AddressCreateDto dto) {
        try {
            return service
                    .create(spexareId, typeId, dto)
                    .map(newDto -> ResponseEntity.status(HttpStatus.ACCEPTED).body(EntityModel.of(newDto, getLinks(newDto, spexareId))))
                    .orElse(new ResponseEntity<>(HttpStatus.CONFLICT));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not create address", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(value = "/{typeId}/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<AddressDto>> update(@PathVariable final Long spexareId, @PathVariable final String typeId, @PathVariable final Long id, @Valid @RequestBody final AddressUpdateDto dto) {
        if (dto.getId() == null || !Objects.equals(id, dto.getId())) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return service
                    .update(spexareId, typeId, id, dto)
                    .map(updatedDto -> ResponseEntity.status(HttpStatus.ACCEPTED).body(EntityModel.of(updatedDto, getLinks(updatedDto, spexareId))))
                    .orElse(new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not update address {}", id, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PatchMapping(value = "/{typeId}/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<AddressDto>> partialUpdate(@PathVariable final Long spexareId, @PathVariable final String typeId, @PathVariable final Long id, @Valid @RequestBody final AddressUpdateDto dto) {
        if (dto.getId() == null || !Objects.equals(id, dto.getId())) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return service
                    .partialUpdate(spexareId, typeId, id, dto)
                    .map(updatedDto -> ResponseEntity.status(HttpStatus.ACCEPTED).body(EntityModel.of(updatedDto, getLinks(updatedDto, spexareId))))
                    .orElse(new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not update address {}", id, e);
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
                log.error("Could not delete address {}", id, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    private void addLinks(final EntityModel<AddressDto> entity, final Long spexareId) {
        if (entity != null && entity.getContent() != null) {
            entity.getContent().add(getLinks(entity.getContent(), spexareId));
        }
    }

    void addLinks(final AddressDto dto, final Long spexareId) {
        dto.add(getLinks(dto, spexareId));
    }

    List<Link> getLinks(final AddressDto dto, final Long spexareId) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(AddressApi.class).retrieve(spexareId, dto.getId())).withSelfRel());
        links.add(linkTo(methodOn(AddressApi.class).retrieve(dto.getId(), Pageable.unpaged())).withRel("addresses"));
        links.add(linkTo(methodOn(SpexareApi.class).retrieve(spexareId)).withRel("spexare"));

        return links;
    }

}
