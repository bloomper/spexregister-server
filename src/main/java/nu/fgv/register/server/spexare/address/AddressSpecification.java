package nu.fgv.register.server.spexare.address;

import lombok.Getter;
import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.settings.Type_;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.util.filter.BaseSpecification;
import nu.fgv.register.server.util.filter.FilterCriteria;
import org.springframework.data.jpa.domain.Specification;

@Getter
public class AddressSpecification extends BaseSpecification<Address> {

    public AddressSpecification(final FilterCriteria criteria) {
        super(criteria);
    }

    public static Specification<Address> hasId(final Long id) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Address_.id), id);
    }

    public static Specification<Address> hasSpexare(final Spexare spexare) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Address_.spexare), spexare);
    }

    public static Specification<Address> hasType(final Type type) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Address_.type), type);
    }

    public static Specification<Address> hasType(final TypeType type) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.join(Address_.type).get(Type_.type), type);
    }

}
