package nu.fgv.register.server.spex;

import nu.fgv.register.server.acl.AclJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpexRepository extends AclJpaRepository<Spex, Long>, JpaSpecificationExecutor<Spex> {

    @PostAuthorize("!returnObject.isEmpty() ? hasPermission(returnObject.get(), 'READ') : true")
    default Optional<Spex> findById0(final Long id) {
        return this
                .findById(id);
    }
}
