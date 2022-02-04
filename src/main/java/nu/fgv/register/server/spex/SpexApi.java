package nu.fgv.register.server.spex;

import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
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
import java.util.stream.Collectors;

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
        final List<Spex> models = service.findAll();

        final List<SpexDto> dtos = models.stream()
                .map(mapper::toDto)
                .map(this::addSelfLink)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<SpexDto> create(@RequestBody SpexDto dto) {
        final Spex model = service.save(mapper.toModel(dto));

        final SpexDto newDto = mapper.toDto(model);
        addSelfLink(newDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(newDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpexDto> findById(@PathVariable Long id) {
        return service
                .findById(id)
                .map(mapper::toDto)
                .map(this::addSelfLink)
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SpexDto> update(@PathVariable Long id, @RequestBody SpexDto dto) {
        final Spex model = mapper.toModel(dto);
        model.setId(id);

        final Spex updatedModel = service.save(model);
        final SpexDto updatedDto = mapper.toDto(updatedModel);
        addSelfLink(updatedDto);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(updatedDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/{id}/revival")
    public ResponseEntity<List<SpexDto>> findAllRevivals(@PathVariable Long id) {
        final List<Spex> models = service.findAllRevivals(id);

        final List<SpexDto> dtos = models.stream()
                .map(mapper::toDto)
                .map(this::addSelfLink)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    private SpexDto addSelfLink(final SpexDto dto) {
        final Link selfLink = WebMvcLinkBuilder.linkTo(SpexApi.class).slash(dto.getId()).withSelfRel();
        dto.add(selfLink);
        return dto;
    }

}
