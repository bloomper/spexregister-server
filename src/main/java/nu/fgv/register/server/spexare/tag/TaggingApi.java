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

package nu.fgv.register.server.spexare.tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.SpexareApi;
import nu.fgv.register.server.tag.TagDto;
import nu.fgv.register.server.tag.Tag_;
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
@RequestMapping("/api/v1/spexare/{spexareId}/tags")
public class TaggingApi {

    private final TaggingService service;
    private final PagedResourcesAssembler<TagDto> pagedResourcesAssembler;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<TagDto>>> retrieve(@PathVariable final Long spexareId,
                                                                    @SortDefault(sort = Tag_.NAME, direction = Sort.Direction.ASC) final Pageable pageable) {
        final PagedModel<EntityModel<TagDto>> paged = pagedResourcesAssembler.toModel(service.findBySpexare(spexareId, pageable));

        paged.getContent().forEach(p -> addLinks(p, spexareId));

        return ResponseEntity.ok(paged);
    }

    @PostMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<Object> create(@PathVariable final Long spexareId, @PathVariable final Long id) {
        service.create(spexareId, id);

        return ResponseEntity.created(linkTo(methodOn(TaggingApi.class).retrieve(spexareId, Pageable.unpaged())).toUri()).build();
    }

    @DeleteMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<Object> delete(@PathVariable final Long spexareId, @PathVariable final Long id) {
        service.deleteById(spexareId, id);

        return ResponseEntity.noContent().build();
    }

    private void addLinks(final EntityModel<TagDto> entity, final Long spexareId) {
        if (entity.getContent() != null) {
            entity.getContent().add(getLinks(entity.getContent(), spexareId));
        }
    }

    void addLinks(final TagDto dto, final Long spexareId) {
        dto.add(getLinks(dto, spexareId));
    }

    List<Link> getLinks(final TagDto dto, final Long spexareId) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(TaggingApi.class).retrieve(spexareId, Pageable.unpaged())).withRel("tags"));
        links.add(linkTo(methodOn(SpexareApi.class).retrieve(spexareId)).withRel("spexare"));

        return links;
    }

}
