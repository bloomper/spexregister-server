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

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.news.NewsRepository;
import nu.fgv.register.server.spex.SpexRepository;
import nu.fgv.register.server.spex.category.SpexCategoryRepository;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.tag.TagRepository;
import nu.fgv.register.server.task.TaskRepository;
import nu.fgv.register.server.task.category.TaskCategoryRepository;
import nu.fgv.register.server.user.UserRepository;
import nu.fgv.register.server.util.AbstractAuditable;
import nu.fgv.register.server.util.security.RequiresAdmin;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static nu.fgv.register.server.event.EventMapper.EVENT_MAPPER;
import static nu.fgv.register.server.event.EventSpecification.hasCreatedAtGreaterThanEqual;
import static nu.fgv.register.server.event.EventSpecification.hasSourceId;
import static nu.fgv.register.server.event.EventSpecification.hasSourceType;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class EventService {

    private final EventRepository repository;
    private final NewsRepository newsRepository;
    private final SpexRepository spexRepository;
    private final SpexCategoryRepository spexCategoryRepository;
    private final SpexareRepository spexareRepository;
    private final TagRepository tagRepository;
    private final TaskRepository taskRepository;
    private final TaskCategoryRepository taskCategoryRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;

    @RequiresAdmin
    public List<EventDto> findBySourceType(final Event.SourceType sourceType, final Integer sinceInDays) {
        return repository
                .findAll(hasCreatedAtGreaterThanEqual(getInstantFromSinceInDays(sinceInDays)).and(hasSourceType(sourceType)), Sort.by("createdAt").descending())
                .stream()
                .map(EVENT_MAPPER::toDto)
                .toList();
    }

    @RequiresAdminOrEditorOrUser
    public List<EventDto> findBySourceTypeAndId(final Event.SourceType source, final Long sourceId, final Integer sinceInDays) {
        final Object targetEntity = fetchTargetEntity(source, sourceId);

        if (targetEntity != null) {
            permissionService.checkReadPermission(targetEntity);

            return repository
                    .findAll(hasCreatedAtGreaterThanEqual(getInstantFromSinceInDays(sinceInDays)).and(hasSourceType(source)).and(hasSourceId(sourceId)), Sort.by("createdAt").descending())
                    .stream()
                    .map(EVENT_MAPPER::toDto)
                    .toList();
        } else {
            return Collections.emptyList();
        }
    }

    private EventDto create(final String createdBy, final Event.EventType event, final Event.SourceType source, final Long sourceId) {
        final Event model = EVENT_MAPPER.toModel(createdBy, event, source, sourceId);
        return EVENT_MAPPER.toDto(repository.save(model));
    }

    @TransactionalEventListener
    @Async
    public void onEvent(final SpringEvent springEvent) {
        if (springEvent.getSource() instanceof final AbstractAuditable auditable) {
            create(auditable.getLastModifiedBy() != null ? auditable.getLastModifiedBy() : auditable.getCreatedBy(), springEvent.getEvent(), springEvent.getSourceType(), springEvent.getSourceId());
        }
    }

    private @Nullable Object fetchTargetEntity(final Event.SourceType source, final Long id) {
        return switch (source) {
            case NEWS -> newsRepository.findById(id).orElse(null);
            case SPEX -> spexRepository.findById(id).orElse(null);
            case SPEX_CATEGORY -> spexCategoryRepository.findById(id).orElse(null);
            case SPEXARE -> spexareRepository.findById(id).orElse(null);
            case TAG -> tagRepository.findById(id).orElse(null);
            case TASK -> taskRepository.findById(id).orElse(null);
            case TASK_CATEGORY -> taskCategoryRepository.findById(id).orElse(null);
            case USER -> userRepository.findById(id).orElse(null);
        };
    }

    private Instant getInstantFromSinceInDays(final Integer sinceInDays) {
        return LocalDate.now().minusDays(sinceInDays != -1 ? sinceInDays : 90).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
}
