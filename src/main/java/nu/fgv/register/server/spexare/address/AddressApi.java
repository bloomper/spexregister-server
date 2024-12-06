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

package nu.fgv.register.server.spexare.address;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.SpexareApi;
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
@RequestMapping("/api/v1/spexare/{spexareId}/addresses")
public class AddressApi {

    private final AddressService service;
    private final PagedResourcesAssembler<AddressDto> pagedResourcesAssembler;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<PagedModel<EntityModel<AddressDto>>> retrieve(@PathVariable final Long spexareId,
                                                                        @SortDefault(sort = Address_.TYPE, direction = Sort.Direction.ASC) final Pageable pageable,
                                                                        @RequestParam(required = false, defaultValue = "") final String filter) {
        final PagedModel<EntityModel<AddressDto>> paged = pagedResourcesAssembler.toModel(service.findBySpexare(spexareId, filter, pageable));

        paged.getContent().forEach(p -> addLinks(p, spexareId));

        return ResponseEntity.ok(paged);
    }

    @PostMapping(value = "/{typeId}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<AddressDto>> create(@PathVariable final Long spexareId, @PathVariable final String typeId, @Valid @RequestBody final AddressCreateDto dto) {
        final AddressDto createdDto = service.create(spexareId, typeId, dto);

        return ResponseEntity.created(linkTo(methodOn(AddressApi.class).retrieve(spexareId, createdDto.getId())).toUri())
                .body(EntityModel.of(createdDto, getLinks(createdDto, spexareId)));
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<AddressDto>> retrieve(@PathVariable final Long spexareId, @PathVariable final Long id) {
        final AddressDto dto = service.findById(spexareId, id);

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto, spexareId)));
    }

    @PutMapping(value = "/{typeId}/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<AddressDto>> update(@PathVariable final Long spexareId, @PathVariable final String typeId, @PathVariable final Long id, @Valid @RequestBody final AddressUpdateDto dto) {
        if (!Objects.equals(id, dto.getId())) {
            return ResponseEntity.badRequest().build();
        }

        final AddressDto updatedDto = service.update(spexareId, typeId, id, dto);

        return ResponseEntity.ok(EntityModel.of(updatedDto, getLinks(updatedDto, spexareId)));
    }

    @PatchMapping(value = "/{typeId}/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<AddressDto>> partialUpdate(@PathVariable final Long spexareId, @PathVariable final String typeId, @PathVariable final Long id, @Valid @RequestBody final AddressUpdateDto dto) {
        if (!Objects.equals(id, dto.getId())) {
            return ResponseEntity.badRequest().build();
        }

        final AddressDto updatedDto = service.partialUpdate(spexareId, typeId, id, dto);

        return ResponseEntity.ok(EntityModel.of(updatedDto, getLinks(updatedDto, spexareId)));
    }

    @DeleteMapping(value = "/{typeId}/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<Object> delete(@PathVariable final Long spexareId, @PathVariable final String typeId, @PathVariable final Long id) {
        service.deleteById(spexareId, typeId, id);

        return ResponseEntity.noContent().build();
    }

    private void addLinks(final EntityModel<AddressDto> entity, final Long spexareId) {
        if (entity.getContent() != null) {
            entity.getContent().add(getLinks(entity.getContent(), spexareId));
        }
    }

    void addLinks(final AddressDto dto, final Long spexareId) {
        dto.add(getLinks(dto, spexareId));
    }

    List<Link> getLinks(final AddressDto dto, final Long spexareId) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(AddressApi.class).retrieve(spexareId, dto.getId())).withSelfRel());
        links.add(linkTo(methodOn(AddressApi.class).retrieve(spexareId, Pageable.unpaged(), "")).withRel("addresses"));
        links.add(linkTo(methodOn(SpexareApi.class).retrieve(spexareId)).withRel("spexare"));

        return links;
    }

}
