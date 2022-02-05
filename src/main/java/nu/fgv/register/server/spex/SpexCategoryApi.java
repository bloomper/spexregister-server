package nu.fgv.register.server.spex;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.SortDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/spex/category")
public class SpexCategoryApi {

    private final SpexCategoryService service;
    private final PagedResourcesAssembler<SpexCategoryDto> pagedResourcesAssembler;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<SpexCategoryDto>>> retrieve(@SortDefault(sort = "name", direction = Sort.Direction.ASC) final Pageable pageable) {
        final PagedModel<EntityModel<SpexCategoryDto>> paged = pagedResourcesAssembler.toModel(service.find(pageable));
        // TODO: Add affordances

        return ResponseEntity.ok(paged);
    }

    @PostMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<SpexCategoryDto> create(@RequestBody SpexCategoryDto dto) {
        final SpexCategoryDto newDto = service.save(dto);
        addSelfLink(newDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(newDto);
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<SpexCategoryDto> retrieve(@PathVariable Long id) {
        return service
                .findById(id)
                .map(this::addSelfLink)
                // TODO: Add affordances
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<SpexCategoryDto> update(@PathVariable Long id, @RequestBody SpexCategoryDto dto) {
        dto.setId(id);
        final SpexCategoryDto updatedDto = service.save(dto);
        addSelfLink(updatedDto);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(updatedDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/{id}/logo")
    public ResponseEntity<Resource> downloadLogo(@PathVariable Long id) {
        return service.getLogo(id)
                .map(tuple -> {
                    final Resource resource = new ByteArrayResource(tuple.getFirst());
                    return ResponseEntity.ok()
                            .contentType(MediaType.valueOf(tuple.getSecond()))
                            .body(resource);
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @RequestMapping(value = "/{id}/logo", method = {RequestMethod.POST, RequestMethod.PUT}, consumes = {"multipart/form-data"})
    public ResponseEntity<?> uploadLogo(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            return service.saveLogo(id, file.getBytes(), file.getContentType())
                    .map(entity -> ResponseEntity.status(HttpStatus.ACCEPTED).build())
                    .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error(String.format("Could not save logo for spex category %s", id), e);
            }
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
    }

    private SpexCategoryDto addSelfLink(final SpexCategoryDto dto) {
        final Link selfLink = WebMvcLinkBuilder.linkTo(SpexCategoryApi.class).slash(dto.getId()).withSelfRel();
        dto.add(selfLink);
        return dto;
    }
}
