package nu.fgv.register.server.util.filter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import org.springframework.data.jpa.domain.Specification;

@Getter
public class BaseSpecification<T> implements Specification<T> {

    private final FilterCriteria criteria;

    public BaseSpecification(final FilterCriteria criteria) {
        this.criteria = criteria;
    }

    @Override
    public Predicate toPredicate(final Root<T> root, final CriteriaQuery<?> query, final CriteriaBuilder builder) {
        return switch (criteria.getOperation()) {
            case EQUALITY -> {
                if (FilterOperation.NULL.equalsIgnoreCase((String) criteria.getValue())) {
                    yield builder.isNull(root.get(criteria.getKey()));
                } else {
                    yield builder.equal(root.get(criteria.getKey()), criteria.getValue());
                }
            }
            case NEGATION -> {
                if (FilterOperation.NULL.equalsIgnoreCase((String) criteria.getValue())) {
                    yield builder.isNotNull(root.get(criteria.getKey()));
                } else {
                    yield builder.notEqual(root.get(criteria.getKey()), criteria.getValue());
                }
            }
            case GREATER_THAN -> builder.greaterThan(root.get(criteria.getKey()), criteria.getValue().toString());
            case LESS_THAN -> builder.lessThan(root.get(criteria.getKey()), criteria.getValue().toString());
            case LIKE -> builder.like(root.get(criteria.getKey()), criteria.getValue().toString());
            case STARTS_WITH -> builder.like(root.get(criteria.getKey()), criteria.getValue() + "%");
            case ENDS_WITH -> builder.like(root.get(criteria.getKey()), "%" + criteria.getValue());
            case CONTAINS -> builder.like(root.get(criteria.getKey()), "%" + criteria.getValue() + "%");
        };
    }

}
