package nu.fgv.register.server.task;

import nu.fgv.register.server.acl.AclJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskRepository extends AclJpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    @PostAuthorize("!returnObject.isEmpty() ? hasPermission(returnObject.get(), 'READ') : true")
    default Optional<Task> findById0(final Long id) {
        return this
                .findById(id);
    }
}
