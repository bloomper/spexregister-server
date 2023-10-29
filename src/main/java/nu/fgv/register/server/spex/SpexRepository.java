package nu.fgv.register.server.spex;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SpexRepository extends JpaRepository<Spex, Long>, JpaSpecificationExecutor<Spex> {
}
