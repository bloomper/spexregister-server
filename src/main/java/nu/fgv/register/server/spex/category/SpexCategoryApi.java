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

package nu.fgv.register.server.spex.category;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.audit.AuditApi;
import nu.fgv.register.server.audit.AuditedType;
import nu.fgv.register.server.impex.JobApi;
import nu.fgv.register.server.impex.JobService;
import nu.fgv.register.server.impex.model.ImpexType;
import nu.fgv.register.server.impex.model.JobReferenceDto;
import nu.fgv.register.server.util.FileUtil;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.util.security.RequiresAdmin;
import nu.fgv.register.server.util.security.RequiresAdminOrEditor;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
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
 * The spex category REST API under {@code /api/spex/categories}, documented by the REST Docs tests.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/spex/categories", version = "1.0")
public class SpexCategoryApi {

    private final SpexCategoryService service;
    private final JobService jobService;
    private final PagedResourcesAssembler<SpexCategoryDto> pagedResourcesAssembler;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<PagedModel<EntityModel<SpexCategoryDto>>> retrieve(@SortDefault(sort = SpexCategory_.NAME, direction = Sort.Direction.ASC) final Pageable pageable,
                                                                             @RequestParam(required = false, defaultValue = "") final String filter) {
        final PagedModel<EntityModel<SpexCategoryDto>> paged = pagedResourcesAssembler.toModel(service.find(filter, pageable));
        paged.getContent().forEach(this::addLinks);

        return ResponseEntity.ok(paged);
    }

    @GetMapping(params = {"type"}, produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditor
    public ResponseEntity<JobReferenceDto> retrieve(@Nullable @RequestParam(required = false) final List<UUID> ids,
                                                    @RequestParam(required = false, defaultValue = "") final String filter,
                                                    @RequestParam final String type,
                                                    final Locale locale) {
        final Long jobId = jobService.createExportJob(SpexCategoryExportService.class, Optional.ofNullable(ids).orElse(Collections.emptyList()), filter, ImpexType.fromValue(type), locale);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .location(linkTo(methodOn(JobApi.class).results(jobId)).toUri())
                .body(JobReferenceDto.builder()
                        .id(jobId)
                        .build());
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<SpexCategoryDto>> retrieve(@PathVariable final UUID id) {
        final SpexCategoryDto dto = service.findById(id);

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto)));
    }

    private void addLinks(final EntityModel<SpexCategoryDto> entity) {
        addLinks(entity.getContent());
    }

    void addLinks(final SpexCategoryDto dto) {
        dto.add(getLinks(dto));
    }

    public List<Link> getLinks(final SpexCategoryDto dto) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(SpexCategoryApi.class).retrieve(dto.getId())).withSelfRel());
        links.add(linkTo(methodOn(SpexCategoryApi.class).retrieve(Pageable.unpaged(), "")).withRel("spex-categories"));
        if (dto.getLogoUrl() != null) {
            links.add(Link.of(dto.getLogoUrl(), "logo"));
        }
        links.add(linkTo(methodOn(AuditApi.class).retrieve(AuditedType.SPEX_CATEGORY, String.valueOf(dto.getId()))).withRel("revisions"));

        return links;
    }
}
