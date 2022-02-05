package nu.fgv.register.server.spex;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/spex")
public class SpexApi {

    private final SpexService service;
    private final PagedResourcesAssembler<SpexDto> pagedResourcesAssembler;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<SpexDto>>> retrieve(@SortDefault(sort = "year", direction = Sort.Direction.ASC) final Pageable pageable) {
        final PagedModel<EntityModel<SpexDto>> paged = pagedResourcesAssembler.toModel(service.find(pageable));
        // TODO: Add affordances

        return ResponseEntity.ok(paged);
    }

    @PostMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<SpexDto> create(@RequestBody SpexDto dto) {
        final SpexDto newDto = service.save(dto);
        addSelfLink(newDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(newDto);
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<SpexDto> retrieve(@PathVariable Long id) {
        return service
                .findById(id)
                .map(this::addSelfLink)
                // TODO: Add affordances
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<SpexDto> update(@PathVariable Long id, @RequestBody SpexDto dto) {
        dto.setId(id);
        final SpexDto updatedDto = service.save(dto);
        addSelfLink(updatedDto);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(updatedDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping(value = "/{id}/revivals", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<List<SpexDto>> findAllRevivals(@PathVariable Long id) {
        // TODO: Pagination
        final List<SpexDto> dtos = service.findAllRevivals(id);

        return ResponseEntity.ok(dtos);
    }

    private SpexDto addSelfLink(final SpexDto dto) {
        final Link selfLink = WebMvcLinkBuilder.linkTo(SpexApi.class).slash(dto.getId()).withSelfRel();
        dto.add(selfLink);
        return dto;
    }

}
