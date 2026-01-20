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

package nu.fgv.register.server.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.event.Event;
import nu.fgv.register.server.event.EventApi;
import nu.fgv.register.server.event.EventDto;
import nu.fgv.register.server.event.EventService;
import nu.fgv.register.server.impex.JobApi;
import nu.fgv.register.server.impex.JobService;
import nu.fgv.register.server.impex.model.ImpexType;
import nu.fgv.register.server.impex.model.JobReferenceDto;
import nu.fgv.register.server.spexare.SpexareApi;
import nu.fgv.register.server.spexare.SpexareDto;
import nu.fgv.register.server.user.authority.AuthorityApi;
import nu.fgv.register.server.user.authority.AuthorityDto;
import nu.fgv.register.server.user.state.StateApi;
import nu.fgv.register.server.user.state.StateDto;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.util.error.ResourceNoValueException;
import nu.fgv.register.server.util.security.RequiresAdmin;
import nu.fgv.register.server.util.security.RequiresAdminOrEditor;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.SortDefault;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/users", version = "1.0")
public class UserApi {

    private final UserService service;
    private final EventService eventService;
    private final JobService jobService;
    private final PagedResourcesAssembler<UserDto> pagedResourcesAssembler;
    private final AuthorityApi authorityApi;
    private final StateApi stateApi;
    private final SpexareApi spexareApi;
    private final EventApi eventApi;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<PagedModel<EntityModel<UserDto>>> retrieve(@SortDefault(sort = User_.ID, direction = Sort.Direction.ASC) final Pageable pageable,
                                                                     @RequestParam(required = false, defaultValue = "") final String filter) {
        final PagedModel<EntityModel<UserDto>> paged = pagedResourcesAssembler.toModel(service.find(filter, pageable));
        paged.getContent().forEach(this::addLinks);

        return ResponseEntity.ok(paged);
    }

