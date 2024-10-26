/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nu.fgv.register.server.config;

import jakarta.annotation.PostConstruct;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.migration.JavaMigration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
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
