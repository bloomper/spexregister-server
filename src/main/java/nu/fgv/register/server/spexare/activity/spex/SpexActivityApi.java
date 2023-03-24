package nu.fgv.register.server.spexare.activity.spex;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spex.SpexApi;
import nu.fgv.register.server.spex.SpexDto;
import nu.fgv.register.server.spexare.SpexareApi;
import nu.fgv.register.server.spexare.activity.ActivityApi;
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
@RequestMapping("/api/v1/spexare/{spexareId}/activities/{activityId}/spex-activities")
public class SpexActivityApi {

    private final SpexActivityService service;
    private final PagedResourcesAssembler<SpexActivityDto> pagedResourcesAssembler;
    private final SpexApi spexApi;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<SpexActivityDto>>> retrieve(@PathVariable final Long spexareId, @PathVariable final Long activityId, @SortDefault(sort = "id", direction = Sort.Direction.ASC) final Pageable pageable) {
        try {
            final PagedModel<EntityModel<SpexActivityDto>> paged = pagedResourcesAssembler.toModel(service.findByActivity(spexareId, activityId, pageable));
            paged.getContent().forEach(p -> addLinks(p, spexareId, activityId));

            return ResponseEntity.ok(paged);
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not retrieve spex activities", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<SpexActivityDto>> retrieve(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long id) {
        try {
            return service
                    .findById(spexareId, activityId, id)
                    .map(dto -> EntityModel.of(dto, getLinks(dto, spexareId, activityId)))
                    .map(ResponseEntity::ok)
                    .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not retrieve spex activity", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping(value = "/{spexId}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<SpexActivityDto>> create(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long spexId) {
        try {
            return service
                    .create(spexareId, activityId, spexId)
                    .map(dto -> ResponseEntity
                            .status(HttpStatus.CREATED)
                            .header(HttpHeaders.LOCATION, linkTo(methodOn(SpexActivityApi.class).retrieve(spexareId, activityId, dto.getId())).toString())
                            .body(EntityModel.of(dto, getLinks(dto, spexareId, activityId)))
                    )
                    .orElse(new ResponseEntity<>(HttpStatus.CONFLICT));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not create spex activity", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(value = "/{id}/{spexId}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<?> update(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long spexId, @PathVariable final Long id) {
        try {
            return service.update(spexareId, activityId, spexId, id) ? ResponseEntity.status(HttpStatus.ACCEPTED).build() : ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not update spex activity", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<?> delete(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long id) {
        try {
            return service.deleteById(spexareId, activityId, id) ? ResponseEntity.status(HttpStatus.NO_CONTENT).build() : ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not delete spex activity {}", id, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value = "/{id}/spex", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<SpexDto>> retrieveSpex(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long id) {
        try {
            return service
                    .findSpexBySpexActivity(spexareId, activityId, id)
                    .map(dto -> ResponseEntity.status(HttpStatus.OK).body(EntityModel.of(dto, spexApi.getLinks(dto))))
                    .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not retrieve spex for spex activity", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    private void addLinks(final EntityModel<SpexActivityDto> entity, final Long spexareId, final Long activityId) {
        if (entity != null && entity.getContent() != null) {
            entity.getContent().add(getLinks(entity.getContent(), spexareId, activityId));
        }
    }

    void addLinks(final SpexActivityDto dto, final Long spexareId, final Long activityId) {
        dto.add(getLinks(dto, spexareId, activityId));
    }

    List<Link> getLinks(final SpexActivityDto dto, final Long spexareId, final Long activityId) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(SpexActivityApi.class).retrieve(spexareId, activityId, dto.getId())).withSelfRel());
        links.add(linkTo(methodOn(SpexActivityApi.class).retrieveSpex(spexareId, activityId, dto.getId())).withRel("spex"));
        links.add(linkTo(methodOn(SpexActivityApi.class).retrieve(spexareId, activityId, Pageable.unpaged())).withRel("spex-activities"));
        links.add(linkTo(methodOn(ActivityApi.class).retrieve(spexareId, Pageable.unpaged())).withRel("activities"));
        links.add(linkTo(methodOn(SpexareApi.class).retrieve(spexareId)).withRel("spexare"));

        return links;
    }

}
