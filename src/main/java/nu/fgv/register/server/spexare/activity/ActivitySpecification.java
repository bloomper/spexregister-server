package nu.fgv.register.server.spexare.activity;

import nu.fgv.register.server.spexare.Spexare;
import org.springframework.data.jpa.domain.Specification;

public class ActivitySpecification {

    private ActivitySpecification() {
    }

    public static Specification<Activity> hasId(final Long id) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Activity_.id), id);
    }

    public static Specification<Activity> hasSpexare(final Spexare spexare) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Activity_.spexare), spexare);
    }

}
