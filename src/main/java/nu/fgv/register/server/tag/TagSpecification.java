package nu.fgv.register.server.tag;

import lombok.Getter;
import nu.fgv.register.server.util.filter.BaseSpecification;
import nu.fgv.register.server.util.filter.FilterCriteria;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@Getter
public class TagSpecification extends BaseSpecification<Tag> {

    public TagSpecification(final FilterCriteria criteria) {
        super(criteria);
    }

    public static Specification<Tag> hasIds(final List<Long> ids) {
        return (root, query, criteriaBuilder) -> root.get(Tag_.id).in(ids);
    }

}
