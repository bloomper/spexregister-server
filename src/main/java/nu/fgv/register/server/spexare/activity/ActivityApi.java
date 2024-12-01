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

package nu.fgv.register.server.spexare.activity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.SpexareApi;
import nu.fgv.register.server.spexare.activity.spex.SpexActivityApi;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
@RequestMapping("/api/v1/spexare/{spexareId}/activities")
public class ActivityApi {

    private final ActivityService service;
    private final PagedResourcesAssembler<ActivityDto> pagedResourcesAssembler;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<PagedModel<EntityModel<ActivityDto>>> retrieve(@PathVariable final Long spexareId,
                                                                         @SortDefault(sort = Activity_.ID, direction = Sort.Direction.ASC) final Pageable pageable) {
        final PagedModel<EntityModel<ActivityDto>> paged = pagedResourcesAssembler.toModel(service.findBySpexare(spexareId, pageable));

        paged.getContent().forEach(p -> addLinks(p, spexareId));

        return ResponseEntity.ok(paged);
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<ActivityDto>> retrieve(@PathVariable final Long spexareId, @PathVariable final Long id) {
        final ActivityDto dto = service.findById(spexareId, id);

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto, spexareId)));
    }

    @PostMapping(value = "", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<ActivityDto>> create(@PathVariable final Long spexareId) {
        final ActivityDto dto = service.create(spexareId);

        return ResponseEntity.created(linkTo(methodOn(ActivityApi.class).retrieve(spexareId, dto.getId())).toUri())
                .body(EntityModel.of(dto, getLinks(dto, spexareId)));
    }

    @DeleteMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<Object> delete(@PathVariable final Long spexareId, @PathVariable final Long id) {
        service.deleteById(spexareId, id);

        return ResponseEntity.noContent().build();
    }

    private void addLinks(final EntityModel<ActivityDto> entity, final Long spexareId) {
        if (entity.getContent() != null) {
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
