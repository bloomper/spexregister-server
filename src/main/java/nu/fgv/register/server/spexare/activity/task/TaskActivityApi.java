package nu.fgv.register.server.spexare.activity.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.SpexareApi;
import nu.fgv.register.server.spexare.activity.ActivityApi;
import nu.fgv.register.server.spexare.activity.task.actor.ActorApi;
import nu.fgv.register.server.task.TaskApi;
import nu.fgv.register.server.task.TaskDto;
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
@RequestMapping("/api/v1/spexare/{spexareId}/activities/{activityId}/task-activities")
public class TaskActivityApi {

    private final TaskActivityService service;
    private final PagedResourcesAssembler<TaskActivityDto> pagedResourcesAssembler;
    private final TaskApi taskApi;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<TaskActivityDto>>> retrieve(@PathVariable final Long spexareId,
                                                                             @PathVariable final Long activityId,
                                                                             @SortDefault(sort = TaskActivity_.ID, direction = Sort.Direction.ASC) final Pageable pageable) {
        try {
            final PagedModel<EntityModel<TaskActivityDto>> paged = pagedResourcesAssembler.toModel(service.findByActivity(spexareId, activityId, pageable));
            paged.getContent().forEach(p -> addLinks(p, spexareId, activityId));

            return ResponseEntity.ok(paged);
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not retrieve task activities", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<TaskActivityDto>> retrieve(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long id) {
        try {
            return service
                    .findById(spexareId, activityId, id)
                    .map(dto -> EntityModel.of(dto, getLinks(dto, spexareId, activityId)))
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not retrieve task activity", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping(value = "/{taskId}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<TaskActivityDto>> create(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long taskId) {
        try {
            return service
                    .create(spexareId, activityId, taskId)
                    .map(dto -> ResponseEntity
                            .status(HttpStatus.CREATED)
                            .header(HttpHeaders.LOCATION, linkTo(methodOn(TaskActivityApi.class).retrieve(spexareId, activityId, dto.getId())).toString())
                            .body(EntityModel.of(dto, getLinks(dto, spexareId, activityId)))
                    )
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.CONFLICT));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not create task activity for activity {}, spexare {} and task {}", activityId, spexareId, taskId, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(value = "/{id}/{taskId}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<?> update(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long taskId, @PathVariable final Long id) {
        try {
            return service.update(spexareId, activityId, taskId, id) ? ResponseEntity.status(HttpStatus.ACCEPTED).build() : ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not update task activity {} for activity {} and spexare {}", id, activityId, spexareId, e);
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
                log.error("Could not delete task activity {} for activity {} and spexare {}", id, activityId, spexareId, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value = "/{id}/task", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<TaskDto>> retrieveTask(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long id) {
        try {
            return service
                    .findTaskByTaskActivity(spexareId, activityId, id)
                    .map(dto -> ResponseEntity.status(HttpStatus.OK).body(EntityModel.of(dto, taskApi.getLinks(dto, false))))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not retrieve spex for spex activity", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    private void addLinks(final EntityModel<TaskActivityDto> entity, final Long spexareId, final Long activityId) {
        if (entity != null && entity.getContent() != null) {
            entity.getContent().add(getLinks(entity.getContent(), spexareId, activityId));
        }
    }

    void addLinks(final TaskActivityDto dto, final Long spexareId, final Long activityId) {
        dto.add(getLinks(dto, spexareId, activityId));
    }

    List<Link> getLinks(final TaskActivityDto dto, final Long spexareId, final Long activityId) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(TaskActivityApi.class).retrieve(spexareId, activityId, dto.getId())).withSelfRel());
        links.add(linkTo(methodOn(TaskActivityApi.class).retrieveTask(spexareId, activityId, dto.getId())).withRel("task"));
        links.add(linkTo(methodOn(TaskActivityApi.class).retrieve(spexareId, activityId, Pageable.unpaged())).withRel("task-activities"));
        links.add(linkTo(methodOn(ActorApi.class).retrieve(spexareId, activityId, dto.getId(), Pageable.unpaged(), "")).withRel("actors"));
        links.add(linkTo(methodOn(ActivityApi.class).retrieve(spexareId, Pageable.unpaged())).withRel("activities"));
        links.add(linkTo(methodOn(SpexareApi.class).retrieve(spexareId)).withRel("spexare"));

        return links;
    }

}
