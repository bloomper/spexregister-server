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

package nu.fgv.register.server.spex;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.audit.AuditApi;
import nu.fgv.register.server.audit.AuditedType;
import nu.fgv.register.server.impex.JobApi;
import nu.fgv.register.server.impex.JobService;
import nu.fgv.register.server.impex.model.ImpexType;
import nu.fgv.register.server.impex.model.JobReferenceDto;
import nu.fgv.register.server.spex.category.SpexCategoryApi;
import nu.fgv.register.server.spex.category.SpexCategoryDto;
import nu.fgv.register.server.util.FileUtil;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.util.filter.FilterOperation;
import nu.fgv.register.server.util.security.RequiresAdmin;
import nu.fgv.register.server.util.security.RequiresAdminOrEditor;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import nu.fgv.register.server.util.security.RequiresSpexReader;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.SortDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * The spex REST API under {@code /api/spex}, documented by the REST Docs tests.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/spex", version = "1.0")
public class SpexApi {

    private final SpexService service;
    private final JobService jobService;
    private final PagedResourcesAssembler<SpexDto> pagedResourcesAssembler;
    private final SpexCategoryApi spexCategoryApi;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE, version = "1.0")
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<PagedModel<EntityModel<SpexDto>>> retrieve(@SortDefault(sort = Spex_.YEAR, direction = Sort.Direction.ASC) final Pageable pageable,
                                                                     @RequestParam(required = false, defaultValue = Spex_.PARENT + ":" + FilterOperation.NULL) final String filter) {
        final PagedModel<EntityModel<SpexDto>> paged = pagedResourcesAssembler.toModel(service.find(filter, pageable));

        paged.getContent().forEach(this::addLinks);

        return ResponseEntity.ok(paged);
    }

    @GetMapping(params = {"type"}, produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditor
    public ResponseEntity<JobReferenceDto> retrieve(@Nullable @RequestParam(required = false) final List<UUID> ids,
                                                    @RequestParam(required = false, defaultValue = "") final String filter,
                                                    @RequestParam final String type,
                                                    final Locale locale) {
        final Long jobId = jobService.createExportJob(SpexExportService.class, Optional.ofNullable(ids).orElse(Collections.emptyList()), filter, ImpexType.fromValue(type), locale);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .location(linkTo(methodOn(JobApi.class).results(jobId)).toUri())
                .body(JobReferenceDto.builder()
                        .id(jobId)
                        .build());
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE, version = "1.0")
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<SpexDto>> retrieve(@PathVariable final UUID id) {
        final SpexDto dto = service.findById(id);

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto)));
    }

    @GetMapping(value = "/{spexId}/parent", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<SpexDto>> retrieveParent(@PathVariable final UUID spexId) {
        final SpexDto dto = service.findParentById(spexId);

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto)));
    }

    @GetMapping(value = "/{spexId}/revivals/{id}", produces = MediaTypes.HAL_JSON_VALUE, version = "1.0")
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<SpexDto>> retrieveRevival(@PathVariable final UUID spexId, @PathVariable final UUID id) {
        final SpexDto dto = service.findRevivalById(spexId, id);

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto)));
    }

    @GetMapping(value = "/{spexId}/revivals", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<PagedModel<EntityModel<SpexDto>>> retrieveRevivalsByParent(@PathVariable final UUID spexId,
                                                                                     @SortDefault(sort = Spex_.YEAR, direction = Sort.Direction.ASC) final Pageable pageable) {
        final PagedModel<EntityModel<SpexDto>> paged = pagedResourcesAssembler.toModel(service.findRevivalsByParent(spexId, pageable));
        paged.getContent().forEach(this::addLinks);

        return ResponseEntity.ok(paged);
    }

    @GetMapping(value = "/{spexId}/category", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<SpexCategoryDto>> retrieveCategory(@PathVariable final UUID spexId) {
        final SpexCategoryDto dto = service.findCategoryBySpex(spexId);

        return ResponseEntity.ok(EntityModel.of(dto, spexCategoryApi.getLinks(dto)));
    }

    private void addLinks(final EntityModel<SpexDto> entity) {
        entity.getContent().add(getLinks(entity.getContent()));
    }

    void addLinks(final SpexDto dto) {
        dto.add(getLinks(dto));
    }

    public List<Link> getLinks(final SpexDto dto) {
        return getLinks(dto, true);
    }

    public List<Link> getLinks(final SpexDto dto, final boolean includeRevisions) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(SpexApi.class).retrieve(dto.getId())).withSelfRel());
        links.add(linkTo(methodOn(SpexApi.class).retrieve(Pageable.unpaged(), Spex_.PARENT + ":NULL")).withRel("spex"));
        links.add(linkTo(methodOn(SpexApi.class).retrieve(Pageable.unpaged(), Spex_.PARENT + "!NULL")).withRel("spex-including-revivals"));
        if (dto.getPosterUrl() != null) {
            links.add(Link.of(dto.getPosterUrl(), "poster"));
        }
        links.add(linkTo(methodOn(SpexApi.class).retrieveCategory(dto.getId())).withRel("category"));
        if (Boolean.TRUE.equals(dto.getRevival())) {
            links.add(linkTo(methodOn(SpexApi.class).retrieveParent(dto.getId())).withRel("parent"));
        } else {
            links.add(linkTo(methodOn(SpexApi.class).retrieveRevivalsByParent(dto.getId(), Pageable.unpaged())).withRel("revivals"));
        }
        if (includeRevisions) {
            links.add(linkTo(methodOn(AuditApi.class).retrieve(AuditedType.SPEX, String.valueOf(dto.getId()))).withRel("revisions"));
        }

        return links;
    }

}
