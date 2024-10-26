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
import nu.fgv.register.server.util.AbstractAuditable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static nu.fgv.register.server.event.EventMapper.EVENT_MAPPER;
import static nu.fgv.register.server.event.EventSpecification.hasCreatedAtGreaterThanEqual;
import static nu.fgv.register.server.event.EventSpecification.hasSource;

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

    public List<EventDto> find(final Integer sinceInDays) {
        final List<EventDto> events = new ArrayList<>();

        Arrays.stream(Event.SourceType.values()).forEach(source ->
                events.addAll(
                        repository
                                .findAll(hasCreatedAtGreaterThanEqual(getInstantFromSinceInDays(sinceInDays)).and(hasSource(source)), Sort.by("createdAt").descending())
                                .stream()
                                .map(EVENT_MAPPER::toDto)
                                .toList()
                )
        );

        return events;
    }

    public List<EventDto> findBySource(final Integer sinceInDays, final Event.SourceType source) {
        return repository
                .findAll(hasCreatedAtGreaterThanEqual(getInstantFromSinceInDays(sinceInDays)).and(hasSource(source)), Sort.by("createdAt").descending())
                .stream()
                .map(EVENT_MAPPER::toDto)
                .toList();
    }

    public Optional<EventDto> findById(final Long id) {
        return repository
                .findById(id)
                .map(EVENT_MAPPER::toDto);
    }

    public EventDto create(final String createdBy, final Event.EventType event, Event.SourceType source) {
        final Event model = EVENT_MAPPER.toModel(createdBy, event, source);
        return EVENT_MAPPER.toDto(repository.save(model));
    }

    @TransactionalEventListener
    @Async
    public void onEvent(final SpringEvent springEvent) {
        if (springEvent.getSource() instanceof AbstractAuditable auditable) {
            create(auditable.getCreatedBy(), springEvent.getEvent(), springEvent.getSourceType());
        }
    }

    private Instant getInstantFromSinceInDays(final Integer sinceInDays) {
        return LocalDate.now().minusDays(sinceInDays != null ? sinceInDays : 90).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
}
