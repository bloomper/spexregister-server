package nu.fgv.register.server.spex;

import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/spex")
public class SpexApi {

    private final SpexService service;
    private final SpexMapper mapper = Mappers.getMapper(SpexMapper.class);

    public SpexApi(final SpexService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<SpexDto>> findAll() {
        return ResponseEntity.ok(mapper.toDtos(service.findAll()));
    }

    @PostMapping
    public ResponseEntity<SpexDto> create(@RequestBody SpexDto dto) {
        service.save(mapper.toModel(dto));

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpexDto> findById(@PathVariable Long id) {
        return service
                .findById(id)
                .map(e -> ResponseEntity.ok(mapper.toDto(e)))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SpexDto> update(@PathVariable Long id, @RequestBody SpexDto dto) {
        Spex entity = mapper.toModel(dto);
        entity.setId(id);

        service.save(entity);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/{id}/revival")
    public ResponseEntity<List<SpexDto>> findAllRevivals(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toDtos(service.findAllRevivals(id)));
    }

}
