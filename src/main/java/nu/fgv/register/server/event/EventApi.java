/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nu.fgv.register.server.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.news.NewsApi;
import nu.fgv.register.server.spex.SpexApi;
import nu.fgv.register.server.spex.category.SpexCategoryApi;
import nu.fgv.register.server.spexare.SpexareApi;
import nu.fgv.register.server.tag.TagApi;
import nu.fgv.register.server.task.TaskApi;
import nu.fgv.register.server.task.category.TaskCategoryApi;
import nu.fgv.register.server.user.UserApi;
import nu.fgv.register.server.util.security.RequiresAdmin;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/events", version = "1.0")
public class EventApi {

    private final EventService service;

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdmin
    public ResponseEntity<CollectionModel<EntityModel<EventDto>>> retrieve(@RequestParam final Event.SourceType sourceType, @RequestParam(defaultValue = "90") final Integer sinceInDays) {
        final List<EntityModel<EventDto>> events = service.findBySourceType(sourceType, sinceInDays).stream()
                .map(dto -> EntityModel.of(dto, getLinks(dto)))
                .toList();

        return ResponseEntity.ok(
                CollectionModel.of(events,
                        linkTo(methodOn(EventApi.class).retrieve(sourceType, -1)).withSelfRel()));
    }

    public List<Link> getLinks(final EventDto dto) {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(NewsApi.class).retrieveEvents(null, -1)).withRel("news-events"));
        links.add(linkTo(methodOn(SpexApi.class).retrieveEvents(null, -1)).withRel("spex-events"));
        links.add(linkTo(methodOn(SpexCategoryApi.class).retrieveEvents(null, -1)).withRel("spex-category-events"));
        links.add(linkTo(methodOn(SpexareApi.class).retrieveEvents(null, -1)).withRel("spexare-events"));
        links.add(linkTo(methodOn(TagApi.class).retrieveEvents(null, -1)).withRel("tag-events"));
        links.add(linkTo(methodOn(TaskApi.class).retrieveEvents(null, -1)).withRel("task-events"));
        links.add(linkTo(methodOn(TaskCategoryApi.class).retrieveEvents(null, -1)).withRel("task-category-events"));
        links.add(linkTo(methodOn(UserApi.class).retrieveEvents(null, -1)).withRel("user-events"));

        return links;
    }

}
