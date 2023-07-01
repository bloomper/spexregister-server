package nu.fgv.register.server.config;

import nu.fgv.register.server.util.search.SimpleSearchEnabledJpaRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(
        value = "nu.fgv.register.server",
        repositoryBaseClass = SimpleSearchEnabledJpaRepository.class
)
public class JpaPersistenceConfig {
}
