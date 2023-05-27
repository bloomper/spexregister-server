package nu.fgv.register.server.util;

import org.opensearch.testcontainers.OpensearchContainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext
@Import({AbstractIntegrationTest.TestConfig.class})
@TestPropertySource(
        properties = {
                "spexregister.crypto.secret-key=7x!A%C*F-JaNdRgUkXp2s5v8y/B?E(G+",
                "spexregister.crypto.initialization-vector=8914343887327891"
        }
)
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.33");

    @Container
    //@ServiceConnection TODO Enable this when supported
    private static final OpensearchContainer opensearch;

    static {
        opensearch = new OpensearchContainer(DockerImageName.parse("opensearchproject/opensearch:2.0.0"));
        opensearch.start();
    }

    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.properties.hibernate.search.backend.hosts", () -> String.format("%s:%s", opensearch.getHost(), opensearch.getMappedPort(9200)));
        registry.add("spring.jpa.properties.hibernate.search.backend.username", opensearch::getUsername);
        registry.add("spring.jpa.properties.hibernate.search.backend.password", opensearch::getPassword);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public AuditorAware<String> auditorAware() {
            return () -> Optional.of("dummy");
        }
    }

}
