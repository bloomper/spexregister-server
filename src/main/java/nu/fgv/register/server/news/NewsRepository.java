package nu.fgv.register.server.news;

import nu.fgv.register.server.acl.AclJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsRepository extends AclJpaRepository<News, Long>, JpaSpecificationExecutor<News> {
}
