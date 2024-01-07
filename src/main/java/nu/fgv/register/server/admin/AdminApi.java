package nu.fgv.register.server.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.util.StringUtils.capitalize;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class AdminApi {

    private final IndexingService indexingService;

    @PostMapping(value = "/index/{entity}")
    @PreAuthorize("hasRole('SPEXREGISTER_ADMIN')")
    public ResponseEntity<Void> index(final @PathVariable String entity) {
        try {
            final Class<?> clazz = Class.forName(String.format("nu.fgv.register.%s.%s", entity.toLowerCase(), capitalize(entity))); // NOSONAR
            indexingService.initiateIndexingFor(clazz, true);
            return ResponseEntity.ok().build();
        } catch (final ClassNotFoundException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
