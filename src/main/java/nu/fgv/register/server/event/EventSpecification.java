package nu.fgv.register.server.event;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public class EventSpecification {

    private EventSpecification() {
    }

    public static Specification<Event> hasSource(final Event.SourceType source) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Event_.source), source);
    }

    public static Specification<Event> hasCreatedAtGreaterThanEqual(final Instant createdAt) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get(Event_.createdAt), createdAt);
    }

}
