package nu.fgv.register.server.config;

import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.SpexareSearchEnabledJpaRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "utcDateTimeProvider")
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
public class JpaConfig {

    @Bean
    public DateTimeProvider utcDateTimeProvider() {
        return () -> Optional.of(LocalDateTime.now(ZoneOffset.UTC));
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
