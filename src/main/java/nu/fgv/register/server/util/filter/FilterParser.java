package nu.fgv.register.server.util.filter;

import com.google.common.base.Joiner;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.util.StringUtils.hasText;

public class FilterParser {
    private static final Map<String, Operator> OPERATIONS;

    private static final Pattern SPECIFICATION_CRITERIA_PATTERN = Pattern.compile("^(\\w+?)(" + Joiner.on("|").join(FilterOperation.SIMPLE_OPERATION_SET) + ")(\\p{Punct}?)(\\w+?)(\\p{Punct}?)$");

    static {
        OPERATIONS = Map.of("AND", Operator.AND, "OR", Operator.OR, "or", Operator.OR, "and", Operator.AND);
    }

    private FilterParser() {
    }

    private static boolean isHigherPrecedenceOperator(final String currentOperation, final String previousOperation) {
        return (OPERATIONS.containsKey(previousOperation) && OPERATIONS.get(previousOperation).precedence >= OPERATIONS.get(currentOperation).precedence);
    }

    public static Deque<?> parse(final String filter) {
        final Deque<Object> output = new LinkedList<>();
        final Deque<String> stack = new LinkedList<>();

        if (hasText(filter)) {
            Arrays.stream(filter.split("\\s+")).forEach(token -> {
                if (OPERATIONS.containsKey(token)) {
                    while (!stack.isEmpty() && isHigherPrecedenceOperator(token, stack.peek())) {
                        output.push(stack.pop().equalsIgnoreCase(FilterOperation.OR_OPERATOR) ? FilterOperation.OR_OPERATOR : FilterOperation.AND_OPERATOR);
                    }
                    stack.push(token.equalsIgnoreCase(FilterOperation.OR_OPERATOR) ? FilterOperation.OR_OPERATOR : FilterOperation.AND_OPERATOR);
                } else if (token.equals(FilterOperation.LEFT_PARENTHESIS)) {
                    stack.push(FilterOperation.LEFT_PARENTHESIS);
                } else if (token.equals(FilterOperation.RIGHT_PARENTHESIS)) {
                    while (stack.peek() != null && !stack.peek().equals(FilterOperation.LEFT_PARENTHESIS)) {
                        output.push(stack.pop());
                    }
                    stack.pop();
                } else {
                    final Matcher matcher = SPECIFICATION_CRITERIA_PATTERN.matcher(token);

                    while (matcher.find()) {
                        output.push(new FilterCriteria(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4), matcher.group(5)));
                    }
                }
            });

            while (!stack.isEmpty()) {
                output.push(stack.pop());
            }

            return output;
        } else {
            return new LinkedList<>();
        }
    }

    private enum Operator {
        OR(1),
        AND(2);

        final int precedence;

        Operator(final int p) {
            precedence = p;
        }
    }
}
