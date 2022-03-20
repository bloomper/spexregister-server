package nu.fgv.register.server.spex;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.Constants;
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
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/spex")
public class SpexApi {

    private final SpexService service;
    private final SpexExportService exportService;
    private final PagedResourcesAssembler<SpexDto> pagedResourcesAssembler;
    private final SpexCategoryApi spexCategoryApi;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<SpexDto>>> retrieve(@RequestParam(required = false, defaultValue = "false") final boolean includeRevivals, @SortDefault(sort = "year", direction = Sort.Direction.ASC) final Pageable pageable) {
        final PagedModel<EntityModel<SpexDto>> paged = pagedResourcesAssembler.toModel(service.find(includeRevivals, pageable));
        paged.getContent().forEach(this::addLinks);

        return ResponseEntity.ok(paged);
    }

    @GetMapping(headers = {
            HttpHeaders.ACCEPT + "=" + Constants.MediaTypes.APPLICATION_XLSX_VALUE,
            HttpHeaders.ACCEPT + "=" + Constants.MediaTypes.APPLICATION_XLS_VALUE
    }, produces = {
            Constants.MediaTypes.APPLICATION_XLSX_VALUE,
            Constants.MediaTypes.APPLICATION_XLS_VALUE
    })
    public ResponseEntity<Resource> retrieve(@RequestParam(required = false) final List<Long> ids, @RequestHeader(HttpHeaders.ACCEPT) String contentType, final Locale locale) {
        try {
            final Pair<String, byte[]> export = exportService.doExport(ids, contentType, locale);
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"spex" + export.getFirst() + "\"")
                    .body(new ByteArrayResource(export.getSecond()));
        } catch (final Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not export spex", e);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<SpexDto>> create(@Valid @RequestBody SpexCreateDto dto) {
        final SpexDto newDto = service.create(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(EntityModel.of(newDto, getLinks(newDto)));
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<SpexDto>> retrieve(@PathVariable Long id) {
        return service
                .findById(id)
                .map(dto -> EntityModel.of(dto, getLinks(dto)))
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<SpexDto>> update(@PathVariable Long id, @Valid @RequestBody SpexUpdateDto dto) {
        if (dto.getId() == null || !Objects.equals(id, dto.getId())) {
            return ResponseEntity.badRequest().build();
        }
        return service
                .update(dto)
                .map(updatedDto -> ResponseEntity.status(HttpStatus.ACCEPTED).body(EntityModel.of(updatedDto, getLinks(updatedDto))))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PatchMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<SpexDto>> partialUpdate(@PathVariable Long id, @Valid @RequestBody SpexUpdateDto dto) {
        if (dto.getId() == null || !Objects.equals(id, dto.getId())) {
            return ResponseEntity.badRequest().build();
        }
        return service
                .partialUpdate(dto)
                .map(updatedDto -> ResponseEntity.status(HttpStatus.ACCEPTED).body(EntityModel.of(updatedDto, getLinks(updatedDto))))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return service
                .findById(id)
                .map(dto -> {
                    service.deleteById(id);
                    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/poster")
    public ResponseEntity<Resource> downloadPoster(@PathVariable Long id) {
        return service.getPoster(id)
                .map(tuple -> {
                    final Resource resource = new ByteArrayResource(tuple.getFirst());
                    return ResponseEntity.ok()
                            .contentType(MediaType.valueOf(tuple.getSecond()))
                            .body(resource);
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @RequestMapping(value = "/{id}/poster", method = {RequestMethod.POST, RequestMethod.PUT}, consumes = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_GIF_VALUE})
    public ResponseEntity<?> uploadPoster(@PathVariable Long id, @RequestBody byte[] file, @RequestHeader(HttpHeaders.CONTENT_TYPE) final String contentType) {
        return service.savePoster(id, file, contentType)
                .map(entity -> ResponseEntity.status(HttpStatus.NO_CONTENT).build())
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @RequestMapping(value = "/{id}/poster", method = {RequestMethod.POST, RequestMethod.PUT}, consumes = {"multipart/form-data"})
    public ResponseEntity<?> uploadPoster(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            return uploadPoster(id, file.getBytes(), file.getContentType());
        } catch (final IOException e) {
            if (log.isErrorEnabled()) {
                log.error(String.format("Could not save poster for spex %s", id), e);
            }
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
    }

    @DeleteMapping("/{id}/poster")
    public ResponseEntity<?> deletePoster(@PathVariable Long id) {
        return service.removePoster(id)
                .map(entity -> ResponseEntity.status(HttpStatus.NO_CONTENT).build())
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping(value = "/revivals", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<SpexDto>>> retrieveRevivals(@SortDefault(sort = "year", direction = Sort.Direction.ASC) final Pageable pageable) {
        final PagedModel<EntityModel<SpexDto>> paged = pagedResourcesAssembler.toModel(service.findRevivals(pageable));
        paged.getContent().forEach(this::addLinks);

        return ResponseEntity.ok(paged);
    }

    @GetMapping(value = "/{id}/revivals", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<SpexDto>>> retrieveRevivalsByParent(@PathVariable Long id, @SortDefault(sort = "year", direction = Sort.Direction.ASC) final Pageable pageable) {
        final PagedModel<EntityModel<SpexDto>> paged = pagedResourcesAssembler.toModel(service.findRevivalsByParent(id, pageable));
        paged.getContent().forEach(this::addLinks);

        return ResponseEntity.ok(paged);
    }

    @PutMapping(value = "/{id}/revivals/{year}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<SpexDto>> addRevival(@PathVariable Long id, @PathVariable final String year) {
        return service
                .addRevival(id, year)
                .map(dto -> ResponseEntity.status(HttpStatus.ACCEPTED).body(EntityModel.of(dto, getLinks(dto))))
                .orElse(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }

    @DeleteMapping(value = "/{id}/revivals/{year}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<?> removeRevival(@PathVariable Long id, @PathVariable final String year) {
        return service.removeRevival(id, year) ? ResponseEntity.status(HttpStatus.NO_CONTENT).build() : ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @PutMapping(value = "/{id}/spex-category/{categoryId}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<SpexDto>> updateCategory(@PathVariable Long id, @PathVariable final Long categoryId) {
        return service
                .updateCategory(id, categoryId)
                .map(updatedDto -> ResponseEntity.status(HttpStatus.ACCEPTED).body(EntityModel.of(updatedDto, getLinks(updatedDto))))
                .orElse(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }

    @DeleteMapping(value = "/{id}/spex-category", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<SpexDto>> removeCategory(@PathVariable Long id) {
        return service
                .removeCategory(id)
                .map(updatedDto -> ResponseEntity.status(HttpStatus.ACCEPTED).body(EntityModel.of(updatedDto, getLinks(updatedDto))))
                .orElse(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }

    private void addLinks(final EntityModel<SpexDto> entity) {
        if (entity != null && entity.getContent() != null) {
            entity.getContent().add(getLinks(entity.getContent()));
        }
    }

    void addLinks(final SpexDto dto) {
        dto.add(getLinks(dto));
    }

    List<Link> getLinks(final SpexDto dto) {
        final List<Link> links = new ArrayList<>();
        links.add(linkTo(methodOn(SpexApi.class).retrieve(dto.getId())).withSelfRel());
        if (hasText(dto.getPoster())) {
            links.add(Link.of(dto.getPoster()).withRel("poster"));
        } else {
            final Link logoLink = linkTo(methodOn(SpexApi.class).downloadPoster(dto.getId())).withRel("poster");
            links.add(logoLink);
        }
        if (dto.getParent() != null) {
            links.add(linkTo(methodOn(SpexApi.class).retrieve(dto.getParent().getId())).withRel("parent"));
        } else {
            links.add(linkTo(methodOn(SpexApi.class).retrieveRevivalsByParent(dto.getId(), null)).withRel("revivals"));
        }
        if (dto.getCategory() != null) {
            spexCategoryApi.addLinks(dto.getCategory());
        }
        return links;
    }

}
