package nu.fgv.register.server.tag;

import nu.fgv.register.server.acl.AclJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TagRepository extends AclJpaRepository<Tag, Long>, JpaSpecificationExecutor<Tag> {

    @PostAuthorize("!returnObject.isEmpty() ? hasPermission(returnObject.get(), 'READ') : true")
    default Optional<Tag> findById0(final Long id) {
        return this
                .findById(id);
    }
}
