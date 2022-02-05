package nu.fgv.register.server.spex;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.FileUtil;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
import java.util.List;
import java.util.stream.Collectors;

import static nu.fgv.register.server.spex.SpexCategoryMapper.SPEX_CATEGORY_MAPPER;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/spex/category")
public class SpexCategoryApi {

    private final SpexCategoryService service;
    private final PagedResourcesAssembler<SpexCategoryDto> pagedResourcesAssembler;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<SpexCategoryDto>>> retrieve(@SortDefault(sort = "name", direction = Sort.Direction.ASC) final Pageable pageable) {
        final Page<SpexCategory> models = service.find(pageable);
        final List<SpexCategoryDto> dtos = toDtos(models.getContent());
        final PagedModel<EntityModel<SpexCategoryDto>> paged = pagedResourcesAssembler.toModel(new PageImpl<>(dtos, pageable, models.getTotalElements()));

        return ResponseEntity.ok(paged);
    }

    @PostMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<SpexCategoryDto> create(@RequestBody SpexCategoryDto dto) {
        final SpexCategory model = service.save(SPEX_CATEGORY_MAPPER.toModel(dto));

        final SpexCategoryDto newDto = toDto(model);

        return ResponseEntity.status(HttpStatus.CREATED).body(newDto);
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<SpexCategoryDto> retrieve(@PathVariable Long id) {
        return service
                .findById(id)
                .map(SPEX_CATEGORY_MAPPER::toDto)
                .map(this::addSelfLink)
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<SpexCategoryDto> update(@PathVariable Long id, @RequestBody SpexCategoryDto dto) {
        final SpexCategory model = SPEX_CATEGORY_MAPPER.toModel(dto);
        model.setId(id);

        final SpexCategory updatedModel = service.save(model);
        final SpexCategoryDto updatedDto = toDto(updatedModel);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(updatedDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/{id}/logo")
    public ResponseEntity<Resource> downloadLogo(@PathVariable Long id) {
        return service.findById(id)
                .map(entity -> {
                    final Resource resource = new ByteArrayResource(entity.getLogo());
                    return ResponseEntity.ok()
                            .contentType(MediaType.valueOf(entity.getLogoContentType()))
                            .body(resource);
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @RequestMapping(value = "/{id}/logo", method = {RequestMethod.POST, RequestMethod.PUT}, consumes = {"multipart/form-data"})
    public ResponseEntity<?> uploadLogo(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return service.findById(id)
                .map(entity -> {
                    try {
                        final byte[] binary = file.getBytes();
                        entity.setLogo(binary);
                        entity.setLogoContentType(hasText(file.getContentType()) ? file.getContentType() : FileUtil.detectMimeType(binary));
                    } catch (final IOException e) {
                        if (log.isErrorEnabled()) {
                            log.error(String.format("Could not save logo for spex category %s", entity.getId()), e);
                        }
                        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
                    }
                    service.save(entity);
                    return ResponseEntity.status(HttpStatus.ACCEPTED).build();
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    private SpexCategoryDto toDto(final SpexCategory model) {
        final SpexCategoryDto newDto = SPEX_CATEGORY_MAPPER.toDto(model);
        addSelfLink(newDto);
        return newDto;
    }

    private List<SpexCategoryDto> toDtos(final List<SpexCategory> models) {
        return models.stream()
                .map(SPEX_CATEGORY_MAPPER::toDto)
                .map(this::addSelfLink)
                .collect(Collectors.toList());
    }

    private SpexCategoryDto addSelfLink(final SpexCategoryDto dto) {
        final Link selfLink = WebMvcLinkBuilder.linkTo(SpexCategoryApi.class).slash(dto.getId()).withSelfRel();
        dto.add(selfLink);
        return dto;
    }
}
