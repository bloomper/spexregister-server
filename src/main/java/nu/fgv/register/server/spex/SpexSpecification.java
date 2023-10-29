package nu.fgv.register.server.spex;

import lombok.Getter;
import nu.fgv.register.server.util.filter.BaseSpecification;
import nu.fgv.register.server.util.filter.FilterCriteria;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@Getter
public class SpexSpecification extends BaseSpecification<Spex> {

    public SpexSpecification(final FilterCriteria criteria) {
        super(criteria);
    }

    public static Specification<Spex> isRevival() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNotNull(root.get(Spex_.parent));
    }

    public static Specification<Spex> isNotRevival() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get(Spex_.parent));
    }

    public static Specification<Spex> hasParent(final Spex parent) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Spex_.parent), parent);
    }

    public static Specification<Spex> hasYear(final String year) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Spex_.year), year);
    }

    public static Specification<Spex> hasIds(final List<Long> ids) {
        return (root, query, criteriaBuilder) -> root.get(Spex_.id).in(ids);
    }

    public static Specification<Spex> hasParentIds(final List<Long> parentIds) {
        return (root, query, criteriaBuilder) -> root.join(Spex_.parent).get(Spex_.id).in(parentIds);
    }
}
