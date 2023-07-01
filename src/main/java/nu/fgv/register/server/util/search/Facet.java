package nu.fgv.register.server.util.search;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Builder
@Getter
public class Facet {

    private String name;

    private Map<String, Long> values;
}
