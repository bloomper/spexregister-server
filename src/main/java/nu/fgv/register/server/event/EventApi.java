package nu.fgv.register.server.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.news.NewsApi;
import nu.fgv.register.server.session.SessionApi;
import nu.fgv.register.server.spex.SpexApi;
import nu.fgv.register.server.spex.SpexCategoryApi;
import nu.fgv.register.server.spexare.SpexareApi;
import nu.fgv.register.server.tag.TagApi;
import nu.fgv.register.server.task.TaskApi;
import nu.fgv.register.server.task.TaskCategoryApi;
import nu.fgv.register.server.user.UserApi;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/events")
public class EventApi {

    private final EventService service;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<CollectionModel<EntityModel<EventDto>>> retrieve(@RequestParam(defaultValue = "90") final Integer sinceInDays) {
        final List<EntityModel<EventDto>> events = service.find(sinceInDays).stream()
                .map(dto -> EntityModel.of(dto, getLinks(dto)))
                .toList();

        return ResponseEntity.ok(
                CollectionModel.of(events,
                        linkTo(methodOn(EventApi.class).retrieve(null)).withSelfRel()));
    }

    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<EntityModel<EventDto>> retrieveById(@PathVariable final Long id) {
        return service
                .findById(id)
                .map(dto -> EntityModel.of(dto, getLinks(dto)))
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    public List<Link> getLinks(final EventDto dto) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(EventApi.class).retrieveById(dto.getId())).withSelfRel());
        links.add(linkTo(methodOn(NewsApi.class).retrieveEvents(null)).withRel("news-events"));
        links.add(linkTo(methodOn(SessionApi.class).retrieveEvents(null)).withRel("session-events"));
        links.add(linkTo(methodOn(SpexApi.class).retrieveEvents(null)).withRel("spex-events"));
        links.add(linkTo(methodOn(SpexCategoryApi.class).retrieveEvents(null)).withRel("spex-category-events"));
        links.add(linkTo(methodOn(SpexareApi.class).retrieveEvents(null)).withRel("spexare-events"));
        links.add(linkTo(methodOn(TagApi.class).retrieveEvents(null)).withRel("tag-events"));
        links.add(linkTo(methodOn(TaskApi.class).retrieveEvents(null)).withRel("task-events"));
        links.add(linkTo(methodOn(TaskCategoryApi.class).retrieveEvents(null)).withRel("task-category-events"));
        links.add(linkTo(methodOn(UserApi.class).retrieveEvents(null)).withRel("user-events"));

        return links;
    }

}
