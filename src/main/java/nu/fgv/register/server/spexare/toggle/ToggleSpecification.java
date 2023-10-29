package nu.fgv.register.server.spexare.toggle;

import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.settings.Type_;
import nu.fgv.register.server.spexare.Spexare;
import org.springframework.data.jpa.domain.Specification;

public class ToggleSpecification {

    public static Specification<Toggle> hasId(final Long id) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Toggle_.id), id);
    }

    public static Specification<Toggle> hasSpexare(final Spexare spexare) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Toggle_.spexare), spexare);
    }

    public static Specification<Toggle> hasType(final Type type) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Toggle_.type), type);
    }

    public static Specification<Toggle> hasType(final TypeType type) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.join(Toggle_.type).get(Type_.type), type);
    }

}
