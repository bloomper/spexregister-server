/*
 * Copyright 2026 the original author or authors.
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

package nu.fgv.register.server.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.security.RequiresAdmin;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
@RequestMapping(path = "/api/revisions", version = "1.0")
public class AuditApi {

    private final AuditService service;
    private final PagedResourcesAssembler<RevisionFeedEntryDto> pagedResourcesAssembler;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<PagedModel<EntityModel<RevisionFeedEntryDto>>> retrieveFeed(final Pageable pageable,
                                                                                      @Nullable @RequestParam(required = false) final AuditedType type,
                                                                                      @RequestParam(required = false, defaultValue = "90") final Integer sinceInDays) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findFeedPaged(type, sinceInDays, pageable)));
    }

    @GetMapping(value = "/{type}/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<CollectionModel<EntityModel<RevisionDto>>> retrieve(@PathVariable final AuditedType type, @PathVariable final String id) {
        final List<EntityModel<RevisionDto>> revisions = service.findRevisions(type, id).stream()
                .map(dto -> EntityModel.of(dto, getLinks(dto)))
                .toList();

        return ResponseEntity.ok(CollectionModel.of(revisions, linkTo(methodOn(AuditApi.class).retrieve(type, id)).withSelfRel()));
    }

    @GetMapping(value = "/{type}/{id}/related/{relatedType}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<CollectionModel<EntityModel<RevisionDto>>> retrieveRelated(@PathVariable final AuditedType type,
                                                                                     @PathVariable final String id,
                                                                                     @PathVariable final AuditedType relatedType) {
        final List<EntityModel<RevisionDto>> revisions = service.findRelatedRevisions(type, id, relatedType).stream()
                .map(dto -> EntityModel.of(dto, getLinks(dto)))
                .toList();

        return ResponseEntity.ok(CollectionModel.of(revisions,
                linkTo(methodOn(AuditApi.class).retrieveRelated(type, id, relatedType)).withSelfRel()));
    }

    @GetMapping(value = "/{type}/{id}/{revision}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<RevisionDto>> retrieve(@PathVariable final AuditedType type,
                                                             @PathVariable final String id,
                                                             @PathVariable final Long revision) {
        final RevisionDto dto = service.findRevision(type, id, revision);

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto)));
    }

    @GetMapping("/{type}/{id}/{revision}/binary/{field}")
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<Resource> retrieveBinary(@PathVariable final AuditedType type,
                                                   @PathVariable final String id,
                                                   @PathVariable final Long revision,
                                                   @PathVariable final String field) {
        final BinaryValueDto binary = service.findBinary(type, id, revision, field);

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(binary.contentType()))
                .body(new ByteArrayResource(binary.content()));
    }

    @GetMapping(value = "/{type}/{id}/{revision}/preview", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<EntityModel<RestorePreviewDto>> preview(@PathVariable final AuditedType type,
                                                                  @PathVariable final String id,
                                                                  @PathVariable final Long revision,
                                                                  @RequestParam(required = false, defaultValue = "false") final Boolean cascade) {
        return ResponseEntity.ok(EntityModel.of(service.preview(type, id, revision, cascade)));
    }

    @PostMapping(value = "/{type}/{id}/{revision}/restore", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<EntityModel<RestoreResultDto>> restore(@PathVariable final AuditedType type,
                                                                 @PathVariable final String id,
                                                                 @PathVariable final Long revision,
                                                                 @RequestParam(required = false, defaultValue = "false") final Boolean cascade) {
        return ResponseEntity.ok(EntityModel.of(service.restore(type, id, revision, cascade)));
    }

    List<Link> getLinks(final RevisionDto dto) {
        final List<Link> links = new ArrayList<>();
        final String id = String.valueOf(dto.entityId());

        links.add(linkTo(methodOn(AuditApi.class).retrieve(dto.type(), id, dto.revision())).withSelfRel());
        links.add(linkTo(methodOn(AuditApi.class).retrieve(dto.type(), id)).withRel("revisions"));

        return links;
    }
}