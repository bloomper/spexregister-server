package nu.fgv.register.server.spexare.consent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.settings.TypeType;
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
import org.springframework.web.bind.annotation.PathVariable;
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
@RequestMapping("/api/v1/spexare/{spexareId}/consents")
public class ConsentApi {

    private final ConsentService service;
    private final PagedResourcesAssembler<ConsentDto> pagedResourcesAssembler;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<ConsentDto>>> retrieveConsents(@PathVariable final Long spexareId, @SortDefault(sort = "type", direction = Sort.Direction.ASC) final Pageable pageable) {
        try {
            final PagedModel<EntityModel<ConsentDto>> paged = pagedResourcesAssembler.toModel(service.findBySpexare(spexareId, pageable));
            paged.getContent().forEach(p -> addLinks(p, spexareId));

            return ResponseEntity.ok(paged);
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not retrieve consents", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<ConsentDto>> retrieve(@PathVariable final Long spexareId, @PathVariable final Long id) {
        return service
                .findById(id)
                .map(dto -> EntityModel.of(dto, getLinks(dto, spexareId)))
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping(value = "/{typeId}/{value}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<ConsentDto>> addConsent(@PathVariable final Long spexareId, @PathVariable final String typeId, @PathVariable final Boolean value) {
        try {
            return service
                    .addConsent(spexareId, typeId, value)
                    .map(dto -> ResponseEntity.status(HttpStatus.ACCEPTED).body(EntityModel.of(dto, getLinks(dto, spexareId))))
                    .orElse(new ResponseEntity<>(HttpStatus.CONFLICT));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not add value to consents", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(value = "/{typeId}/{id}/{value}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<ConsentDto>> updateConsent(@PathVariable final Long spexareId, @PathVariable final String typeId, @PathVariable final Long id, @PathVariable final Boolean value) {
        try {
            return service
                    .updateConsent(spexareId, typeId, id, value)
                    .map(dto -> ResponseEntity.status(HttpStatus.ACCEPTED).body(EntityModel.of(dto, getLinks(dto, spexareId))))
                    .orElse(new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not update value in consents", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping(value = "/{typeId}/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<?> removeConsent(@PathVariable final Long spexareId, @PathVariable final String typeId, @PathVariable final Long id) {
        try {
            return service.removeConsent(spexareId, typeId, id) ? ResponseEntity.status(HttpStatus.NO_CONTENT).build() : ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not remove value from consents", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    private void addLinks(final EntityModel<ConsentDto> entity, final Long spexareId) {
        if (entity != null && entity.getContent() != null) {
            entity.getContent().add(getLinks(entity.getContent(), spexareId));
        }
    }

    void addLinks(final ConsentDto dto, final Long spexareId) {
        dto.add(getLinks(dto, spexareId));
    }

    List<Link> getLinks(final ConsentDto dto, final Long spexareId) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(ConsentApi.class).retrieve(spexareId, dto.getId())).withSelfRel());

        final Link consentsLink = linkTo(methodOn(ConsentApi.class).retrieveConsents(dto.getId(), null)).withRel("consents");
        links.add(consentsLink);

        final Link spexareLink = linkTo(methodOn(SpexareApi.class).retrieve(spexareId)).withRel("spexare");
        links.add(spexareLink);

        return links;
    }

}
