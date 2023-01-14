package nu.fgv.register.server.spex;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface SpexDetailsRepository extends JpaRepository<SpexDetails, Long> {
}
