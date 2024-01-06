package nu.fgv.register.server.spexare.activity.spex;

import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spexare.activity.Activity;
import org.springframework.data.jpa.domain.Specification;

public class SpexActivitySpecification {

    private SpexActivitySpecification() {
    }

    public static Specification<SpexActivity> hasId(final Long id) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(SpexActivity_.id), id);
    }

    public static Specification<SpexActivity> hasActivity(final Activity activity) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(SpexActivity_.activity), activity);
    }

    public static Specification<SpexActivity> hasSpex(final Spex spex) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(SpexActivity_.spex), spex);
    }

}
