package nu.fgv.register.server.spexare.consent;

import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.settings.Type_;
import nu.fgv.register.server.spexare.Spexare;
import org.springframework.data.jpa.domain.Specification;

public class ConsentSpecification {

    public static Specification<Consent> hasId(final Long id) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Consent_.id), id);
    }

    public static Specification<Consent> hasSpexare(final Spexare spexare) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Consent_.spexare), spexare);
    }

    public static Specification<Consent> hasType(final Type type) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Consent_.type), type);
    }

    public static Specification<Consent> hasType(final TypeType type) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.join(Consent_.type).get(Type_.type), type);
    }

}
