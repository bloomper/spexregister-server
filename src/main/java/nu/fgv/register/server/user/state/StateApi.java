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

package nu.fgv.register.server.user.state;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.user.authority.Authority_;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@RequestMapping(path = "/api/users/states", version = "1.0")
public class StateApi {

    private final StateService service;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<CollectionModel<EntityModel<StateDto>>> retrieve(@SortDefault(sort = Authority_.ID, direction = Sort.Direction.ASC) final Sort sort) {
        final List<EntityModel<StateDto>> states = service.findAll(sort).stream()
                .map(dto -> EntityModel.of(dto, getLinks(dto)))
                .toList();

        return ResponseEntity.ok(
                CollectionModel.of(states,
                        linkTo(methodOn(StateApi.class).retrieve(Sort.unsorted())).withSelfRel()));
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<StateDto>> retrieve(@PathVariable final String id) {
        final StateDto dto = service.findById(id);

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto)));
    }

    private void addLinks(final EntityModel<StateDto> entity) {
        if (entity.getContent() != null) {
            entity.getContent().add(getLinks(entity.getContent()));
        }
    }

    void addLinks(final StateDto dto) {
        dto.add(getLinks(dto));
    }

    public List<Link> getLinks(final StateDto dto) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(StateApi.class).retrieve(dto.getId())).withSelfRel());
        links.add(linkTo(methodOn(StateApi.class).retrieve(Sort.unsorted())).withRel("states"));

        return links;
    }

}
