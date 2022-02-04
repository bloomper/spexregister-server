package nu.fgv.register.server.spex;

import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.FileUtil;
import org.mapstruct.factory.Mappers;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
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
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;

import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RestController
@RequestMapping("/api/v1/spex/category")
public class SpexCategoryApi {

    private final SpexCategoryService service;
    private final SpexCategoryMapper mapper = Mappers.getMapper(SpexCategoryMapper.class);

    public SpexCategoryApi(final SpexCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<SpexCategoryDto>> findAll() {
        return ResponseEntity.ok(mapper.toDtos(service.findAll()));
    }

    @PostMapping
    public ResponseEntity<SpexCategoryDto> create(@RequestBody SpexCategoryDto dto) {
        service.save(mapper.toModel(dto));

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpexCategoryDto> findById(@PathVariable Long id) {
        return service
                .findById(id)
                .map(e -> ResponseEntity.ok(mapper.toDto(e)))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SpexCategoryDto> update(@PathVariable Long id, @RequestBody SpexCategoryDto dto) {
        SpexCategory entity = mapper.toModel(dto);
        entity.setId(id);

        service.save(entity);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(dto);
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
}
