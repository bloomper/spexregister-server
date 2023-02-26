package nu.fgv.register.server.spexare.membership;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.SpexareApi;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.SortDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/spexare/{spexareId}/membership")
public class MembershipApi {

    private final MembershipService service;
    private final PagedResourcesAssembler<MembershipDto> pagedResourcesAssembler;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<MembershipDto>>> retrieveMemberships(@PathVariable final Long spexareId, @SortDefault(sort = "year", direction = Sort.Direction.ASC) final Pageable pageable) {
        final PagedModel<EntityModel<MembershipDto>> paged = pagedResourcesAssembler.toModel(service.findBySpexare(spexareId, pageable));
        paged.getContent().forEach(p -> addLinks(p, spexareId));

        return ResponseEntity.ok(paged);
    }

    @GetMapping(value = "/type/{type}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<MembershipDto>>> retrieveMembershipsByType(@PathVariable final Long spexareId, @PathVariable final String type, @SortDefault(sort = "year", direction = Sort.Direction.ASC) final Pageable pageable) {
        final PagedModel<EntityModel<MembershipDto>> paged = pagedResourcesAssembler.toModel(service.findBySpexareAndType(spexareId, type, pageable));
        paged.getContent().forEach(p -> addLinks(p, spexareId));

        return ResponseEntity.ok(paged);
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<MembershipDto>> retrieve(@PathVariable final Long spexareId, @PathVariable final Long id) {
        return service
                .findById(id)
                .map(dto -> EntityModel.of(dto, getLinks(dto, spexareId)))
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping(value = "/{type}/{year}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<MembershipDto>> addMembership(@PathVariable final Long spexareId, @PathVariable final String type, @PathVariable final String year) {
        return service
                .addMembership(spexareId, type, year)
                .map(dto -> ResponseEntity.status(HttpStatus.ACCEPTED).body(EntityModel.of(dto, getLinks(dto, spexareId))))
                .orElse(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }

    @DeleteMapping(value = "/{type}/{year}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<?> removeMembership(@PathVariable final Long spexareId, @PathVariable final String type, @PathVariable final String year) {
        return service.removeMembership(spexareId, type, year) ? ResponseEntity.status(HttpStatus.NO_CONTENT).build() : ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    private void addLinks(final EntityModel<MembershipDto> entity, final Long spexareId) {
        if (entity != null && entity.getContent() != null) {
            entity.getContent().add(getLinks(entity.getContent(), spexareId));
        }
    }

    void addLinks(final MembershipDto dto, final Long spexareId) {
        dto.add(getLinks(dto, spexareId));
    }

    List<Link> getLinks(final MembershipDto dto, final Long spexareId) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(MembershipApi.class).retrieve(spexareId, dto.getId())).withSelfRel());

        final Link membershipsLink = linkTo(methodOn(MembershipApi.class).retrieveMemberships(dto.getId(), null)).withRel("memberships");
        links.add(membershipsLink);

        final Link spexareLink = linkTo(methodOn(SpexareApi.class).retrieve(spexareId)).withRel("spexare");
        links.add(spexareLink);

        return links;
    }

}
