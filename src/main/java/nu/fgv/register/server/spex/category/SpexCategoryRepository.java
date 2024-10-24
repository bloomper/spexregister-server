package nu.fgv.register.server.spex.category;

import nu.fgv.register.server.acl.AclJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpexCategoryRepository extends AclJpaRepository<SpexCategory, Long>, JpaSpecificationExecutor<SpexCategory> {

    @PostAuthorize("!returnObject.isEmpty() ? hasPermission(returnObject.get(), 'READ') : true")
    default Optional<SpexCategory> findById0(final Long id) {
        return this
                .findById(id);
    }
}
