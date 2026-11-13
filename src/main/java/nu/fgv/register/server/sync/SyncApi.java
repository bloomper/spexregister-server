package nu.fgv.register.server.sync;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spexipedia tells Spexregister that its change feed has something new; the call only starts a pull and carries no
 * data. Requires {@code NOTIFY} on {@code spexregister}.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@RestController
public class SyncApi {

    private final SyncService service;

    public SyncApi(final SyncService service) {
        this.service = service;
    }

    @PostMapping("/api/sync/notify")
    @PreAuthorize("hasRole('spexregister_NOTIFY')")
    public ResponseEntity<Void> notifyChanged() {
        service.pullSoon();
        return ResponseEntity.accepted().build();
    }
}
