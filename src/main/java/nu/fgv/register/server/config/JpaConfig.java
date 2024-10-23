package nu.fgv.register.server.config;

import nu.fgv.register.server.acl.SimpleAclJpaRepository;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository;
import nu.fgv.register.server.util.security.JwtAuditorAwareImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(
        basePackages = {
                "nu.fgv.register.server"
        },
        repositoryBaseClass = SimpleAclJpaRepository.class,
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = {
                                SpexareRepository.class
                        }
                )
        }
)
public class JpaConfig {

    @Bean
    @Profile("!test & !integrationtest")
    public AuditorAware<String> auditorAware() {
        return new JwtAuditorAwareImpl();
    }

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
