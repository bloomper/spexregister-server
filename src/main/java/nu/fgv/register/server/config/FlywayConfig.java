package nu.fgv.register.server.config;

import jakarta.annotation.PostConstruct;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.migration.JavaMigration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {
    private final DataSource dataSource;
    private final ApplicationContext applicationContext;

    @Value("${spexregister.sample-data.import:false}")
    private boolean importSampleData;

    @Value("${spexregister.crypto.algorithm}")
    private String algorithm;

    @Value("${spexregister.crypto.secret-key}")
    private String secretKey;

    @Value("${spexregister.crypto.initialization-vector}")
    private String iv;

    public FlywayConfig(final DataSource dataSource, final ApplicationContext applicationContext) {
        this.dataSource = dataSource;
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        final JavaMigration[] migrationBeans = applicationContext
                .getBeansOfType(JavaMigration.class)
                .values()
                .toArray(new JavaMigration[0]);
        final Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .installedBy("system")
                .javaMigrations(migrationBeans)
                .load();

        flyway.migrate();
    }
}
