package nu.fgv.register.server.spexare.activity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.SpexareApi;
import nu.fgv.register.server.spexare.activity.spex.SpexActivityApi;
import nu.fgv.register.server.spexare.activity.task.TaskActivityApi;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/spexare/{spexareId}/activities")
public class ActivityApi {

    private final ActivityService service;
    private final PagedResourcesAssembler<ActivityDto> pagedResourcesAssembler;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<ActivityDto>>> retrieve(@PathVariable final Long spexareId,
                                                                         @SortDefault(sort = Activity_.ID, direction = Sort.Direction.ASC) final Pageable pageable) {
        try {
            final PagedModel<EntityModel<ActivityDto>> paged = pagedResourcesAssembler.toModel(service.findBySpexare(spexareId, pageable));
            paged.getContent().forEach(p -> addLinks(p, spexareId));

            return ResponseEntity.ok(paged);
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not retrieve activities", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<ActivityDto>> retrieve(@PathVariable final Long spexareId, @PathVariable final Long id) {
        try {
            return service
                    .findById(spexareId, id)
                    .map(dto -> EntityModel.of(dto, getLinks(dto, spexareId)))
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not retrieve activity", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping(value = "", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<ActivityDto>> create(@PathVariable final Long spexareId) {
        try {
            return service
                    .create(spexareId)
                    .map(dto -> ResponseEntity
                            .status(HttpStatus.CREATED)
                            .header(HttpHeaders.LOCATION, linkTo(methodOn(ActivityApi.class).retrieve(spexareId, dto.getId())).toString())
                            .body(EntityModel.of(dto, getLinks(dto, spexareId)))
                    )
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.CONFLICT)); // Unreachable
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not create activity for spexare {}", spexareId, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<?> delete(@PathVariable final Long spexareId, @PathVariable final Long id) {
        try {
            return service.deleteById(spexareId, id) ? ResponseEntity.status(HttpStatus.NO_CONTENT).build() : ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not delete activity {} for spexare {}", id, spexareId, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    private void addLinks(final EntityModel<ActivityDto> entity, final Long spexareId) {
        if (entity != null && entity.getContent() != null) {
            entity.getContent().add(getLinks(entity.getContent(), spexareId));
        }
    }

    void addLinks(final ActivityDto dto, final Long spexareId) {
        dto.add(getLinks(dto, spexareId));
    }

    List<Link> getLinks(final ActivityDto dto, final Long spexareId) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(ActivityApi.class).retrieve(spexareId, dto.getId())).withSelfRel());
        links.add(linkTo(methodOn(ActivityApi.class).retrieve(spexareId, Pageable.unpaged())).withRel("activities"));
        links.add(linkTo(methodOn(SpexActivityApi.class).retrieve(spexareId, dto.getId(), Pageable.unpaged())).withRel("spex-activities"));
        links.add(linkTo(methodOn(TaskActivityApi.class).retrieve(spexareId, dto.getId(), Pageable.unpaged())).withRel("task-activities"));
        links.add(linkTo(methodOn(SpexareApi.class).retrieve(spexareId)).withRel("spexare"));

        return links;
    }

}
