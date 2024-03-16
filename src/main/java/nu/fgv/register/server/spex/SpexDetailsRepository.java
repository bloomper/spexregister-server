package nu.fgv.register.server.spex;

import nu.fgv.register.server.acl.AclJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpexDetailsRepository extends AclJpaRepository<SpexDetails, Long> {
}
