package nu.fgv.register.server.acl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.security.acls.model.Permission;

import java.util.List;

@NoRepositoryBean
public interface AclJpaRepository<T, ID> extends JpaRepository<T, ID> {

    List<T> findAll(Permission permission);

    List<T> findAll(Sort sort, Permission permission);

    List<T> findAll(Specification<T> spec, Sort sort, Permission permission);

    Page<T> findAll(Pageable pageable, Permission permission);

    List<T> findAll(Specification<T> spec, Permission permission);

    Page<T> findAll(Specification<T> spec, Pageable pageable, Permission permission);
}
