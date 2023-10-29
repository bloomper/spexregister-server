package nu.fgv.register.server.task;

import lombok.Getter;
import nu.fgv.register.server.util.filter.BaseSpecification;
import nu.fgv.register.server.util.filter.FilterCriteria;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@Getter
public class TaskSpecification extends BaseSpecification<Task> {

    public TaskSpecification(final FilterCriteria criteria) {
        super(criteria);
    }

    public static Specification<Task> hasIds(final List<Long> ids) {
        return (root, query, criteriaBuilder) -> root.get(Task_.id).in(ids);
    }

}
