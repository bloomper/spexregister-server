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

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class EventService {

    private final EventRepository repository;

    public List<EventDto> find(final Integer sinceInDays) {
        final List<EventDto> events = new ArrayList<>();

        Arrays.stream(Event.SourceType.values()).forEach(s ->
                events.addAll(
                        repository
                                .findAllBySourceSince(getInstantFromSinceInDays(sinceInDays), s, Sort.by("createdAt").descending())
                                .stream()
                                .map(EVENT_MAPPER::toDto)
                                .toList()
                )
        );

        return events;
    }

    public List<EventDto> findBySource(final Integer sinceInDays, final Event.SourceType source) {
        return repository
                .findAllBySourceSince(getInstantFromSinceInDays(sinceInDays), source, Sort.by("createdAt").descending())
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
