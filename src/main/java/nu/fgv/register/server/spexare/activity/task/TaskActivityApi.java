/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nu.fgv.register.server.spexare.activity.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.SpexareApi;
import nu.fgv.register.server.spexare.activity.ActivityApi;
import nu.fgv.register.server.spexare.activity.task.actor.ActorApi;
import nu.fgv.register.server.task.TaskApi;
import nu.fgv.register.server.task.TaskDto;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.SortDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
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

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/spexare/{spexareId}/activities/{activityId}/task-activities")
public class TaskActivityApi {

    private final TaskActivityService service;
    private final PagedResourcesAssembler<TaskActivityDto> pagedResourcesAssembler;
    private final TaskApi taskApi;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<PagedModel<EntityModel<TaskActivityDto>>> retrieve(@PathVariable final Long spexareId,
                                                                             @PathVariable final Long activityId,
                                                                             @SortDefault(sort = TaskActivity_.ID, direction = Sort.Direction.ASC) final Pageable pageable) {
        final PagedModel<EntityModel<TaskActivityDto>> paged = pagedResourcesAssembler.toModel(service.findByActivity(spexareId, activityId, pageable));

        paged.getContent().forEach(p -> addLinks(p, spexareId, activityId));

        return ResponseEntity.ok(paged);
    }

    @PostMapping(value = "/{taskId}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<TaskActivityDto>> create(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long taskId) {
        final TaskActivityDto dto = service.create(spexareId, activityId, taskId);

        return ResponseEntity.created(linkTo(methodOn(TaskActivityApi.class).retrieve(spexareId, activityId, dto.getId())).toUri())
                .body(EntityModel.of(dto, getLinks(dto, spexareId, activityId)));
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<TaskActivityDto>> retrieve(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long id) {
        final TaskActivityDto dto = service.findById(spexareId, activityId, id);

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto, spexareId, activityId)));
    }

    @PutMapping(value = "/{id}/{taskId}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<TaskActivityDto>> update(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long id, @PathVariable final Long taskId) {
        final TaskActivityDto dto = service.update(spexareId, activityId, taskId, id);

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto, spexareId, activityId)));
    }

    @DeleteMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<Object> delete(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long id) {
        service.deleteById(spexareId, activityId, id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{id}/task", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<TaskDto>> retrieveTask(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long id) {
        final TaskDto dto = service.findTaskByTaskActivity(spexareId, activityId, id);

        return ResponseEntity.ok(EntityModel.of(dto, taskApi.getLinks(dto, false)));
    }

    private void addLinks(final EntityModel<TaskActivityDto> entity, final Long spexareId, final Long activityId) {
        if (entity.getContent() != null) {
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
