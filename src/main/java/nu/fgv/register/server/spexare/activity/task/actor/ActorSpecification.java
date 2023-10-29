package nu.fgv.register.server.spexare.activity.task.actor;

import lombok.Getter;
import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.spexare.activity.task.TaskActivity;
import nu.fgv.register.server.util.filter.BaseSpecification;
import nu.fgv.register.server.util.filter.FilterCriteria;
import org.springframework.data.jpa.domain.Specification;

@Getter
public class ActorSpecification extends BaseSpecification<Actor> {

    public ActorSpecification(final FilterCriteria criteria) {
        super(criteria);
    }

    public static Specification<Actor> hasId(final Long id) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Actor_.id), id);
    }

    public static Specification<Actor> hasTaskActivity(final TaskActivity taskActivity) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Actor_.taskActivity), taskActivity);
    }

    public static Specification<Actor> hasVocal(final Type vocal) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Actor_.vocal), vocal);
    }

}
