package nu.fgv.register.server.web.rest;

import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.dto.SpexDto;
import nu.fgv.register.server.mapper.SpexMapper;
import nu.fgv.register.server.model.Spex;
import nu.fgv.register.server.service.SpexService;
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
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/spex")
public class SpexController {

    private final SpexService service;
    private final SpexMapper mapper;

    public SpexController(final SpexService service, final SpexMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<SpexDto>> findAll() {
        return ResponseEntity.ok(mapper.toSpexDtos(service.findAll()));
    }

    @PostMapping
    public ResponseEntity<SpexDto> create(@RequestBody SpexDto dto) {
        service.save(mapper.toSpex(dto));

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpexDto> findById(@PathVariable Long id) {
        Optional<Spex> entity = service.findById(id);

        return ResponseEntity.ok(mapper.toSpexDto(entity.get()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SpexDto> update(@PathVariable Long id, @RequestBody SpexDto dto) {
        Spex entity = mapper.toSpex(dto);
        entity.setId(id);

        service.save(entity);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable Long id) {
        service.deleteById(id);

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
