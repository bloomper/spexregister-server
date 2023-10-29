package nu.fgv.register.server.spex.category;

import lombok.Getter;
import nu.fgv.register.server.util.filter.BaseSpecification;
import nu.fgv.register.server.util.filter.FilterCriteria;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@Getter
public class SpexCategorySpecification extends BaseSpecification<SpexCategory> {

    public SpexCategorySpecification(final FilterCriteria criteria) {
        super(criteria);
    }

    public static Specification<SpexCategory> hasIds(final List<Long> ids) {
        return (root, query, criteriaBuilder) -> root.get(SpexCategory_.id).in(ids);
    }

}