    @GetMapping(params = {"type"}, produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<JobReferenceDto> retrieve(@Nullable @RequestParam(required = false) final List<Long> ids,
                                                    @RequestParam(required = false, defaultValue = "") final String filter,
                                                    @RequestParam final String type,
                                                    final Locale locale) {
        final Long jobId = jobService.createExportJob(UserExportService.class, Optional.ofNullable(ids).orElse(Collections.emptyList()), filter, ImpexType.fromValue(type), locale);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .location(linkTo(methodOn(JobApi.class).results(jobId)).toUri())
                .body(JobReferenceDto.builder()
                        .id(jobId)
                        .build());
    }

    @PostMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<EntityModel<UserDto>> create(@Valid @RequestBody final UserCreateDto dto) {
        final UserDto createdDto = service.create(dto);

        return ResponseEntity.created(linkTo(methodOn(UserApi.class).retrieve(createdDto.getId())).toUri())
                .body(EntityModel.of(createdDto, getLinks(createdDto)));
    }

    @GetMapping(value = "/me", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<UserDto>> retrieve(@AuthenticationPrincipal final Jwt jwt) {
        final UserDto dto = service.findByExternalId(jwt.getSubject());

        if (dto == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto)));
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<EntityModel<UserDto>> retrieve(@PathVariable final Long id) {
        final UserDto dto = service.findById(id);

        return ResponseEntity.ok(EntityModel.of(dto, getLinks(dto)));
    }

    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT}, params = {"type"})
    @RequiresAdminOrEditor
    public ResponseEntity<JobReferenceDto> createAndUpdate(@RequestBody final byte[] file,
                                                           @RequestParam final String type,
                                                           final Locale locale) {
        final Long jobId = jobService.createImportJob(UserImportService.class, file, ImpexType.fromValue(type), locale);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .location(linkTo(methodOn(JobApi.class).results(jobId)).toUri())
                .body(JobReferenceDto.builder()
                        .id(jobId)
                        .build());
    }

    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT}, params = {"type"}, consumes = {"multipart/form-data"})
    @RequiresAdminOrEditor
    public ResponseEntity<JobReferenceDto> createAndUpdate(@RequestParam("file") final MultipartFile file,
                                                           @RequestParam final String type,
                                                           final Locale locale) {
        try {
            return createAndUpdate(file.getBytes(), type, locale);
        } catch (final IOException e) {
            throw new InternalErrorException(e.getMessage());
        }
    }

    @PutMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<EntityModel<UserDto>> update(@PathVariable final Long id, @Valid @RequestBody final UserUpdateDto dto) {
        if (!Objects.equals(id, dto.id())) {
            return ResponseEntity.badRequest().build();
        }

        final UserDto updatedDto = service.update(dto);

        return ResponseEntity.ok(EntityModel.of(updatedDto, getLinks(updatedDto)));
    }

    @PatchMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<EntityModel<UserDto>> partialUpdate(@PathVariable final Long id, @Valid @RequestBody final UserUpdateDto dto) {
        if (!Objects.equals(id, dto.id())) {
            return ResponseEntity.badRequest().build();
        }

        final UserDto updatedDto = service.partialUpdate(dto);

        return ResponseEntity.ok(EntityModel.of(updatedDto, getLinks(updatedDto)));
    }

    @DeleteMapping("/{id}")
    @RequiresAdmin
    public ResponseEntity<Object> delete(@PathVariable final Long id) {
        service.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{userId}/authorities", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<CollectionModel<EntityModel<AuthorityDto>>> retrieveAuthorities(@PathVariable final Long userId) {
        final Set<EntityModel<AuthorityDto>> authorities = service.getAuthoritiesByUser(userId).stream()
                .map(dto -> EntityModel.of(dto, authorityApi.getLinks(dto)))
                .collect(Collectors.toSet());

        return ResponseEntity.ok(
                CollectionModel.of(authorities,
                        linkTo(methodOn(AuthorityApi.class).retrieve(Sort.unsorted())).withSelfRel()));
    }

    @PutMapping(value = "/{userId}/authorities/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<Serializable> addAuthority(@PathVariable final Long userId, @PathVariable final String id) {
        service.addAuthority(userId, id);

        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{userId}/authorities", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<Serializable> addAuthorities(@PathVariable final Long userId, @RequestParam final List<String> ids) {
        service.addAuthorities(userId, ids);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value = "/{userId}/authorities/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<Serializable> removeAuthority(@PathVariable final Long userId, @PathVariable final String id) {
        service.removeAuthority(userId, id);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value = "/{userId}/authorities", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<Serializable> removeAuthorities(@PathVariable final Long userId, @RequestParam final List<String> ids) {
        service.removeAuthorities(userId, ids);

        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{userId}/state", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<EntityModel<StateDto>> retrieveState(@PathVariable final Long userId) {
        final StateDto dto = service.getStateByUser(userId);

        return ResponseEntity.ok(EntityModel.of(dto, stateApi.getLinks(dto)));
    }

    @PutMapping(value = "/{userId}/state/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<Serializable> setState(@PathVariable final Long userId, @PathVariable final String id) {
        service.setState(userId, id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{userId}/spexare", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<EntityModel<SpexareDto>> retrieveSpexare(@PathVariable final Long userId) {
        final SpexareDto dto = service.findSpexareByUser(userId)
                .orElseThrow(() -> new ResourceNoValueException(User.class, User_.SPEXARE, userId));

        return ResponseEntity.ok(EntityModel.of(dto, spexareApi.getLinks(dto)));
    }

    @PutMapping(value = "/{userId}/spexare/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<Serializable> addSpexare(@PathVariable final Long userId, @PathVariable final Long id) {
        service.addSpexare(userId, id);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value = "/{userId}/spexare", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<Serializable> removeSpexare(@PathVariable final Long userId) {
        service.removeSpexare(userId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/events/{sourceId}", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<CollectionModel<EntityModel<EventDto>>> retrieveEvents(@PathVariable final Long sourceId, @RequestParam(defaultValue = "90") final Integer sinceInDays) {
        final List<EntityModel<EventDto>> events = eventService.findBySourceTypeAndId(Event.SourceType.USER, sourceId, sinceInDays).stream()
                .map(dto -> EntityModel.of(dto, eventApi.getLinks(dto)))
                .toList();

        return ResponseEntity.ok(
                CollectionModel.of(events,
                        linkTo(methodOn(EventApi.class).retrieve(Event.SourceType.USER, -1)).withSelfRel()));
    }

    private void addLinks(final EntityModel<UserDto> entity) {
        if (entity.getContent() != null) {
            entity.getContent().add(getLinks(entity.getContent()));
        }
    }

    void addLinks(final UserDto dto) {
        dto.add(getLinks(dto));
    }

    public List<Link> getLinks(final UserDto dto) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(UserApi.class).retrieve(dto.getId())).withSelfRel());
        links.add(linkTo(methodOn(UserApi.class).retrieve(Pageable.unpaged(), "")).withRel("users"));
        links.add(linkTo(methodOn(UserApi.class).retrieveSpexare(dto.getId())).withRel("spexare"));
        links.add(linkTo(methodOn(UserApi.class).retrieveState(dto.getId())).withRel("state"));
        links.add(linkTo(methodOn(UserApi.class).retrieveAuthorities(dto.getId())).withRel("authorities"));
        links.add(linkTo(methodOn(UserApi.class).retrieveEvents(dto.getId(), -1)).withRel("events"));

        return links;
    }

}
