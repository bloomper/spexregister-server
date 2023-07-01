package nu.fgv.register.server.config;

import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository;
import nu.fgv.register.server.util.search.AbstractSearchEnabledJpaRepository;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(
        basePackages = {
                "nu.fgv.register.server"
        },
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = {
                                SpexareRepository.class
                        }
                )
        }
)
public class JpaPersistenceConfig {

    @Configuration
    @EnableJpaRepositories(
            basePackages = "nu.fgv.register.server.spexare",
            repositoryBaseClass = SpexareSearchEnabledJpaRepository.class,
            includeFilters = {
                    @ComponentScan.Filter(
                            type = FilterType.ASSIGNABLE_TYPE,
                            classes = {
                                    SpexareRepository.class
                            }
                    )
            }
    )
    public static class SpexareJpaPersistenceConfig {
    }

}
