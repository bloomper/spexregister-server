package nu.fgv.register.server.spexare;

import nu.fgv.register.server.util.search.SearchEnabledJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SpexareRepository extends SearchEnabledJpaRepository<Spexare, Long>, JpaSpecificationExecutor<Spexare> {
}
