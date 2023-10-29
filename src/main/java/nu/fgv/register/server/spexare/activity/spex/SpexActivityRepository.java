package nu.fgv.register.server.spexare.activity.spex;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SpexActivityRepository extends JpaRepository<SpexActivity, Long>, JpaSpecificationExecutor<SpexActivity> {
}
