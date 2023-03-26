package nu.fgv.register.server.spexare.activity.task.actor;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.SpexareApi;
import nu.fgv.register.server.spexare.activity.ActivityApi;
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
@RequestMapping("/api/v1/spexare/{spexareId}/activities/{activityId}/task-activities/{taskActivityId}/actors")
public class ActorApi {

    private final ActorService service;
    private final PagedResourcesAssembler<ActorDto> pagedResourcesAssembler;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<ActorDto>>> retrieve(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long taskActivityId, @SortDefault(sort = "id", direction = Sort.Direction.ASC) final Pageable pageable) {
        try {
            final PagedModel<EntityModel<ActorDto>> paged = pagedResourcesAssembler.toModel(service.findByTaskActivity(spexareId, activityId, taskActivityId, pageable));
            paged.getContent().forEach(p -> addLinks(p, spexareId, activityId, taskActivityId));

            return ResponseEntity.ok(paged);
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not retrieve actors", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<ActorDto>> retrieve(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long taskActivityId, @PathVariable final Long id) {
        try {
            return service
                    .findById(spexareId, activityId, taskActivityId, id)
                    .map(dto -> EntityModel.of(dto, getLinks(dto, spexareId, activityId, taskActivityId)))
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not retrieve actor", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping(value = "/{vocalId}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<ActorDto>> create(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long taskActivityId, @PathVariable final String vocalId, @Valid @RequestBody final ActorCreateDto dto) {
        try {
            return service
                    .create(spexareId, activityId, taskActivityId, vocalId, dto)
                    .map(newDto -> ResponseEntity
                            .status(HttpStatus.CREATED)
                            .header(HttpHeaders.LOCATION, linkTo(methodOn(ActorApi.class).retrieve(spexareId, activityId, taskActivityId, newDto.getId())).toString())
                            .body(EntityModel.of(newDto, getLinks(newDto, spexareId, activityId, taskActivityId)))
                    )
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.CONFLICT));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not create actor for task activity {}, activity {} and spexare {}", taskActivityId, activityId, spexareId, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(value = "/{vocalId}/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<ActorDto>> update(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long taskActivityId, @PathVariable final String vocalId, @PathVariable final Long id, @Valid @RequestBody final ActorUpdateDto dto) {
        if (dto.getId() == null || !Objects.equals(id, dto.getId())) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return service
                    .update(spexareId, activityId, taskActivityId, vocalId, id, dto)
                    .map(updatedDto -> ResponseEntity.status(HttpStatus.ACCEPTED).body(EntityModel.of(updatedDto, getLinks(updatedDto, spexareId, activityId, taskActivityId))))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not update actor {} for task activity {}, activity {} and spexare {}", id, taskActivityId, activityId, spexareId, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PatchMapping(value = "/{vocalId}/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<ActorDto>> partialUpdate(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long taskActivityId, @PathVariable final String vocalId, @PathVariable final Long id, @Valid @RequestBody final ActorUpdateDto dto) {
        if (dto.getId() == null || !Objects.equals(id, dto.getId())) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return service
                    .partialUpdate(spexareId, activityId, taskActivityId, vocalId, id, dto)
                    .map(updatedDto -> ResponseEntity.status(HttpStatus.ACCEPTED).body(EntityModel.of(updatedDto, getLinks(updatedDto, spexareId, activityId, taskActivityId))))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not update actor {} for task activity {}, activity {} and spexare {}", id, taskActivityId, activityId, spexareId, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping(value = "/{vocalId}/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<?> delete(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long taskActivityId, @PathVariable final String vocalId, @PathVariable final Long id) {
        try {
            return service.deleteById(spexareId, activityId, taskActivityId, vocalId, id) ? ResponseEntity.status(HttpStatus.NO_CONTENT).build() : ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not delete actor {} for task activity {}, activity {} and spexare {}", id, taskActivityId, activityId, spexareId, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    private void addLinks(final EntityModel<ActorDto> entity, final Long spexareId, final Long activityId, final Long taskActivityId) {
        if (entity != null && entity.getContent() != null) {
            entity.getContent().add(getLinks(entity.getContent(), spexareId, activityId, taskActivityId));
        }
    }

    void addLinks(final ActorDto dto, final Long spexareId, final Long activityId, final Long taskActivityId) {
        dto.add(getLinks(dto, spexareId, activityId, taskActivityId));
    }

    List<Link> getLinks(final ActorDto dto, final Long spexareId, final Long activityId, final Long taskActivityId) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(ActorApi.class).retrieve(spexareId, activityId, taskActivityId, dto.getId())).withSelfRel());
        links.add(linkTo(methodOn(ActorApi.class).retrieve(spexareId, activityId, taskActivityId, Pageable.unpaged())).withRel("actors"));
        links.add(linkTo(methodOn(TaskActivityApi.class).retrieve(spexareId, activityId, Pageable.unpaged())).withRel("task-activities"));
        links.add(linkTo(methodOn(ActivityApi.class).retrieve(spexareId, Pageable.unpaged())).withRel("activities"));
        links.add(linkTo(methodOn(SpexareApi.class).retrieve(spexareId)).withRel("spexare"));

        return links;
    }

}
