package nu.fgv.register.server.task.category;

import lombok.Getter;
import nu.fgv.register.server.util.filter.BaseSpecification;
import nu.fgv.register.server.util.filter.FilterCriteria;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@Getter
public class TaskCategorySpecification extends BaseSpecification<TaskCategory> {

    public TaskCategorySpecification(final FilterCriteria criteria) {
        super(criteria);
    }

    public static Specification<TaskCategory> hasIds(final List<Long> ids) {
        return (root, query, criteriaBuilder) -> root.get(TaskCategory_.id).in(ids);
    }

}
