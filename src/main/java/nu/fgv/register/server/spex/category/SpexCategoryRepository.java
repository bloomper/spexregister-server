package nu.fgv.register.server.spex.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SpexCategoryRepository extends JpaRepository<SpexCategory, Long>, JpaSpecificationExecutor<SpexCategory> {
}
