package nu.fgv.register.server.sync;

import nu.fgv.register.server.util.security.RequiresAdmin;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * The sync of base data from Spexipedia, for admins.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Controller
public class SyncGraphqlApi {

    private final SyncService service;

    public SyncGraphqlApi(final SyncService service) {
        this.service = service;
    }

    @QueryMapping
    @RequiresAdmin
    public List<SyncService.Status> syncSources() {
        return List.of(service.status());
    }

    @MutationMapping
    @RequiresAdmin
    public SyncService.@Nullable Result syncRun(@Argument final @Nullable Boolean full) {
        return service.pull(Boolean.TRUE.equals(full));
    }
}
