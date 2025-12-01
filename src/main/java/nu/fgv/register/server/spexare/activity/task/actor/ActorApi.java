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

package nu.fgv.register.server.spexare.activity.task.actor;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.SpexareApi;
import nu.fgv.register.server.spexare.activity.ActivityApi;
import nu.fgv.register.server.spexare.activity.task.TaskActivityApi;
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

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/spexare/{spexareId}/activities/{activityId}/task-activities/{taskActivityId}/actors")
public class ActorApi {

    private final ActorService service;
    private final PagedResourcesAssembler<ActorDto> pagedResourcesAssembler;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<PagedModel<EntityModel<ActorDto>>> retrieve(@PathVariable final Long spexareId,
                                                                      @PathVariable final Long activityId,
                                                                      @PathVariable final Long taskActivityId,
                                                                      @SortDefault(sort = Actor_.ID, direction = Sort.Direction.ASC) final Pageable pageable,
                                                                      @RequestParam(required = false, defaultValue = "") final String filter) {
        final PagedModel<EntityModel<ActorDto>> paged = pagedResourcesAssembler.toModel(service.findByTaskActivity(spexareId, activityId, taskActivityId, filter, pageable));

        paged.getContent().forEach(p -> addLinks(p, spexareId, activityId, taskActivityId));

        return ResponseEntity.ok(paged);
    }

    @PostMapping(value = "/{vocalId}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<ActorDto>> create(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long taskActivityId, @PathVariable final String vocalId, @Valid @RequestBody final ActorCreateDto dto) {
        final ActorDto createdDto = service.create(spexareId, activityId, taskActivityId, vocalId, dto);

        return ResponseEntity.created(linkTo(methodOn(ActorApi.class).retrieve(spexareId, activityId, taskActivityId, createdDto.getId())).toUri())
                .body(EntityModel.of(createdDto, getLinks(createdDto, spexareId, activityId, taskActivityId)));
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<ActorDto>> retrieve(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long taskActivityId, @PathVariable final Long id) {
        final ActorDto dto = service.findById(spexareId, activityId, taskActivityId, id);

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto, spexareId, activityId, taskActivityId)));
    }

    @PutMapping(value = "/{vocalId}/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<ActorDto>> update(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long taskActivityId, @PathVariable final String vocalId, @PathVariable final Long id, @Valid @RequestBody final ActorUpdateDto dto) {
        if (!Objects.equals(id, dto.id())) {
            return ResponseEntity.badRequest().build();
        }

        final ActorDto updatedDto = service.update(spexareId, activityId, taskActivityId, vocalId, id, dto);

        return ResponseEntity.ok(EntityModel.of(updatedDto, getLinks(updatedDto, spexareId, activityId, taskActivityId)));
    }

    @PatchMapping(value = "/{vocalId}/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<ActorDto>> partialUpdate(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long taskActivityId, @PathVariable final String vocalId, @PathVariable final Long id, @Valid @RequestBody final ActorUpdateDto dto) {
        if (!Objects.equals(id, dto.id())) {
            return ResponseEntity.badRequest().build();
        }

        final ActorDto updatedDto = service.partialUpdate(spexareId, activityId, taskActivityId, vocalId, id, dto);

        return ResponseEntity.ok(EntityModel.of(updatedDto, getLinks(updatedDto, spexareId, activityId, taskActivityId)));
    }

    @DeleteMapping(value = "/{vocalId}/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<Object> delete(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long taskActivityId, @PathVariable final String vocalId, @PathVariable final Long id) {
        service.deleteById(spexareId, activityId, taskActivityId, vocalId, id);

        return ResponseEntity.noContent().build();
    }

    private void addLinks(final EntityModel<ActorDto> entity, final Long spexareId, final Long activityId, final Long taskActivityId) {
        if (entity.getContent() != null) {
            entity.getContent().add(getLinks(entity.getContent(), spexareId, activityId, taskActivityId));
        }
    }

    void addLinks(final ActorDto dto, final Long spexareId, final Long activityId, final Long taskActivityId) {
        dto.add(getLinks(dto, spexareId, activityId, taskActivityId));
    }

    List<Link> getLinks(final ActorDto dto, final Long spexareId, final Long activityId, final Long taskActivityId) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(ActorApi.class).retrieve(spexareId, activityId, taskActivityId, dto.getId())).withSelfRel());
        links.add(linkTo(methodOn(ActorApi.class).retrieve(spexareId, activityId, taskActivityId, Pageable.unpaged(), "")).withRel("actors"));
        links.add(linkTo(methodOn(TaskActivityApi.class).retrieve(spexareId, activityId, Pageable.unpaged())).withRel("task-activities"));
        links.add(linkTo(methodOn(ActivityApi.class).retrieve(spexareId, Pageable.unpaged())).withRel("activities"));
        links.add(linkTo(methodOn(SpexareApi.class).retrieve(spexareId)).withRel("spexare"));

        return links;
    }

}
