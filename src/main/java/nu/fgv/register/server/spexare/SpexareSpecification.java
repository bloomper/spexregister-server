package nu.fgv.register.server.spexare;

import lombok.Getter;
import nu.fgv.register.server.util.filter.BaseSpecification;
import nu.fgv.register.server.util.filter.FilterCriteria;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@Getter
public class SpexareSpecification extends BaseSpecification<Spexare> {

    public SpexareSpecification(final FilterCriteria criteria) {
        super(criteria);
    }

    public static Specification<Spexare> hasIds(final List<Long> ids) {
        return (root, query, criteriaBuilder) -> root.get(Spexare_.id).in(ids);
    }

}
