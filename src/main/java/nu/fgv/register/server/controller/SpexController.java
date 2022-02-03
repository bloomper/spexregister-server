package nu.fgv.register.server.controller;

import nu.fgv.register.server.model.Spex;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class SpexController {

    @QueryMapping
    public Spex spexById(@Argument Long id) {
        // ...
    }

    @MutationMapping
    public Spex addSpex(@Argument SpexInput spexInput) {
        // ...
    }
}
