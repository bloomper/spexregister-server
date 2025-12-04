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

package nu.fgv.register.server.spexare.activity.spex;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spex.SpexApi;
import nu.fgv.register.server.spex.SpexDto;
import nu.fgv.register.server.spexare.SpexareApi;
import nu.fgv.register.server.spexare.activity.ActivityApi;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
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
@RequestMapping(path = "/api/spexare/{spexareId}/activities/{activityId}/spex-activity", version = "1.0")
public class SpexActivityApi {

    private final SpexActivityService service;
    private final SpexApi spexApi;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<SpexActivityDto>> retrieve(@PathVariable final Long spexareId,
                                                                 @PathVariable final Long activityId) {
        final SpexActivityDto dto = service.findByActivity(spexareId, activityId);

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto, spexareId, activityId)));
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<SpexActivityDto>> retrieve(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long id) {
        final SpexActivityDto dto = service.findById(spexareId, activityId, id);

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto, spexareId, activityId)));
    }

    @PostMapping(value = "/{spexId}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<SpexActivityDto>> create(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long spexId) {
        final SpexActivityDto dto = service.create(spexareId, activityId, spexId);

        return ResponseEntity.created(linkTo(methodOn(SpexActivityApi.class).retrieve(spexareId, activityId, dto.getId())).toUri())
                .body(EntityModel.of(dto, getLinks(dto, spexareId, activityId)));
    }

    @PutMapping(value = "/{id}/{spexId}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<SpexActivityDto>> update(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long id, @PathVariable final Long spexId) {
        final SpexActivityDto dto = service.update(spexareId, activityId, spexId, id);

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto, spexareId, activityId)));
    }

    @DeleteMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<Object> delete(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long id) {
        service.deleteById(spexareId, activityId, id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{id}/spex", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<SpexDto>> retrieveSpex(@PathVariable final Long spexareId, @PathVariable final Long activityId, @PathVariable final Long id) {
        final SpexDto dto = service.findSpexBySpexActivity(spexareId, activityId, id);

        return ResponseEntity.ok(EntityModel.of(dto, spexApi.getLinks(dto, false)));
    }

    private void addLinks(final EntityModel<SpexActivityDto> entity, final Long spexareId, final Long activityId) {
        if (entity.getContent() != null) {
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
        links.add(linkTo(methodOn(SpexActivityApi.class).retrieve(spexareId, activityId)).withRel("spex-activity"));
        links.add(linkTo(methodOn(ActivityApi.class).retrieve(spexareId, Pageable.unpaged())).withRel("activities"));
        links.add(linkTo(methodOn(SpexareApi.class).retrieve(spexareId)).withRel("spexare"));

        return links;
    }

}
