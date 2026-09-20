/*
 * Copyright 2026 the original author or authors.
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

package nu.fgv.register.server.spexare.bulk;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.SpexareApi;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static nu.fgv.register.server.spexare.Spexare_.PUBLISHED;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/spexare/bulk", version = "1.0")
public class SpexareBulkApi {

    private final SpexareBulkService service;

    @PostMapping(value = "/preview", produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<BulkResultDto>> preview(@Valid @RequestBody final SpexareBulkInputDto dto) {
        final BulkResultDto result = service.preview(dto);

        return ResponseEntity.ok(EntityModel.of(result, getLinks()));
    }

    @PostMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @RequiresAdminOrEditorOrUser
    public ResponseEntity<EntityModel<BulkResultDto>> apply(@Valid @RequestBody final SpexareBulkInputDto dto) {
        final BulkResultDto result = service.apply(dto);

        return ResponseEntity.ok(EntityModel.of(result, getLinks()));
    }

    List<Link> getLinks() {
        final List<Link> links = new ArrayList<>();

        links.add(linkTo(methodOn(SpexareBulkApi.class).apply(null)).withSelfRel());
        links.add(linkTo(methodOn(SpexareBulkApi.class).preview(null)).withRel("preview"));
        links.add(linkTo(methodOn(SpexareApi.class).retrieve(Pageable.unpaged(), PUBLISHED + ":true")).withRel("spexare"));

        return links;
    }

}
