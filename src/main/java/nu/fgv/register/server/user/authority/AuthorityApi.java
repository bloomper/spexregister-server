package nu.fgv.register.server.user.authority;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users/authorities")
public class AuthorityApi {

    private final AuthorityService service;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<CollectionModel<EntityModel<AuthorityDto>>> retrieve(@SortDefault(sort = "id", direction = Sort.Direction.ASC) final Sort sort) {
        final List<EntityModel<AuthorityDto>> authorities = service.findAll(sort).stream()
                .map(dto -> EntityModel.of(dto, getLinks(dto)))
                .toList();

        return ResponseEntity.ok(
                CollectionModel.of(authorities,
                        linkTo(methodOn(AuthorityApi.class).retrieve(Sort.unsorted())).withSelfRel()));
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<AuthorityDto>> retrieve(@PathVariable final String id) {
        return service
                .findById(id)
                .map(dto -> EntityModel.of(dto, getLinks(dto)))
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    private void addLinks(final EntityModel<AuthorityDto> entity) {
        if (entity != null && entity.getContent() != null) {
            entity.getContent().add(getLinks(entity.getContent()));
        }
    }

    void addLinks(final AuthorityDto dto) {
        dto.add(getLinks(dto));
    }

    public List<Link> getLinks(final AuthorityDto dto) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(AuthorityApi.class).retrieve(dto.getId())).withSelfRel());
        links.add(linkTo(methodOn(AuthorityApi.class).retrieve(Sort.unsorted())).withRel("authorities"));

        return links;
    }

}
