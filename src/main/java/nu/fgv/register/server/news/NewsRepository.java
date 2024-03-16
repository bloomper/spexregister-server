package nu.fgv.register.server.news;

import nu.fgv.register.server.acl.AclJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NewsRepository extends AclJpaRepository<News, Long>, JpaSpecificationExecutor<News> {

    @PostAuthorize("!returnObject.isEmpty() ? hasPermission(returnObject.get(), 'READ') : true")
    default Optional<News> findById0(final Long id) {
        return this
                .findById(id);
    }
}
