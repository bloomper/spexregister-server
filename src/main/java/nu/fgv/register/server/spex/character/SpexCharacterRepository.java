package nu.fgv.register.server.spex.character;

import nu.fgv.register.server.spex.Spex;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link SpexCharacter}, Spexipedia's characters as copied by the sync.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
public interface SpexCharacterRepository extends JpaRepository<SpexCharacter, Long> {

    Optional<SpexCharacter> findByUuid(UUID uuid);

    List<SpexCharacter> findBySpexInOrderByPositionAsc(Collection<Spex> spex);

    List<SpexCharacter> findByUuidIn(Collection<UUID> uuids);
}
