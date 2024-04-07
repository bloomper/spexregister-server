package nu.fgv.register.server.user;

import nu.fgv.register.server.acl.AclJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends AclJpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @PostAuthorize("!returnObject.isEmpty() ? hasPermission(returnObject.get(), 'READ') : true")
    default Optional<User> findById0(final Long id) {
        return this
                .findById(id);
    }

    boolean existsByExternalId(String externalId);
}
