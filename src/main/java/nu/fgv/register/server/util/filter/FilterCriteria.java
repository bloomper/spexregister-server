package nu.fgv.register.server.util.filter;

import lombok.Getter;

@Getter
public class FilterCriteria {

    private String key;
    private FilterOperation operation;
    private Object value;
    private boolean orPredicate;

    public FilterCriteria() {
    }

    public FilterCriteria(final String key, final FilterOperation operation, final Object value) {
        super();
        this.key = key;
        this.operation = operation;
        this.value = value;
    }

    public FilterCriteria(final String orPredicate, final String key, final FilterOperation operation, final Object value) {
        super();
        this.orPredicate = orPredicate != null && orPredicate.equals(FilterOperation.OR_PREDICATE_FLAG);
        this.key = key;
        this.operation = operation;
        this.value = value;
    }

    public FilterCriteria(final String key, final String operation, final String prefix, final String value, final String suffix) {
        FilterOperation op = FilterOperation.getSimpleOperation(operation.charAt(0));

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
        this.key = key;
        this.operation = op;
        this.value = value;
    }

}
