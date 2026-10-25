package nu.fgv.register.server.util.search;

/**
 * A facet value to narrow a search to.
 *
 * @param name  the facet's id
 * @param value the value's id
 */
public record AggregationFilter(String name, String value) {
}