package nu.fgv.register.server.spexare.membership;

import lombok.Getter;
import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.settings.Type_;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.util.filter.BaseSpecification;
import nu.fgv.register.server.util.filter.FilterCriteria;
import org.springframework.data.jpa.domain.Specification;

@Getter
public class MembershipSpecification extends BaseSpecification<Membership> {

    public MembershipSpecification(final FilterCriteria criteria) {
        super(criteria);
    }

    public static Specification<Membership> hasId(final Long id) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Membership_.id), id);
    }

    public static Specification<Membership> hasSpexare(final Spexare spexare) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Membership_.spexare), spexare);
    }

    public static Specification<Membership> hasYear(final String year) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Membership_.year), year);
    }

    public static Specification<Membership> hasType(final Type type) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Membership_.type), type);
    }

    public static Specification<Membership> hasType(final TypeType type) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.join(Membership_.type).get(Type_.type), type);
    }
}
