package nu.fgv.register.server.task.category;

import nu.fgv.register.server.acl.AclJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskCategoryRepository extends AclJpaRepository<TaskCategory, Long>, JpaSpecificationExecutor<TaskCategory> {

    @PostAuthorize("!returnObject.isEmpty() ? hasPermission(returnObject.get(), 'READ') : true")
    default Optional<TaskCategory> findById0(final Long id) {
        return this
                .findById(id);
    }
}
