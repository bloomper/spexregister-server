package nu.fgv.register.server.util.filter;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SpecificationsBuilder<T> {

    private final List<FilterCriteria> params;

    private SpecificationsBuilder() {
        params = new ArrayList<>();
    }

    public static <T> SpecificationsBuilder<T> builder() {
        return new SpecificationsBuilder<>();
    }

    public final SpecificationsBuilder<T> with(final String key, final String operation, final Object value, final String prefix, final String suffix) {
        return with(null, key, operation, value, prefix, suffix);
    }

    public final SpecificationsBuilder<T> with(final String orPredicate, final String key, final String operation, final Object value, final String prefix, final String suffix) {
        FilterOperation op = FilterOperation.getSimpleOperation(operation.charAt(0));

        if (op != null) {
            if (op == FilterOperation.EQUALITY) {
                final boolean startWithAsterisk = prefix != null && prefix.contains(FilterOperation.ZERO_OR_MORE_REGEX);
                final boolean endWithAsterisk = suffix != null && suffix.contains(FilterOperation.ZERO_OR_MORE_REGEX);

                if (startWithAsterisk && endWithAsterisk) {
                    op = FilterOperation.CONTAINS;
                } else if (startWithAsterisk) {
                    op = FilterOperation.ENDS_WITH;
                } else if (endWithAsterisk) {
                    op = FilterOperation.STARTS_WITH;
                }
            }
            params.add(new FilterCriteria(orPredicate, key, op, value));
        }

        return this;
    }

    public Specification<T> build(final Function<FilterCriteria, Specification<T>> converter) {
        if (params.isEmpty()) {
            return null;
        }

        final List<Specification<T>> specifications = params.stream()
                .map(converter)
                .collect(Collectors.toCollection(ArrayList::new));

        Specification<T> result = specifications.getFirst();

        for (int i = 1; i < specifications.size(); i++) {
            result = params.get(i).isOrPredicate()
                    ? Specification.where(result).or(specifications.get(i))
                    : Specification.where(result).and(specifications.get(i));
        }

        return result;
    }

    public Specification<T> build(final Deque<?> postFixedExpressionStack, final Function<FilterCriteria, Specification<T>> converter) {
        final Deque<Specification<T>> specificationStack = new LinkedList<>();

        Collections.reverse((List<?>) postFixedExpressionStack);

        while (!postFixedExpressionStack.isEmpty()) {
            final Object mayBeOperand = postFixedExpressionStack.pop();

            if (!(mayBeOperand instanceof String)) {
                specificationStack.push(converter.apply((FilterCriteria) mayBeOperand));
            } else {
                final Specification<T> operand1 = specificationStack.pop();
                final Specification<T> operand2 = specificationStack.pop();

                if (mayBeOperand.equals(FilterOperation.AND_OPERATOR)) {
                    specificationStack.push(Specification.where(operand1).and(operand2));
                } else if (mayBeOperand.equals(FilterOperation.OR_OPERATOR)) {
                    specificationStack.push(Specification.where(operand1).or(operand2));
                }
            }

        }
        return !specificationStack.isEmpty() ? specificationStack.pop() : null;
    }

}
