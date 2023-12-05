package nu.fgv.register.server.news;

import lombok.Getter;
import nu.fgv.register.server.util.filter.BaseSpecification;
import nu.fgv.register.server.util.filter.FilterCriteria;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

@Getter
public class NewsSpecification extends BaseSpecification<News> {

    public NewsSpecification(final FilterCriteria criteria) {
        super(criteria);
    }

    public static Specification<News> hasVisibleFromAfterYesterday() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThan(root.get(News_.visibleFrom), LocalDate.now().minusDays(1));
    }

    public static Specification<News> hasVisibleToBeforeToday() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.lessThan(root.get(News_.visibleTo), LocalDate.now());
    }

    public static Specification<News> hasVisibleToToday() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(News_.visibleTo), LocalDate.now());
    }

    public static Specification<News> hasVisibleToAfterToday() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThan(root.get(News_.visibleTo), LocalDate.now());
    }
}
