package nu.fgv.register.server.spex;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import static nu.fgv.register.server.spex.SpexMapper.SPEX_MAPPER;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/spex")
public class SpexApi {

    private final SpexService service;
    private final PagedResourcesAssembler<SpexDto> pagedResourcesAssembler;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<SpexDto>>> retrieve(@SortDefault(sort = "year", direction = Sort.Direction.ASC) final Pageable pageable) {
        final Page<Spex> models = service.find(pageable);
        final List<SpexDto> dtos = toDtos(models.getContent());
        final PagedModel<EntityModel<SpexDto>> paged = pagedResourcesAssembler.toModel(new PageImpl<>(dtos, pageable, models.getTotalElements()));

        return ResponseEntity.ok(paged);
    }

    @PostMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<SpexDto> create(@RequestBody SpexDto dto) {
        final Spex model = service.save(SPEX_MAPPER.toModel(dto));

        final SpexDto newDto = toDto(model);

        return ResponseEntity.status(HttpStatus.CREATED).body(newDto);
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<SpexDto> retrieve(@PathVariable Long id) {
        return service
                .findById(id)
                .map(SPEX_MAPPER::toDto)
                .map(this::addSelfLink)
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<SpexDto> update(@PathVariable Long id, @RequestBody SpexDto dto) {
        final Spex model = SPEX_MAPPER.toModel(dto);
        model.setId(id);

        final Spex updatedModel = service.save(model);
        final SpexDto updatedDto = toDto(updatedModel);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(updatedDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping(value = "/{id}/revivals", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<List<SpexDto>> findAllRevivals(@PathVariable Long id) {
        final List<Spex> models = service.findAllRevivals(id);

        final List<SpexDto> dtos = toDtos(models);

        return ResponseEntity.ok(dtos);
    }

    private SpexDto toDto(final Spex model) {
        final SpexDto newDto = SPEX_MAPPER.toDto(model);
        addSelfLink(newDto);
        return newDto;
    }

    private List<SpexDto> toDtos(final List<Spex> models) {
        return models.stream()
                .map(SPEX_MAPPER::toDto)
                .map(this::addSelfLink)
                .collect(Collectors.toList());
    }

    private SpexDto addSelfLink(final SpexDto dto) {
        final Link selfLink = WebMvcLinkBuilder.linkTo(SpexApi.class).slash(dto.getId()).withSelfRel();
        dto.add(selfLink);
        return dto;
    }

}
