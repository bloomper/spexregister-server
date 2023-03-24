package nu.fgv.register.server.task;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.Constants;
import nu.fgv.register.server.util.impex.model.ImportResultDto;
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
import org.springframework.http.HttpMethod;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/tasks/categories")
public class TaskCategoryApi {

    private final TaskCategoryService service;
    private final TaskCategoryExportService exportService;
    private final TaskCategoryImportService importService;
    private final PagedResourcesAssembler<TaskCategoryDto> pagedResourcesAssembler;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<TaskCategoryDto>>> retrieve(@SortDefault(sort = "name", direction = Sort.Direction.ASC) final Pageable pageable) {
        final PagedModel<EntityModel<TaskCategoryDto>> paged = pagedResourcesAssembler.toModel(service.find(pageable));
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
    public ResponseEntity<Resource> retrieve(@RequestParam(required = false) final List<Long> ids, @RequestHeader(HttpHeaders.ACCEPT) final String contentType, final Locale locale) {
        try {
            final Pair<String, byte[]> export = exportService.doExport(ids, contentType, locale);
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"task_categories" + export.getFirst() + "\"")
                    .body(new ByteArrayResource(export.getSecond()));
        } catch (final Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not export task categories", e);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<TaskCategoryDto>> create(@Valid @RequestBody final TaskCategoryCreateDto dto) {
        final TaskCategoryDto newDto = service.create(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, linkTo(methodOn(TaskCategoryApi.class).retrieve(newDto.getId())).toString())
                .body(EntityModel.of(newDto, getLinks(newDto)));
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<TaskCategoryDto>> retrieve(@PathVariable final Long id) {
        return service
                .findById(id)
                .map(dto -> EntityModel.of(dto, getLinks(dto)))
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT},
            consumes = {
                    Constants.MediaTypes.APPLICATION_XLSX_VALUE,
                    Constants.MediaTypes.APPLICATION_XLS_VALUE
            })
    public ResponseEntity<ImportResultDto> createAndUpdate(@RequestBody final byte[] file, @RequestHeader(HttpHeaders.CONTENT_TYPE) final String contentType, final Locale locale, final HttpMethod method) {
        try {
            final ImportResultDto result = importService.doImport(file, contentType, locale);
            return ResponseEntity
                    .status(result.isSuccess() ? (method == HttpMethod.POST ? HttpStatus.CREATED : HttpStatus.ACCEPTED) : HttpStatus.BAD_REQUEST)
                    .body(result);
        } catch (final Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not import task categories", e);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT}, consumes = {"multipart/form-data"})
    public ResponseEntity<ImportResultDto> createAndUpdate(@RequestParam("file") final MultipartFile file, final Locale locale, final HttpMethod method) {
        try {
            return createAndUpdate(file.getBytes(), file.getContentType(), locale, method);
        } catch (final IOException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not import task categories %s", e);
            }
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
    }

    @PutMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<TaskCategoryDto>> update(@PathVariable final Long id, @Valid @RequestBody final TaskCategoryUpdateDto dto) {
        if (dto.getId() == null || !Objects.equals(id, dto.getId())) {
            return ResponseEntity.badRequest().build();
        }
        return service
                .update(dto)
                .map(updatedDto -> ResponseEntity.status(HttpStatus.ACCEPTED).body(EntityModel.of(updatedDto, getLinks(updatedDto))))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PatchMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<TaskCategoryDto>> partialUpdate(@PathVariable final Long id, @RequestBody final TaskCategoryUpdateDto dto) {
        if (dto.getId() == null || !Objects.equals(id, dto.getId())) {
            return ResponseEntity.badRequest().build();
        }
        return service
                .partialUpdate(dto)
                .map(updatedDto -> ResponseEntity.status(HttpStatus.ACCEPTED).body(EntityModel.of(updatedDto, getLinks(updatedDto))))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable final Long id) {
        return service
                .findById(id)
                .map(dto -> {
                    service.deleteById(id);
                    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private void addLinks(final EntityModel<TaskCategoryDto> entity) {
        if (entity != null && entity.getContent() != null) {
            addLinks(entity.getContent());
        }
    }

    void addLinks(final TaskCategoryDto dto) {
        dto.add(getLinks(dto));
    }

    List<Link> getLinks(final TaskCategoryDto dto) {
        final List<Link> links = new ArrayList<>();
        links.add(linkTo(methodOn(TaskCategoryApi.class).retrieve(dto.getId())).withSelfRel());
        links.add(linkTo(methodOn(TaskCategoryApi.class).retrieve(Pageable.unpaged())).withRel("task-categories"));
        return links;
    }
}
