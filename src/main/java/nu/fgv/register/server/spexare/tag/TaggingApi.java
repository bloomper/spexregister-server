package nu.fgv.register.server.spexare.tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.SpexareApi;
import nu.fgv.register.server.tag.TagDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.SortDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/spexare/{spexareId}/tags")
public class TaggingApi {

    private final TaggingService service;
    private final PagedResourcesAssembler<TagDto> pagedResourcesAssembler;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<PagedModel<EntityModel<TagDto>>> retrieve(@PathVariable final Long spexareId, @SortDefault(sort = "name", direction = Sort.Direction.ASC) final Pageable pageable) {
        try {
            final PagedModel<EntityModel<TagDto>> paged = pagedResourcesAssembler.toModel(service.findBySpexare(spexareId, pageable));
            paged.getContent().forEach(p -> addLinks(p, spexareId));

            return ResponseEntity.ok(paged);
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not retrieve tags", e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<?> create(@PathVariable final Long spexareId, @PathVariable final Long id) {
        try {
            return service.create(spexareId, id) ?
                    ResponseEntity
                            .status(HttpStatus.CREATED)
                            .header(HttpHeaders.LOCATION, linkTo(methodOn(TaggingApi.class).retrieve(spexareId, Pageable.unpaged())).toString())
                            .build() :
                    ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not create tag {}", id, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<?> delete(@PathVariable final Long spexareId, @PathVariable final Long id) {
        try {
            return service.deleteById(spexareId, id) ? ResponseEntity.status(HttpStatus.NO_CONTENT).build() : ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (final ResourceNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not delete tag {}", id, e);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    private void addLinks(final EntityModel<TagDto> entity, final Long spexareId) {
        if (entity != null && entity.getContent() != null) {
            entity.getContent().add(getLinks(entity.getContent(), spexareId));
        }
    }

    void addLinks(final TagDto dto, final Long spexareId) {
        dto.add(getLinks(dto, spexareId));
    }

    List<Link> getLinks(final TagDto dto, final Long spexareId) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(TaggingApi.class).retrieve(spexareId, Pageable.unpaged())).withRel("tags"));
        links.add(linkTo(methodOn(SpexareApi.class).retrieve(spexareId)).withRel("spexare"));

        return links;
    }

}
