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

package nu.fgv.register.server.spexare.toggle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.SpexareApi;
import nu.fgv.register.server.spexare.address.AddressApi;
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
@RequestMapping("/api/v1/spexare/{spexareId}/toggles")
public class ToggleApi {

    private final ToggleService service;
    private final PagedResourcesAssembler<ToggleDto> pagedResourcesAssembler;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<ToggleDto>>> retrieve(@PathVariable final Long spexareId,
                                                                       @SortDefault(sort = Toggle_.TYPE, direction = Sort.Direction.ASC) final Pageable pageable) {
        final PagedModel<EntityModel<ToggleDto>> paged = pagedResourcesAssembler.toModel(service.findBySpexare(spexareId, pageable));

        paged.getContent().forEach(p -> addLinks(p, spexareId));

        return ResponseEntity.ok(paged);
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<ToggleDto>> retrieve(@PathVariable final Long spexareId, @PathVariable final Long id) {
        final ToggleDto dto = service.findById(spexareId, id);

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto, spexareId)));
    }

    @PostMapping(value = "/{typeId}/{value}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<ToggleDto>> create(@PathVariable final Long spexareId, @PathVariable final String typeId, @PathVariable final Boolean value) {
        final ToggleDto dto = service.create(spexareId, typeId, value);

        return ResponseEntity.created(linkTo(methodOn(AddressApi.class).retrieve(spexareId, dto.getId())).toUri())
                .body(EntityModel.of(dto, getLinks(dto, spexareId)));
    }

    @PutMapping(value = "/{typeId}/{id}/{value}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<ToggleDto>> update(@PathVariable final Long spexareId, @PathVariable final String typeId, @PathVariable final Long id, @PathVariable final Boolean value) {
        final ToggleDto dto = service.update(spexareId, typeId, id, value);

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto, spexareId)));
    }

    @DeleteMapping(value = "/{typeId}/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<Object> delete(@PathVariable final Long spexareId, @PathVariable final String typeId, @PathVariable final Long id) {
        service.deleteById(spexareId, typeId, id);

        return ResponseEntity.noContent().build();
    }

    private void addLinks(final EntityModel<ToggleDto> entity, final Long spexareId) {
        if (entity.getContent() != null) {
            entity.getContent().add(getLinks(entity.getContent(), spexareId));
        }
    }

    void addLinks(final ToggleDto dto, final Long spexareId) {
        dto.add(getLinks(dto, spexareId));
    }

    List<Link> getLinks(final ToggleDto dto, final Long spexareId) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(ToggleApi.class).retrieve(spexareId, dto.getId())).withSelfRel());
        links.add(linkTo(methodOn(ToggleApi.class).retrieve(spexareId, Pageable.unpaged())).withRel("toggles"));
        links.add(linkTo(methodOn(SpexareApi.class).retrieve(spexareId)).withRel("spexare"));

        return links;
    }

}
